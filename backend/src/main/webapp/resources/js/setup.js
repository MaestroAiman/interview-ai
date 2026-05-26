let currentStepNum = 1
let selectedType = 'TECHNICAL'
let selectedDifficulty = 'MID'

function nextStep(from) {
    if (from === 2) {
        const posInput = document.getElementById('setupForm:position')
        if (!posInput || !posInput.value.trim()) {
            alert('Please enter a job position')
            return
        }
    }
    goToStep(from + 1)
    if (from + 1 === 5) populateConfirm()
}

function goToStep(step) {
    document.querySelectorAll('.step-panel').forEach(p => p.classList.add('d-none'))
    const target = document.getElementById('step' + step)
    if (target) target.classList.remove('d-none')
    document.querySelectorAll('.step-indicator .step').forEach((s, i) => {
        s.classList.toggle('active', i < step)
    })
    currentStepNum = step
}

function selectType(type, el) {
    selectedType = type
    document.querySelectorAll('.type-card').forEach(c => c.classList.remove('selected'))
    el.classList.add('selected')
    const hidden = document.getElementById('setupForm:selectedType')
    if (hidden) hidden.value = type
}

function selectDifficulty(diff, el) {
    selectedDifficulty = diff
    document.querySelectorAll('.difficulty-option').forEach(d => d.classList.remove('selected'))
    el.classList.add('selected')
    const hidden = document.getElementById('setupForm:difficulty')
    if (hidden) hidden.value = diff
}

function updateCount(val) {
    const countEl = document.getElementById('countValue')
    const durEl   = document.getElementById('durationEst')
    if (countEl) countEl.textContent = val
    if (durEl)   durEl.textContent   = '~' + (val * 2) + ' minutes'
    const hidden = document.getElementById('setupForm:questionCount')
    if (hidden) hidden.value = val
}

function setPosition(pos) {
    const input = document.getElementById('setupForm:position')
    if (input) input.value = pos
}

function populateConfirm() {
    const posEl = document.getElementById('setupForm:position')
    const countEl = document.getElementById('countValue')
    const confirmType     = document.getElementById('confirmType')
    const confirmPosition = document.getElementById('confirmPosition')
    const confirmDiff     = document.getElementById('confirmDifficulty')
    const confirmCount    = document.getElementById('confirmCount')

    if (confirmType)     confirmType.textContent     = selectedType
    if (confirmPosition) confirmPosition.textContent = posEl ? posEl.value : ''
    if (confirmDiff)     confirmDiff.textContent     = selectedDifficulty
    if (confirmCount)    confirmCount.textContent    = (countEl ? countEl.textContent : '10') + ' questions'
}

// =============================================================================
// CV Analysis — appended (no changes above this line)
// =============================================================================

let cvSelectedFile = null

document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.step-panel').forEach(p => p.classList.add('d-none'))
    const s0 = document.getElementById('step0')
    if (s0) s0.classList.remove('d-none')
    const indicator = document.getElementById('main-step-indicator')
    if (indicator) indicator.style.display = 'none'
})

function chooseCvMode() {
    document.getElementById('gateway-choice').classList.add('d-none')
    document.getElementById('cv-upload-area').classList.remove('d-none')
}

function chooseManualMode() {
    cvClearAiSuggestedBadges()
    const indicator = document.getElementById('main-step-indicator')
    if (indicator) indicator.style.display = ''
    goToStep(1)
}

function cvBackToGateway() {
    document.getElementById('cv-upload-area').classList.add('d-none')
    document.getElementById('gateway-choice').classList.remove('d-none')
    cvClearFile()
    cvHideError()
}

function cvDragOver(event) {
    event.preventDefault()
    document.getElementById('cv-drop-zone').style.borderColor = '#3B82F6'
}

function cvDragLeave() {
    document.getElementById('cv-drop-zone').style.borderColor = 'rgba(255,255,255,0.15)'
}

function cvDrop(event) {
    event.preventDefault()
    document.getElementById('cv-drop-zone').style.borderColor = 'rgba(255,255,255,0.15)'
    const files = event.dataTransfer.files
    if (files && files.length > 0) cvFileSelected(files[0])
}

function cvFileSelected(file) {
    if (!file) return
    if (!file.name.toLowerCase().endsWith('.pdf')) {
        cvShowError('Only PDF files are accepted.')
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        cvShowError('File is too large. Maximum allowed size is 5 MB.')
        return
    }
    cvHideError()
    cvSelectedFile = file
    document.getElementById('cv-file-preview').classList.remove('d-none')
    document.getElementById('cv-filename').textContent = file.name
    document.getElementById('cv-filesize').textContent = (file.size / 1024).toFixed(1) + ' KB'
    document.getElementById('cv-analyze-btn').removeAttribute('disabled')
}

function cvClearFile() {
    cvSelectedFile = null
    const input = document.getElementById('cv-file-input')
    if (input) input.value = ''
    document.getElementById('cv-file-preview').classList.add('d-none')
    document.getElementById('cv-analyze-btn').setAttribute('disabled', 'disabled')
    cvHideError()
}

async function uploadAndAnalyzeCv() {
    if (!cvSelectedFile) return

    document.getElementById('cv-analyzing').classList.remove('d-none')
    document.getElementById('cv-analyze-btn').setAttribute('disabled', 'disabled')
    document.getElementById('cv-file-preview').classList.add('d-none')
    cvHideError()

    const formData = new FormData()
    formData.append('file', cvSelectedFile)

    try {
        const response = await fetch('/interview-ai/api/cv/analyze', {
            method: 'POST',
            credentials: 'include',
            body: formData
        })

        if (!response.ok) {
            let msg = 'CV analysis failed. Switching to manual setup.'
            try {
                const body = await response.json()
                if (body && body.error) msg = body.error + ' Switching to manual setup.'
            } catch (_) {}
            cvShowError(msg)
            cvFallbackToManual()
            return
        }

        const recommendation = await response.json()
        autoFillFromCv(recommendation)

    } catch (_) {
        cvShowError('Network error during CV analysis. Switching to manual setup.')
        cvFallbackToManual()
    } finally {
        document.getElementById('cv-analyzing').classList.add('d-none')
    }
}

function autoFillFromCv(recommendation) {
    const type       = (recommendation.type       || 'TECHNICAL').toUpperCase()
    const position   = (recommendation.position   || '').trim()
    const difficulty = (recommendation.difficulty || 'MID').toUpperCase()
    const reasoning  = recommendation.reasoning   || ''

    selectedType       = type
    selectedDifficulty = difficulty

    const hiddenType = document.getElementById('setupForm:selectedType')
    if (hiddenType) hiddenType.value = type

    const hiddenDiff = document.getElementById('setupForm:difficulty')
    if (hiddenDiff) hiddenDiff.value = difficulty

    const posInput = document.getElementById('setupForm:position')
    if (posInput) posInput.value = position

    document.querySelectorAll('.type-card').forEach(c => {
        const oc = c.getAttribute('onclick') || ''
        c.classList.toggle('selected', oc.includes("'" + type + "'"))
    })
    document.querySelectorAll('.difficulty-option').forEach(d => {
        const oc = d.getAttribute('onclick') || ''
        d.classList.toggle('selected', oc.includes("'" + difficulty + "'"))
    })

    cvShowAiSuggestedBadges()

    const reasoningCard = document.getElementById('cv-reasoning-card')
    const reasoningText = document.getElementById('cv-reasoning-text')
    if (reasoningCard && reasoning) {
        reasoningCard.classList.remove('d-none')
        if (reasoningText) reasoningText.textContent = reasoning
    }

    const indicator = document.getElementById('main-step-indicator')
    if (indicator) indicator.style.display = ''

    populateConfirm()
    goToStep(5)
}

function cvFallbackToManual() {
    setTimeout(() => {
        cvClearAiSuggestedBadges()
        const indicator = document.getElementById('main-step-indicator')
        if (indicator) indicator.style.display = ''
        goToStep(1)
    }, 2500)
}

function cvShowAiSuggestedBadges() {
    ['badge-type', 'badge-position', 'badge-difficulty'].forEach(id => {
        const el = document.getElementById(id)
        if (el) el.classList.remove('d-none')
    })
}

function cvClearAiSuggestedBadges() {
    ['badge-type', 'badge-position', 'badge-difficulty'].forEach(id => {
        const el = document.getElementById(id)
        if (el) el.classList.add('d-none')
    })
    const rc = document.getElementById('cv-reasoning-card')
    if (rc) rc.classList.add('d-none')
}

function cvShowError(msg) {
    const el = document.getElementById('cv-error')
    if (!el) return
    el.textContent = msg
    el.classList.remove('d-none')
    document.getElementById('cv-file-preview').classList.remove('d-none')
    document.getElementById('cv-analyze-btn').removeAttribute('disabled')
}

function cvHideError() {
    const el = document.getElementById('cv-error')
    if (el) el.classList.add('d-none')
}
