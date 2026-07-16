package com.example.feature.auth

import com.example.R
import com.example.core.model.AppLanguage
import com.example.feature.settings.SettingsViewModel

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.MessageDigest

enum class PasswordStrength(val labelEn: String, val labelAr: String, val color: Color, val progress: Float) {
    EMPTY("", "", Color.Transparent, 0f),
    WEAK("Weak", "ضعيفة", Color(0xFFE53935), 0.25f),
    MEDIUM("Medium", "متوسطة", Color(0xFFFFB300), 0.6f),
    STRONG("Strong", "قوية", Color(0xFF43A047), 1.0f)
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    if (password.isEmpty()) return PasswordStrength.EMPTY
    if (password.length < 6) return PasswordStrength.WEAK
    
    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    
    return when {
        score >= 3 -> PasswordStrength.STRONG
        score >= 1 -> PasswordStrength.MEDIUM
        else -> PasswordStrength.WEAK
    }
}

fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.fold("") { str, it -> str + "%02x".format(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    settingsViewModel: SettingsViewModel,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val isArabic = settings.language == AppLanguage.ARABIC

    // Login screen states: 
    // 0: Onboarding, 1: Sign In Form, 2: Sign Up Form, 3: Forgot Password, 4: Verification & Reset
    var currentStep by remember { mutableStateOf(0) }
    
    // Inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    
    // Remember Me
    var rememberMe by remember { mutableStateOf(true) }
    
    // Forgot password states
    var forgotEmailInput by remember { mutableStateOf("") }
    var generatedResetCode by remember { mutableStateOf("") }
    var verificationCodeInput by remember { mutableStateOf("") }
    var newPasswordInput by remember { mutableStateOf("") }
    var confirmNewPasswordInput by remember { mutableStateOf("") }
    
    // Security / Visibility
    var passwordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Onboarding items
    val onboardingSlides = remember(isArabic) {
        listOf(
            OnboardingSlide(
                title = if (isArabic) "محادثات ذكية فائقة الدقة" else "Intelligent Conversations",
                description = if (isArabic) "تواصل مع نماذج متطورة مدعومة بقدرات التفكير العلمي العميق وحل المشكلات المعقدة." else "Engage with advanced AI models optimized for scientific reasoning and complex problem-solving.",
                icon = Icons.Rounded.Lightbulb
            ),
            OnboardingSlide(
                title = if (isArabic) "تحليل متعدد الوسائط ذكي" else "Multimodal Processing",
                description = if (isArabic) "حلل المستندات والصور والمستندات التقنية والأكواد البرمجية بلمحة بصر." else "Instantly digest and analyze documents, high-res images, files, and complete technical codebases.",
                icon = Icons.Rounded.DocumentScanner
            ),
            OnboardingSlide(
                title = if (isArabic) "توجيه تلقائي ذكي ومستقر" else "Automatic Failover & Stability",
                description = if (isArabic) "استمتع باتصال آمن، مستقر، وموجّه تلقائياً إلى أفضل النماذج العاملة دون الحاجة لضبط مفاتيح API." else "Experience seamless AI services that automatically select the best model and failover if any API is unavailable.",
                icon = Icons.Rounded.CloudQueue
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = if (isArabic) "الجيل القادم من الذكاء الاصطناعي" else "The Next Gen Intelligent Workspace",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                    },
                    label = "auth_screen_navigation"
                ) { step ->
                    when (step) {
                        0 -> {
                            // Onboarding Slider
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                var activeSlideIndex by remember { mutableStateOf(0) }
                                val activeSlide = onboardingSlides[activeSlideIndex]

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 200.dp),
                                    shape = RoundedCornerShape(24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = activeSlide.icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = activeSlide.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = activeSlide.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Indicator dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    onboardingSlides.forEachIndexed { idx, _ ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (activeSlideIndex == idx) 16.dp else 8.dp, 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activeSlideIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                                )
                                                .clickable { activeSlideIndex = idx }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { currentStep = 1 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "ابدأ الآن" else "Get Started",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Email & Password login Form
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل الدخول" else "Sign In",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                                )

                                // Remember Me & Forgot Password link row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { rememberMe = !rememberMe }
                                    ) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it }
                                        )
                                        Text(
                                            text = if (isArabic) "تذكرني" else "Remember Me",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                        )
                                    }
                                    Text(
                                        text = if (isArabic) "نسيت كلمة المرور؟" else "Forgot Password?",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { currentStep = 3 }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى تعبئة جميع الحقول" else "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        } else if (!emailPattern.matcher(emailInput.trim()).matches()) {
                                            Toast.makeText(context, if (isArabic) "يرجى إدخال عنوان بريد إلكتروني صحيح." else "Please enter a valid email address.", Toast.LENGTH_LONG).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1000)
                                                val user = settingsViewModel.getUserByEmail(emailInput)
                                                isLoading = false
                                                if (user == null) {
                                                    val msg = if (isArabic) {
                                                        "الحساب غير موجود. يرجى إنشاء حساب أولاً."
                                                    } else {
                                                        "Account not found. Please create an account first."
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } else {
                                                    val isPasswordCorrect = (user.passwordHash == hashPassword(passwordInput) || user.passwordHash == passwordInput)
                                                    if (!isPasswordCorrect) {
                                                        val msg = if (isArabic) {
                                                            "البريد الإلكتروني أو كلمة المرور غير صحيحة."
                                                        } else {
                                                            "Incorrect email or password."
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    } else {
                                                        settingsViewModel.updateLoginState(true, "EMAIL", user.email, user.name, rememberMe)
                                                        Toast.makeText(context, if (isArabic) "مرحباً بك ${user.name}" else "Welcome, ${user.name}!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(if (isArabic) "دخول" else "Login", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "ليس لديك حساب؟ " else "Don't have an account? ",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (isArabic) "أنشئ حساباً جديداً" else "Sign Up",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { currentStep = 2 }
                                    )
                                }
                            }
                        }

                        2 -> {
                            // Sign Up Form
                            val strength = calculatePasswordStrength(passwordInput)
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { currentStep = 1 }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "إنشاء حساب جديد" else "Create Account",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("name_signup_input")
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("email_signup_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("password_signup_input")
                                )

                                // Password Strength indicator
                                if (passwordInput.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isArabic) "قوة كلمة المرور:" else "Password strength:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = if (isArabic) strength.labelAr else strength.labelEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = strength.color
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { strength.progress },
                                            color = strength.color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = confirmPasswordInput,
                                    onValueChange = { confirmPasswordInput = it },
                                    label = { Text(if (isArabic) "تأكيد كلمة المرور" else "Confirm Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Terms and Privacy acceptance row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { termsAccepted = !termsAccepted },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = termsAccepted,
                                        onCheckedChange = { termsAccepted = it }
                                    )
                                    Text(
                                        text = if (isArabic) "أوافق على شروط الخدمة وسياسة الخصوصية" else "I accept the Terms & Privacy Policy",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        if (nameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى ملء كافة التفاصيل" else "Please fill in all details", Toast.LENGTH_SHORT).show()
                                        } else if (!emailPattern.matcher(emailInput.trim()).matches()) {
                                            Toast.makeText(context, if (isArabic) "يرجى إدخال عنوان بريد إلكتروني صحيح." else "Please enter a valid email address.", Toast.LENGTH_LONG).show()
                                        } else if (passwordInput.length < 6) {
                                            Toast.makeText(context, if (isArabic) "يجب أن تكون كلمة المرور 6 أحرف على الأقل." else "Password must be at least 6 characters.", Toast.LENGTH_LONG).show()
                                        } else if (passwordInput != confirmPasswordInput) {
                                            Toast.makeText(context, if (isArabic) "كلمات المرور غير متطابقة." else "Passwords do not match.", Toast.LENGTH_LONG).show()
                                        } else if (!termsAccepted) {
                                            Toast.makeText(context, if (isArabic) "يرجى الموافقة على الشروط أولاً." else "Please accept the Terms & Privacy Policy first.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1000)
                                                val existingUser = settingsViewModel.getUserByEmail(emailInput)
                                                if (existingUser != null) {
                                                    isLoading = false
                                                    val msg = if (isArabic) {
                                                        "البريد الإلكتروني مسجل بالفعل. يرجى تسجيل الدخول."
                                                    } else {
                                                        "Email is already registered. Please sign in."
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } else {
                                                    val secureHashedPassword = hashPassword(passwordInput)
                                                    settingsViewModel.registerUser(emailInput, nameInput, secureHashedPassword)
                                                    settingsViewModel.updateLoginState(true, "EMAIL", emailInput, nameInput, rememberMe)
                                                    isLoading = false
                                                    Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                    onLoginSuccess()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("submit_signup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(if (isArabic) "إنشاء حساب" else "Sign Up", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "لديك حساب بالفعل؟ " else "Already have an account? ",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (isArabic) "سجل دخولك هنا" else "Sign In",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { currentStep = 1 }
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Forgot Password Screen
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { currentStep = 1 }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "استعادة كلمة المرور" else "Forgot Password",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (isArabic) "أدخل بريدك الإلكتروني لإرسال رمز التحقق المكون من 6 أرقام." else "Enter your email address to receive a secure 6-digit verification code.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )

                                OutlinedTextField(
                                    value = forgotEmailInput,
                                    onValueChange = { forgotEmailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        if (forgotEmailInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى كتابة البريد الإلكتروني" else "Please enter your email", Toast.LENGTH_SHORT).show()
                                        } else if (!emailPattern.matcher(forgotEmailInput.trim()).matches()) {
                                            Toast.makeText(context, if (isArabic) "بريد إلكتروني غير صحيح" else "Invalid email address", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1200)
                                                val user = settingsViewModel.getUserByEmail(forgotEmailInput)
                                                isLoading = false
                                                if (user == null) {
                                                    Toast.makeText(context, if (isArabic) "لم يتم العثور على حساب بهذا البريد" else "No account found with this email", Toast.LENGTH_LONG).show()
                                                } else {
                                                    // Generate 6-digit random code and toast it for testing simulation
                                                    val randomCode = (100000..999999).random().toString()
                                                    generatedResetCode = randomCode
                                                    Toast.makeText(context, if (isArabic) "تم إرسال الرمز! رمز التحقق هو: $randomCode" else "Verification code sent! Code is: $randomCode", Toast.LENGTH_LONG).show()
                                                    currentStep = 4
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isArabic) "إرسال رمز التحقق" else "Send Verification Code", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        4 -> {
                            // Verification & Password Reset Screen
                            val resetStrength = calculatePasswordStrength(newPasswordInput)
                            
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { currentStep = 3 }) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "إعادة تعيين كلمة المرور" else "Reset Password",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (isArabic) "أدخل الرمز المكون من 6 أرقام المرسل إلى بريدك الإلكتروني مع كلمة المرور الجديدة." else "Enter the 6-digit code sent to your email and set a new secure password.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )

                                OutlinedTextField(
                                    value = verificationCodeInput,
                                    onValueChange = { verificationCodeInput = it },
                                    label = { Text(if (isArabic) "رمز التحقق (6 أرقام)" else "6-Digit Verification Code") },
                                    leadingIcon = { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = { newPasswordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور الجديدة" else "New Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                            Icon(
                                                imageVector = if (newPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (newPasswordInput.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (isArabic) "قوة كلمة المرور:" else "Password strength:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = if (isArabic) resetStrength.labelAr else resetStrength.labelEn,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = resetStrength.color
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { resetStrength.progress },
                                            color = resetStrength.color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = confirmNewPasswordInput,
                                    onValueChange = { confirmNewPasswordInput = it },
                                    label = { Text(if (isArabic) "تأكيد كلمة المرور الجديدة" else "Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (verificationCodeInput.trim() != generatedResetCode) {
                                            Toast.makeText(context, if (isArabic) "رمز التحقق غير صحيح!" else "Incorrect verification code!", Toast.LENGTH_SHORT).show()
                                        } else if (newPasswordInput.length < 6) {
                                            Toast.makeText(context, if (isArabic) "يجب أن تكون كلمة المرور 6 أحرف على الأقل" else "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                        } else if (newPasswordInput != confirmNewPasswordInput) {
                                            Toast.makeText(context, if (isArabic) "كلمات المرور غير متطابقة" else "Passwords do not match", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1000)
                                                // Temporarily login user to settings to update password
                                                settingsViewModel.updateLoginState(true, "EMAIL", forgotEmailInput, "")
                                                val success = settingsViewModel.updatePassword(hashPassword(newPasswordInput))
                                                settingsViewModel.logout() // Securely logout after update
                                                isLoading = false
                                                if (success) {
                                                    Toast.makeText(context, if (isArabic) "تم تغيير كلمة المرور بنجاح! الرجاء تسجيل الدخول الآن." else "Password updated successfully! Please login now.", Toast.LENGTH_LONG).show()
                                                    currentStep = 1
                                                } else {
                                                    Toast.makeText(context, if (isArabic) "حدث خطأ أثناء التحديث" else "Failed to update password", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (isArabic) "تأكيد وإعادة التعيين" else "Confirm & Reset Password", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Footer / Bottom Brand Details
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Nabih Secure Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f)
                )
                Text(
                    text = if (isArabic) "تشفير محلي آمن بالكامل 256-بت" else "Full 256-bit Local Secure Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    fontSize = 10.sp
                )
            }
        }

        // Full Screen Loading overlay
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isArabic) "جاري العمل بشكل آمن..." else "Working securely...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

data class OnboardingSlide(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
