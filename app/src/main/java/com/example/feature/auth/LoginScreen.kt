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
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.UserProfileChangeRequest

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
    var currentStep by remember(settings.onboardingCompleted) {
        mutableStateOf(if (settings.onboardingCompleted) 1 else 0)
    }
    
    // Inputs
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    
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
    var isLoading by remember { mutableStateOf(false) }

    // FirebaseAuth Instance
    val firebaseAuth = remember { FirebaseAuth.getInstance() }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                scope.launch {
                    isLoading = true
                    try {
                        val credential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = firebaseAuth.signInWithCredential(credential).await()
                        val firebaseUser = authResult.user
                        val email = firebaseUser?.email ?: ""
                        val displayName = firebaseUser?.displayName ?: "Google User"
                        
                        // Also ensure registered in local Room db to keep compat with chat/settings
                        val localUser = settingsViewModel.getUserByEmail(email)
                        if (localUser == null) {
                            settingsViewModel.registerUser(email, displayName, "google_auth")
                        }
                        
                        settingsViewModel.updateLoginState(true, "GOOGLE", email, displayName, true)
                        Toast.makeText(context, if (isArabic) "مرحباً بك $displayName" else "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } catch (e: Exception) {
                        android.util.Log.e("LoginScreen", "Firebase Google Auth Error", e)
                        val msg = e.localizedMessage ?: (if (isArabic) "فشل تسجيل الدخول عبر Google" else "Google login failed")
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            } else {
                Toast.makeText(context, if (isArabic) "فشل الحصول على رمز تعريف Google" else "Failed to get Google ID Token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode != com.google.android.gms.common.api.CommonStatusCodes.CANCELED &&
                e.statusCode != 12501
            ) {
                android.util.Log.e("LoginScreen", "Google Sign In Error", e)
                val msg = if (isArabic) "فشل تسجيل الدخول عبر Google (${e.statusCode})" else "Google Sign-In failed (${e.statusCode})"
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    // local error states for Sign Up Form
    var nameSignUpError by remember { mutableStateOf<String?>(null) }
    var emailSignUpError by remember { mutableStateOf<String?>(null) }
    var passwordSignUpError by remember { mutableStateOf<String?>(null) }

    // local error states for Sign In Form
    var emailSignInError by remember { mutableStateOf<String?>(null) }
    var passwordSignInError by remember { mutableStateOf<String?>(null) }

    // Focus states for SignUp Form
    var isNameSignUpFocused by remember { mutableStateOf(false) }
    var isEmailSignUpFocused by remember { mutableStateOf(false) }
    var isPasswordSignUpFocused by remember { mutableStateOf(false) }

    // Focus states for SignIn Form
    var isEmailSignInFocused by remember { mutableStateOf(false) }
    var isPasswordSignInFocused by remember { mutableStateOf(false) }

    // Reset validation errors when currentStep changes
    LaunchedEffect(currentStep) {
        nameSignUpError = null
        emailSignUpError = null
        passwordSignUpError = null
        emailSignInError = null
        passwordSignInError = null
    }

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
                    Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(72.dp))
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

                                // Visually fixed Skip button container at the top
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp),
                                    contentAlignment = if (isArabic) Alignment.TopStart else Alignment.TopEnd
                                ) {
                                    if (activeSlideIndex < onboardingSlides.size - 1) {
                                        Text(
                                            text = if (isArabic) "تخطي" else "Skip",
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .clickable {
                                                    settingsViewModel.updateOnboardingCompleted(true)
                                                    currentStep = 1
                                                }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                .testTag("onboarding_skip_button")
                                        )
                                    }
                                }

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
                                    onClick = {
                                        if (activeSlideIndex < onboardingSlides.size - 1) {
                                            activeSlideIndex++
                                        } else {
                                            settingsViewModel.updateOnboardingCompleted(true)
                                            currentStep = 1
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("onboarding_continue_button"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (activeSlideIndex < onboardingSlides.size - 1) {
                                            if (isArabic) "التالي" else "Next"
                                        } else {
                                            if (isArabic) "ابدأ الآن" else "Get Started"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        1 -> {
                            // Email & Password login Form
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل الدخول" else "Sign In",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Start
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(
                                        value = emailInput,
                                        onValueChange = { 
                                            emailInput = it
                                            emailSignInError = null
                                        },
                                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        isError = emailSignInError != null,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("email_input")
                                    )
                                    if (emailSignInError != null) {
                                        Text(
                                            text = emailSignInError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(
                                        value = passwordInput,
                                        onValueChange = { 
                                            passwordInput = it
                                            passwordSignInError = null
                                        },
                                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { passwordVisible = !passwordVisible },
                                                modifier = Modifier.size(48.dp) // Large touch target
                                            ) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        singleLine = true,
                                        isError = passwordSignInError != null,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("password_input")
                                    )
                                    if (passwordSignInError != null) {
                                        Text(
                                            text = passwordSignInError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

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

                                Spacer(modifier = Modifier.height(16.dp))

                                val isSignInButtonEnabled = emailInput.isNotBlank() && passwordInput.isNotBlank()

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        var hasError = false
                                        if (emailInput.isBlank()) {
                                            emailSignInError = if (isArabic) "البريد الإلكتروني مطلوب" else "Email is required"
                                            hasError = true
                                        } else if (!emailPattern.matcher(emailInput.trim()).matches()) {
                                            emailSignInError = if (isArabic) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email address format"
                                            hasError = true
                                        }

                                        if (passwordInput.isBlank()) {
                                            passwordSignInError = if (isArabic) "كلمة المرور مطلوبة" else "Password is required"
                                            hasError = true
                                        }

                                        if (!hasError) {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val trimmedEmail = emailInput.trim()
                                                    val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                    val firebaseUser = authResult.user
                                                    
                                                    // Also ensure registered in local Room db to keep compat with chat/settings
                                                    val localUser = settingsViewModel.getUserByEmail(trimmedEmail)
                                                    if (localUser == null) {
                                                        settingsViewModel.registerUser(trimmedEmail, firebaseUser?.displayName ?: "User", "firebase_auth")
                                                    }
                                                    
                                                    settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, firebaseUser?.displayName ?: "User", rememberMe)
                                                    Toast.makeText(context, if (isArabic) "مرحباً بك ${firebaseUser?.displayName ?: ""}" else "Welcome, ${firebaseUser?.displayName ?: ""}!", Toast.LENGTH_SHORT).show()
                                                    onLoginSuccess()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("LoginScreen", "FirebaseAuth Sign In Error", e)
                                                    val msg = when (e) {
                                                        is com.google.firebase.auth.FirebaseAuthInvalidUserException,
                                                        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> {
                                                            if (isArabic) "البريد الإلكتروني أو كلمة المرور غير صحيحة" else "Incorrect email or password"
                                                        }
                                                        is com.google.firebase.FirebaseNetworkException -> {
                                                            if (isArabic) "تعذر الاتصال، تحقق من الإنترنت وحاول مرة أخرى" else "Network error. Please check your internet connection."
                                                        }
                                                        else -> e.localizedMessage ?: (if (isArabic) "فشل تسجيل الدخول" else "Login failed")
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = isSignInButtonEnabled && !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(if (isArabic) "دخول" else "Login", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                    )
                                    Text(
                                        text = if (isArabic) "أو" else "OR",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""
                                        if (webClientId.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                if (isArabic) "مفتاح Google Client ID غير متوفر في ملف google-services.json" else "Google Client ID is not configured in google-services.json",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(webClientId)
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            googleSignInClient.signOut()
                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1F1F1F)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google),
                                            contentDescription = "Google Icon",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "المتابعة عبر Google" else "Continue with Google",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
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
                                verticalArrangement = Arrangement.spacedBy(16.dp),
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

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(
                                        value = nameInput,
                                        onValueChange = { 
                                            nameInput = it
                                            nameSignUpError = null
                                        },
                                        label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        singleLine = true,
                                        isError = nameSignUpError != null,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("name_signup_input")
                                    )
                                    if (nameSignUpError != null) {
                                        Text(
                                            text = nameSignUpError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(
                                        value = emailInput,
                                        onValueChange = { 
                                            emailInput = it
                                            emailSignUpError = null
                                        },
                                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                        singleLine = true,
                                        isError = emailSignUpError != null,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("email_signup_input")
                                    )
                                    if (emailSignUpError != null) {
                                        Text(
                                            text = emailSignUpError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextField(
                                        value = passwordInput,
                                        onValueChange = { 
                                            passwordInput = it
                                            passwordSignUpError = null
                                        },
                                        label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingIcon = {
                                            IconButton(
                                                onClick = { passwordVisible = !passwordVisible },
                                                modifier = Modifier.size(48.dp) // Large touch target
                                            ) {
                                                Icon(
                                                    imageVector = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                        singleLine = true,
                                        isError = passwordSignUpError != null,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            disabledContainerColor = Color.Transparent,
                                            errorContainerColor = Color.Transparent,
                                            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                            unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("password_signup_input")
                                    )
                                    if (passwordSignUpError != null) {
                                        Text(
                                            text = passwordSignUpError!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

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

                                Spacer(modifier = Modifier.height(16.dp))

                                val isSignUpButtonEnabled = nameInput.isNotBlank() && emailInput.isNotBlank() && passwordInput.isNotBlank()

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        var hasError = false
                                        if (nameInput.isBlank()) {
                                            nameSignUpError = if (isArabic) "الاسم الكامل مطلوب" else "Full name is required"
                                            hasError = true
                                        }

                                        if (emailInput.isBlank()) {
                                            emailSignUpError = if (isArabic) "البريد الإلكتروني مطلوب" else "Email is required"
                                            hasError = true
                                        } else if (!emailPattern.matcher(emailInput.trim()).matches()) {
                                            emailSignUpError = if (isArabic) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email address format"
                                            hasError = true
                                        }

                                        if (passwordInput.isBlank()) {
                                            passwordSignUpError = if (isArabic) "كلمة المرور مطلوبة" else "Password is required"
                                            hasError = true
                                        } else if (passwordInput.length < 6) {
                                            passwordSignUpError = if (isArabic) "يجب أن تكون كلمة المرور 6 أحرف على الأقل" else "Password must be at least 6 characters"
                                            hasError = true
                                        }

                                        if (!hasError) {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val trimmedEmail = emailInput.trim()
                                                    val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                    val firebaseUser = authResult.user
                                                    
                                                    // Set display name in Firebase Auth
                                                    val profileUpdates = UserProfileChangeRequest.Builder()
                                                        .setDisplayName(nameInput.trim())
                                                        .build()
                                                    firebaseUser?.updateProfile(profileUpdates)?.await()
                                                    
                                                    // Register in local Room db to keep compat with chat/settings
                                                    val secureHashedPassword = hashPassword(passwordInput)
                                                    settingsViewModel.registerUser(trimmedEmail, nameInput.trim(), secureHashedPassword)
                                                    settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, nameInput.trim(), rememberMe)
                                                    
                                                    Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                    onLoginSuccess()
                                                } catch (e: FirebaseAuthUserCollisionException) {
                                                    val msg = if (isArabic) "هذا البريد الإلكتروني مسجّل بالفعل" else "This email is already registered"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } catch (e: FirebaseAuthWeakPasswordException) {
                                                    val msg = if (isArabic) "كلمة المرور ضعيفة للغاية" else "Password is too weak"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } catch (e: FirebaseAuthInvalidCredentialsException) {
                                                    val msg = if (isArabic) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email address format"
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } catch (e: Exception) {
                                                    android.util.Log.e("LoginScreen", "FirebaseAuth Sign Up Error", e)
                                                    val msg = e.localizedMessage ?: (if (isArabic) "حدث خطأ غير متوقع" else "An unexpected error occurred")
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    enabled = isSignUpButtonEnabled && !isLoading,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("submit_signup_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(if (isArabic) "إنشاء حساب" else "Sign Up", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                    )
                                    Text(
                                        text = if (isArabic) "أو" else "OR",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                                        val webClientId = if (webClientIdResId != 0) context.getString(webClientIdResId) else ""
                                        if (webClientId.isEmpty()) {
                                            Toast.makeText(
                                                context,
                                                if (isArabic) "مفتاح Google Client ID غير متوفر في ملف google-services.json" else "Google Client ID is not configured in google-services.json",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                .requestIdToken(webClientId)
                                                .requestEmail()
                                                .build()
                                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                            googleSignInClient.signOut()
                                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF1F1F1F)
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google),
                                            contentDescription = "Google Icon",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "المتابعة عبر Google" else "Continue with Google",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
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

                                TextField(
                                    value = forgotEmailInput,
                                    onValueChange = { forgotEmailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error
                                    ),
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

                                TextField(
                                    value = verificationCodeInput,
                                    onValueChange = { verificationCodeInput = it },
                                    label = { Text(if (isArabic) "رمز التحقق (6 أرقام)" else "6-Digit Verification Code") },
                                    leadingIcon = { Icon(Icons.Rounded.VpnKey, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                TextField(
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
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error
                                    ),
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

                                TextField(
                                    value = confirmNewPasswordInput,
                                    onValueChange = { confirmNewPasswordInput = it },
                                    label = { Text(if (isArabic) "تأكيد كلمة المرور الجديدة" else "Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        errorContainerColor = Color.Transparent,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        errorIndicatorColor = MaterialTheme.colorScheme.error
                                    ),
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
