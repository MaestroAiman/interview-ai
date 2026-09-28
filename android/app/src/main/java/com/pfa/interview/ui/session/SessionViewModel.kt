package com.pfa.interview.ui.session

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.CurrentSessionHolder
import com.pfa.interview.data.model.Feedback
import com.pfa.interview.data.model.Question
import com.pfa.interview.data.model.SessionState
import com.pfa.interview.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isAI: Boolean,
    val timestamp: String,
    val isTyping: Boolean = false
)

data class SessionUiState(
    val sessionId: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val currentQuestion: Question? = null,
    val currentQuestionIndex: Int = 0,
    val totalQuestions: Int = 0,
    val currentAnswer: String = "",
    val pendingAnswer: String = "",
    val isRecording: Boolean = false,
    val isAITyping: Boolean = false,
    val isTimeout: Boolean = false,
    val timerSeconds: Int = 60,
    val showFeedback: Boolean = false,
    val currentFeedback: Feedback? = null,
    val isSessionComplete: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val currentSessionHolder: CurrentSessionHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    init {
        val pending = currentSessionHolder.consume()
        if (pending != null) {
            initSession(pending.sessionId, pending.firstQuestion, pending.totalQuestions)
        }
    }

    fun initSession(sessionId: String, firstQuestion: Question, totalQuestions: Int) {
        _uiState.update {
            it.copy(
                sessionId = sessionId,
                currentQuestion = firstQuestion,
                totalQuestions = totalQuestions,
                messages = listOf(
                    ChatMessage(
                        text = firstQuestion.content,
                        isAI = true,
                        timestamp = timeFormat.format(Date())
                    )
                )
            )
        }
        startTimer()
    }

    fun loadForResume(sessionId: String) {
        if (_uiState.value.sessionId.isNotBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(sessionId = sessionId) }
            when (val result = sessionRepository.getSessionState(sessionId)) {
                is Resource.Success -> {
                    val state = result.data
                    if (state.isCompleted()) {
                        _uiState.update { it.copy(isSessionComplete = true) }
                        return@launch
                    }
                    val answeredIds = state.answers.map { it.questionId }.toSet()
                    val nextQuestion = state.questions
                        .sortedBy { it.order }
                        .firstOrNull { it.id !in answeredIds }
                        ?: return@launch
                    _uiState.update { it.copy(
                        currentQuestion = nextQuestion,
                        currentQuestionIndex = state.answeredCount,
                        totalQuestions = state.totalQuestions,
                        messages = listOf(ChatMessage(
                            text = nextQuestion.content,
                            isAI = true,
                            timestamp = timeFormat.format(Date())
                        ))
                    )}
                    startTimer()
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun onAnswerChange(text: String) = _uiState.update { it.copy(currentAnswer = text) }

    fun startRecording(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _uiState.update { it.copy(error = "Speech recognition not available") }
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _uiState.update { it.copy(currentAnswer = partial?.firstOrNull() ?: it.currentAnswer) }
            }
            override fun onResults(results: Bundle?) {
                val final = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _uiState.update { it.copy(currentAnswer = final?.firstOrNull() ?: it.currentAnswer, isRecording = false) }
            }
            override fun onError(error: Int) { _uiState.update { it.copy(isRecording = false) } }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
        _uiState.update { it.copy(isRecording = true) }
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isRecording = false) }
    }

    fun submitAnswer() {
        val state = _uiState.value
        if (state.currentAnswer.isBlank() || state.currentQuestion == null) return
        submitAnswerInternal(state.currentAnswer)
    }

    fun retryAnswer() {
        val state = _uiState.value
        if (state.pendingAnswer.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isTimeout = false, isAITyping = true) }
            when (val stateResult = sessionRepository.getSessionState(state.sessionId)) {
                is Resource.Success -> handleServerState(stateResult.data, state.pendingAnswer)
                else -> submitAnswerInternal(state.pendingAnswer)
            }
        }
    }

    private fun handleServerState(serverState: SessionState, pendingAnswer: String) {
        when {
            serverState.isCompleted() -> {
                currentSessionHolder.setSummary(serverState.toSummary())
                _uiState.update { it.copy(isAITyping = false, isSessionComplete = true) }
            }
            serverState.hasFeedbackFor(_uiState.value.currentQuestion?.id) -> {
                val feedback = serverState.feedbackFor(_uiState.value.currentQuestion?.id)
                _uiState.update { it.copy(
                    isAITyping = false,
                    currentFeedback = feedback,
                    showFeedback = true,
                    pendingAnswer = ""
                )}
            }
            else -> submitAnswerInternal(pendingAnswer)
        }
    }

    private fun submitAnswerInternal(answerText: String) {
        val state = _uiState.value
        if (state.currentQuestion == null) return
        timerJob?.cancel()

        val userMessage = ChatMessage(
            text = answerText,
            isAI = false,
            timestamp = timeFormat.format(Date())
        )
        val typingMessage = ChatMessage("", isAI = true, timestamp = "", isTyping = true)

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + typingMessage,
                isAITyping = true,
                isTimeout = false,
                error = null,
                currentAnswer = "",
                pendingAnswer = answerText
            )
        }

        viewModelScope.launch {
            when (val result = sessionRepository.submitAdaptiveAnswer(
                state.sessionId,
                state.currentQuestion.id,
                answerText
            )) {
                is Resource.Success -> {
                    val response = result.data
                    _uiState.update { s ->
                        s.copy(
                            messages = s.messages.dropLast(1),
                            isAITyping = false,
                            pendingAnswer = "",
                            currentFeedback = response.feedback,
                            showFeedback = true
                        )
                    }
                    if (response.sessionSummary != null) {
                        currentSessionHolder.setSummary(response.sessionSummary)
                        _uiState.update { it.copy(isSessionComplete = true) }
                    } else if (response.nextQuestion == null) {
                        _uiState.update { it.copy(isSessionComplete = true) }
                    } else {
                        _uiState.update { s ->
                            s.copy(
                                currentQuestion = response.nextQuestion,
                                currentQuestionIndex = s.currentQuestionIndex + 1
                            )
                        }
                    }
                }
                is Resource.Error -> {
                    val msg = result.message ?: ""
                    when {
                        msg.contains("not in progress", ignoreCase = true) -> {
                            _uiState.update { s -> s.copy(messages = s.messages.dropLast(1), isAITyping = false) }
                            val stateResult = sessionRepository.getSessionState(state.sessionId)
                            if (stateResult is Resource.Success && stateResult.data.isCompleted()) {
                                currentSessionHolder.setSummary(stateResult.data.toSummary())
                                _uiState.update { it.copy(pendingAnswer = "", isSessionComplete = true) }
                            } else {
                                _uiState.update { it.copy(error = msg) }
                            }
                        }
                        msg.contains("timeout", ignoreCase = true) -> {
                            _uiState.update { s ->
                                s.copy(messages = s.messages.dropLast(1), isAITyping = false, isTimeout = true)
                            }
                        }
                        else -> {
                            _uiState.update { s ->
                                s.copy(messages = s.messages.dropLast(1), isAITyping = false, error = msg)
                            }
                        }
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun dismissFeedback() {
        val state = _uiState.value
        val nextQuestion = state.currentQuestion
        if (nextQuestion != null && !state.isSessionComplete) {
            val aiMessage = ChatMessage(
                text = nextQuestion.content,
                isAI = true,
                timestamp = timeFormat.format(Date())
            )
            _uiState.update { it.copy(showFeedback = false, messages = it.messages + aiMessage) }
            startTimer()
        } else {
            _uiState.update { it.copy(showFeedback = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun startTimer() {
        timerJob?.cancel()
        _uiState.update { it.copy(timerSeconds = 60) }
        timerJob = viewModelScope.launch {
            repeat(60) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        speechRecognizer?.destroy()
    }
}
