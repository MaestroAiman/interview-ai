package com.pfa.interview.ui.setup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.core.utils.Resource
import com.pfa.interview.ui.theme.*

@Composable
fun SetupScreen(
    onSessionStarted: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val cvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.analyzeCv(it, context) } }

    LaunchedEffect(uiState.sessionId) {
        uiState.sessionId?.let { onSessionStarted(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (uiState.currentStep == 1) onBack()
                else viewModel.previousStep()
            }) {
                Icon(Icons.Default.ArrowBack, null, tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Setup Interview", style = MaterialTheme.typography.titleLarge,
                color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        StepIndicator(currentStep = uiState.currentStep, totalSteps = uiState.totalSteps)

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(visible = uiState.currentStep == 1) {
            CvAnalysisBanner(
                cvState = uiState.cvState,
                cvApplied = uiState.cvApplied,
                onSelectFile = { cvLauncher.launch(arrayOf("application/pdf")) },
                onApply = { (uiState.cvState as? Resource.Success)?.let { viewModel.applyCvRecommendations(it.data) } },
                onDismiss = viewModel::dismissCv
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            modifier = Modifier.weight(1f),
            label = "setup_step"
        ) { step ->
            when (step) {
                1 -> Step1InterviewType(uiState.selectedType, viewModel::selectType)
                2 -> Step2Position(uiState.selectedPosition, viewModel::onPositionChange)
                3 -> Step3Difficulty(uiState.difficulty, viewModel::selectDifficulty)
                4 -> Step4QuestionCount(uiState.questionCount, viewModel::onQuestionCountChange)
                5 -> Step5Confirm(uiState)
            }
        }

        uiState.error?.let {
            Text(it, color = Rose, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp))
        }

        val canProceed = when (uiState.currentStep) {
            1 -> uiState.selectedType != null
            2 -> uiState.selectedPosition.isNotBlank()
            else -> true
        }

        Button(
            onClick = {
                if (uiState.currentStep < uiState.totalSteps) viewModel.nextStep()
                else viewModel.startSession()
            },
            enabled = canProceed && !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    if (uiState.currentStep < uiState.totalSteps) "Next" else "Start Interview",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (index < currentStep) ElectricBlue else NavyBorder)
            )
        }
    }
    Text(
        "Step $currentStep of $totalSteps",
        style = MaterialTheme.typography.labelMedium,
        color = TextSecondary,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp)
    )
}

@Composable
private fun Step1InterviewType(selected: InterviewType?, onSelect: (InterviewType) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Select Interview Type", style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("What kind of interview do you want to practice?",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        listOf(
            Triple(InterviewType.TECHNICAL, Icons.Default.Code, "Algorithms, architecture, debugging"),
            Triple(InterviewType.HR, Icons.Default.People, "Behavioral, motivational, situational"),
            Triple(InterviewType.DOMAIN, Icons.Default.BarChart, "Finance, marketing, and more")
        ).forEach { (type, icon, desc) ->
            InterviewTypeCard(
                type = type,
                icon = icon,
                description = desc,
                isSelected = selected == type,
                onClick = { onSelect(type) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun InterviewTypeCard(
    type: InterviewType,
    icon: ImageVector,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale = if (isSelected) 1.02f else 1f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricBlue else NavyBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElectricBlue.copy(alpha = 0.1f) else NavyCard
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (isSelected) ElectricBlue else TextSecondary,
                modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(type.display, style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun Step2Position(position: String, onPositionChange: (String) -> Unit) {
    val commonPositions = listOf(
        "Software Engineer", "Data Scientist", "Product Manager",
        "UX Designer", "DevOps Engineer", "Frontend Developer",
        "Backend Developer", "Machine Learning Engineer"
    )
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Job Position", style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Select or type your target position", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = position,
            onValueChange = onPositionChange,
            label = { Text("Position", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Work, null, tint = ElectricBlue) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = NavyBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = ElectricBlue
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        commonPositions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                row.forEach { pos ->
                    FilterChip(
                        selected = position == pos,
                        onClick = { onPositionChange(pos) },
                        label = { Text(pos, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricBlue.copy(alpha = 0.2f),
                            selectedLabelColor = ElectricBlue
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun Step3Difficulty(selected: Difficulty, onSelect: (Difficulty) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Difficulty Level", style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Choose the level that matches your experience",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Difficulty.values().forEach { diff ->
                val isSelected = selected == diff
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) ElectricBlue else NavyBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.1f) else NavyCard)
                        .clickable { onSelect(diff) }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(diff.display, color = if (isSelected) ElectricBlue else TextSecondary,
                        style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        val description = when (selected) {
            Difficulty.JUNIOR -> "Entry-level questions focusing on fundamentals and basic concepts."
            Difficulty.MID -> "Intermediate questions covering real-world problem solving."
            Difficulty.SENIOR -> "Advanced questions on architecture, trade-offs, and leadership."
        }
        Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun Step4QuestionCount(count: Int, onCountChange: (Int) -> Unit) {
    val options = listOf(5, 10, 15, 20)
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Number of Questions", style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("How many questions do you want?",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(40.dp))
        Text("$count questions", style = MaterialTheme.typography.displaySmall,
            color = ElectricBlue, fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("~${count * 2} minutes", style = MaterialTheme.typography.bodyLarge, color = TextSecondary,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Slider(
            value = options.indexOf(count).toFloat(),
            onValueChange = { onCountChange(options[it.toInt()]) },
            valueRange = 0f..(options.size - 1).toFloat(),
            steps = options.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = ElectricBlue,
                activeTrackColor = ElectricBlue,
                inactiveTrackColor = NavyBorder
            )
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            options.forEach { opt ->
                Text("$opt", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun Step5Confirm(state: SetupUiState) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("Ready to Start?", style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("Review your setup before beginning",
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NavyCard)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ConfirmRow("Type", state.selectedType?.display ?: "-")
                ConfirmRow("Position", state.selectedPosition.ifBlank { "-" })
                ConfirmRow("Difficulty", state.difficulty.display)
                ConfirmRow("Questions", "${state.questionCount}")
            }
        }
    }
}

@Composable
private fun ConfirmRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CvAnalysisBanner(
    cvState: Resource<com.pfa.interview.data.remote.dto.CvAnalysisResponse>?,
    cvApplied: Boolean,
    onSelectFile: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    if (cvApplied) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Analyser mon CV", style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "L'IA détecte le type d'entretien et la difficulté adaptés à ton profil",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary
            )

            when (cvState) {
                is Resource.Loading -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ElectricBlue, trackColor = NavyBorder
                )
                is Resource.Error -> {
                    Text(cvState.message ?: "Erreur", color = Rose, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = onSelectFile,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue)
                    ) { Text("Réessayer", style = MaterialTheme.typography.labelSmall) }
                }
                is Resource.Success -> {
                    val data = cvState.data
                    HorizontalDivider(color = NavyBorder)
                    Text(
                        "Type : ${data.type} • ${data.difficulty} • ${data.position}",
                        style = MaterialTheme.typography.labelMedium, color = Emerald
                    )
                    Text(data.reasoning, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onApply,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            modifier = Modifier.weight(1f)
                        ) { Text("Appliquer", style = MaterialTheme.typography.labelSmall) }
                        OutlinedButton(
                            onClick = onDismiss,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NavyBorder),
                            modifier = Modifier.weight(1f)
                        ) { Text("Ignorer", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                null -> OutlinedButton(
                    onClick = onSelectFile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.UploadFile, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sélectionner mon CV (PDF)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun SetupScreenPreview() {
    InterviewSimulatorTheme {
        SetupScreen(onSessionStarted = {}, onBack = {})
    }
}
