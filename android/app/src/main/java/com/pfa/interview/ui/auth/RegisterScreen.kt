package com.pfa.interview.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pfa.interview.ui.theme.*

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onRegisterSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(DeepNavy, NavyCard)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text("Create Account", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Start your interview prep journey", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth().shadow(16.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NavyCard.copy(alpha = 0.85f))
            ) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = viewModel::onNameChange,
                        label = { Text("Full Name", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = ElectricBlue) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = viewModel::onEmailChange,
                        label = { Text("Email", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = ElectricBlue) },
                        trailingIcon = {
                            if (uiState.email.contains("@") && uiState.email.contains(".")) {
                                Icon(Icons.Default.CheckCircle, null, tint = Emerald)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    var passwordVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = viewModel::onPasswordChange,
                        label = { Text("Password", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = ElectricBlue) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    if (uiState.password.isNotEmpty()) {
                        PasswordStrengthBar(strength = uiState.passwordStrength)
                    }

                    var confirmVisible by remember { mutableStateOf(false) }
                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = { Text("Confirm Password", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = ElectricBlue) },
                        trailingIcon = {
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(
                                    if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    null, tint = TextSecondary
                                )
                            }
                        },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = textFieldColors()
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = uiState.termsAccepted,
                            onCheckedChange = viewModel::onTermsToggle,
                            colors = CheckboxDefaults.colors(checkedColor = ElectricBlue)
                        )
                        Text("I agree to the ", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Text("Terms & Conditions", color = BlueLight, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { })
                    }

                    AnimatedVisibility(visible = uiState.error != null) {
                        uiState.error?.let {
                            Text(it, color = Rose, style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                    }

                    val isFormValid = uiState.name.isNotBlank() && uiState.email.isNotBlank() &&
                            uiState.password.length >= 6 && uiState.password == uiState.confirmPassword &&
                            uiState.termsAccepted

                    Button(
                        onClick = viewModel::register,
                        enabled = isFormValid && !uiState.isLoading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            disabledContainerColor = NavyBorder
                        )
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Create Account", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row {
                Text("Already have an account? ", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Text("Login", color = BlueLight, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onNavigateToLogin() })
            }
        }
    }
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val (color, label) = when (strength) {
        PasswordStrength.NONE -> Pair(TextMuted, "")
        PasswordStrength.WEAK -> Pair(Rose, "Weak")
        PasswordStrength.FAIR -> Pair(Amber, "Fair")
        PasswordStrength.STRONG -> Pair(Emerald, "Strong")
        PasswordStrength.VERY_STRONG -> Pair(EmeraldLight, "Very Strong")
    }
    val bars = when (strength) {
        PasswordStrength.NONE -> 0
        PasswordStrength.WEAK -> 1
        PasswordStrength.FAIR -> 2
        PasswordStrength.STRONG -> 3
        PasswordStrength.VERY_STRONG -> 4
    }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            if (index < bars) color else NavyBorder,
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        }
        if (label.isNotEmpty()) {
            Text(label, color = color, style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ElectricBlue,
    unfocusedBorderColor = NavyBorder,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = ElectricBlue
)

@Preview(showBackground = true, backgroundColor = 0xFF0A0E1A)
@Composable
private fun RegisterScreenPreview() {
    InterviewSimulatorTheme {
        RegisterScreen(onNavigateToLogin = {}, onRegisterSuccess = {})
    }
}
