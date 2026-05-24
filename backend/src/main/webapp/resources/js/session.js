const data = document.getElementById('session-data').dataset
let currentQuestionId = data.questionId
let questionIndex = parseInt(data.questionIndex) || 1
const totalQuestions = parseInt(data.questionCount)
let timerInterval = null
let timerSeconds = 60
let isRecording = false

// ── Timer ─────────────────────────────────────────────────────────────────
function startTimer() {
    clearInterval(timerInterval)
    timerSeconds = 60
    updateTimerDisplay()
    timerInterval = setInterval(() => {
        timerSeconds--
        updateTimerDisplay()
        if (timerSeconds <= 0) {
            clearInterval(timerInterval)
            submitAnswer()
        }
    }, 1000)
}

function updateTimerDisplay() {
    document.getElementById('timerText').textContent = timerSeconds
    const progress = document.getElementById('timerProgress')
    const circumference = 2 * Math.PI * 45
    const offset = circumference * (1 - timerSeconds / 60)
    progress.style.strokeDasharray = circumference
    progress.style.strokeDashoffset = offset
    progress.style.stroke = timerSeconds > 30 ? '#10B981' :
                             timerSeconds > 10 ? '#F59E0B' : '#F43F5E'
}

// ── Chat bubbles ──────────────────────────────────────────────────────────
function appendAIBubble(text) {
    const chatArea = document.getElementById('chatArea')
    const bubble = document.createElement('div')
    bubble.className = 'bubble-ai animate-in'
    bubble.innerHTML = `
        <div class="bubble-avatar">AI</div>
        <div class="bubble-content">${text}</div>`
    chatArea.appendChild(bubble)
    chatArea.scrollTop = chatArea.scrollHeight
}

function appendUserBubble(text) {
    const chatArea = document.getElementById('chatArea')
    const bubble = document.createElement('div')
    bubble.className = 'bubble-user animate-in'
    bubble.innerHTML = `<div class="bubble-content">${text}</div>`
    chatArea.appendChild(bubble)
    chatArea.scrollTop = chatArea.scrollHeight
}

function showTypingIndicator() {
    const chatArea = document.getElementById('chatArea')
    const typing = document.createElement('div')
    typing.id = 'typingIndicator'
    typing.className = 'bubble-ai'
    typing.innerHTML = `
        <div class="bubble-avatar">AI</div>
        <div class="typing-dots">
            <span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span>
        </div>`
    chatArea.appendChild(typing)
    chatArea.scrollTop = chatArea.scrollHeight
}

function hideTypingIndicator() {
    const el = document.getElementById('typingIndicator')
    if (el) el.remove()
}

// ── Answer submission ──────────────────────────────────────────────────────
function submitAnswer() {
    const answerText = document.getElementById('answerInput').value.trim()
    if (!answerText) return

    clearInterval(timerInterval)
    appendUserBubble(answerText)
    document.getElementById('answerInput').value = ''
    document.getElementById('sendBtn').disabled = true
    showTypingIndicator()

    fetch('/interview-ai/api/session/answer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            sessionId: data.sessionId,
            questionId: currentQuestionId,
            answerText: answerText
        })
    })
    .then(r => r.json())
    .then(result => {
        hideTypingIndicator()
        showFeedbackPanel(result.feedback)
        document.getElementById('nextBtn').onclick = () => {
            hideFeedbackPanel()
            if (result.nextQuestion) {
                currentQuestionId = result.nextQuestion.id
                questionIndex++
                updateProgress()
                setTimeout(() => {
                    appendAIBubble(result.nextQuestion.content)
                    document.getElementById('sendBtn').disabled = false
                    startTimer()
                }, 500)
            } else {
                setTimeout(() => {
                    window.location.href =
                        '/interview-ai/results.xhtml?sessionId=' + data.sessionId
                }, 1000)
            }
        }
    })
    .catch(err => {
        hideTypingIndicator()
        console.error('Error:', err)
        document.getElementById('sendBtn').disabled = false
    })
}

// ── Feedback panel ─────────────────────────────────────────────────────────
function showFeedbackPanel(feedback) {
    const panel = document.getElementById('feedbackPanel')
    panel.classList.remove('d-none')

    const score = feedback.overallScore
    const circumference = 2 * Math.PI * 35
    const fill = document.getElementById('ringFill')
    fill.style.strokeDasharray = circumference
    fill.style.strokeDashoffset = circumference * (1 - score / 10)
    fill.style.stroke = score >= 7 ? '#10B981' : score >= 5 ? '#F59E0B' : '#F43F5E'
    document.getElementById('ringValue').textContent = score.toFixed(1)

    setBar('relevanceBar', 'relevanceVal', feedback.relevanceScore)
    setBar('clarityBar',   'clarityVal',   feedback.clarityScore)
    setBar('sentimentBar', 'sentimentVal', feedback.sentimentScore)

    document.getElementById('feedbackComment').textContent     = feedback.shortComment
    document.getElementById('feedbackImprovements').textContent = feedback.improvements
}

function setBar(barId, valId, score) {
    document.getElementById(barId).style.width = (score * 10) + '%'
    document.getElementById(valId).textContent = score.toFixed(1)
}

function hideFeedbackPanel() {
    document.getElementById('feedbackPanel').classList.add('d-none')
}

function updateProgress() {
    document.getElementById('qIndex').textContent = questionIndex
    document.getElementById('progressFill').style.width =
        ((questionIndex / totalQuestions) * 100) + '%'
}

// ── Resume: restore prior Q&A chat history ────────────────────────────────
async function restoreSessionHistory(sessionId) {
    try {
        const resp = await fetch('/interview-ai/api/sessions/' + sessionId)
        if (!resp.ok) return
        const state = await resp.json()

        const chatArea = document.getElementById('chatArea')
        // The server-rendered current-question bubble is the first child.
        // Insert history BEFORE it so the order becomes:
        //   Q1 → A1 → Q2 → A2 → … → current-question (server-rendered)
        const anchor = chatArea.firstElementChild

        const questions = (state.questions || []).sort((a, b) => a.order - b.order)
        const answers   = state.answers || []

        for (const q of questions) {
            const ans = answers.find(a => a.questionId === q.id)
            if (!ans) break   // reached the unanswered question — stop

            const aiBubble = document.createElement('div')
            aiBubble.className = 'bubble-ai'
            aiBubble.innerHTML =
                `<div class="bubble-avatar">AI</div>` +
                `<div class="bubble-content">${escapeHtml(q.content)}</div>`

            const userBubble = document.createElement('div')
            userBubble.className = 'bubble-user'
            userBubble.innerHTML = `<div class="bubble-content">${escapeHtml(ans.text)}</div>`

            chatArea.insertBefore(aiBubble,  anchor)
            chatArea.insertBefore(userBubble, anchor)
        }
        chatArea.scrollTop = chatArea.scrollHeight
    } catch (e) {
        console.warn('Could not restore session history:', e)
    }
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
}

// ── Voice recording (Web Speech API) ──────────────────────────────────────
const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
let recognition = null

function toggleRecording() {
    if (!SpeechRecognition) {
        alert('Voice recognition is not supported in this browser. Use Chrome.')
        return
    }
    if (isRecording) {
        recognition.stop()
        isRecording = false
        document.getElementById('micIcon').className = 'bi bi-mic-fill'
        document.getElementById('micPulse').style.display = 'none'
    } else {
        recognition = new SpeechRecognition()
        recognition.lang = 'fr-FR'
        recognition.continuous = false
        recognition.interimResults = true
        recognition.onresult = (e) => {
            const transcript = Array.from(e.results)
                .map(r => r[0].transcript).join('')
            document.getElementById('answerInput').value = transcript
        }
        recognition.onend = () => {
            isRecording = false
            document.getElementById('micIcon').className = 'bi bi-mic-fill'
            document.getElementById('micPulse').style.display = 'none'
        }
        recognition.start()
        isRecording = true
        document.getElementById('micIcon').className = 'bi bi-stop-fill'
        document.getElementById('micPulse').style.display = 'block'
    }
}

// ── Init ───────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    updateProgress()   // show correct bar position on resume (no-op for new sessions)
    startTimer()
    document.getElementById('answerInput').addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.ctrlKey) submitAnswer()
    })
    if (questionIndex > 1) {
        await restoreSessionHistory(data.sessionId)
    }
})
