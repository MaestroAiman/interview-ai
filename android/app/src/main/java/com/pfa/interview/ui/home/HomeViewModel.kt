package com.pfa.interview.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.data.local.TokenDataStore
import com.pfa.interview.data.model.InterviewSession
import com.pfa.interview.data.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

private val dailyTips = listOf(
    "Use the STAR method: Situation, Task, Action, Result.",
    "Research the company before your interview.",
    "Prepare 2-3 questions to ask the interviewer.",
    "Practice your answers out loud, not just in your head.",
    "Dress one level above the company's dress code.",
    "Arrive 10 minutes early to calm your nerves.",
    "Follow up with a thank-you email within 24 hours."
)

data class HomeUiState(
    val userName: String = "",
    val totalSessions: Int = 0,
    val averageScore: Float = 0f,
    val streakDays: Int = 0,
    val recentSessions: List<InterviewSession> = emptyList(),
    val dailyTip: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val userName = tokenDataStore.getUserName() ?: "there"
            val tip = dailyTips[(System.currentTimeMillis() / 86_400_000).toInt() % dailyTips.size]
            _uiState.update { it.copy(userName = userName, dailyTip = tip, isLoading = true) }

            historyRepository.getSessions().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val sessions = result.data
                        val avg = if (sessions.isEmpty()) 0f
                        else sessions.filter { it.overallScore > 0 }.map { it.overallScore }
                            .average().toFloat()
                        val streak = computeStreak(sessions)
                        _uiState.update {
                            it.copy(
                                totalSessions = sessions.size,
                                averageScore = avg,
                                streakDays = streak,
                                recentSessions = sessions.take(3),
                                isLoading = false
                            )
                        }
                    }
                    is Resource.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                    is Resource.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun refresh() = loadData()

    private fun computeStreak(sessions: List<InterviewSession>): Int {
        if (sessions.isEmpty()) return 0
        val isoFmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val basicFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val days = sessions.mapNotNull { session ->
            val raw = session.startedAt
            runCatching { LocalDate.parse(raw, isoFmt) }.getOrNull()
                ?: runCatching { LocalDate.parse(raw, basicFmt) }.getOrNull()
                ?: runCatching { LocalDate.parse(raw.take(10)) }.getOrNull()
        }.toSortedSet(compareByDescending { it })

        if (days.isEmpty()) return 0

        var streak = 0
        var expected = LocalDate.now(ZoneId.systemDefault())
        for (day in days) {
            when {
                day == expected || day == expected.minusDays(1) -> {
                    streak++
                    expected = day.minusDays(1)
                }
                day.isBefore(expected) -> break
            }
        }
        return streak
    }
}
