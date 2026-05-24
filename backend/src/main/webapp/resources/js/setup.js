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
