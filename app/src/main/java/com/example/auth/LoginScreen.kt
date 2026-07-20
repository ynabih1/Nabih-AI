package com.example.auth

import com.example.R
import androidx.compose.foundation.border
import com.example.model.AppLanguage
import com.example.settings.SettingsViewModel

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
import androidx.compose.foundation.text.KeyboardActions
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
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.UserProfileChangeRequest

enum class PasswordStrength(val labelEn: String, val labelAr: String, val color: Color, val progress: Float) {
    EMPTY("", "", Color.Transparent, 0f),
    WEAK("Weak", "ضعيفة", Color(0xFFE53935), 0.25f),
    MEDIUM("Medium", "متوسطة", Color(0xFFFFB300), 0.6f),
    STRONG("Strong", "قوية", Color(0xFF2563EB), 1.0f)
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
                val msg = if (e.statusCode == 10) { // 10 = DEVELOPER_ERROR
                    if (isArabic) "حدثت مشكلة في إعدادات تسجيل الدخول (SHA-1 / Client ID)، يرجى التواصل مع المطور. (رمز: 10)" 
                    else "Login configuration issue (SHA-1 / Client ID), please contact the developer. (Code: 10)"
                } else {
                    if (isArabic) "فشل تسجيل الدخول عبر Google (${e.statusCode})" else "Google Sign-In failed (${e.statusCode})"
                }
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
    var isPasswordSignUpFocused by remember { mutableStateOf(false) }
    var isNameSignUpFocused by remember { mutableStateOf(false) }
    var isEmailSignUpFocused by remember { mutableStateOf(false) }

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
                .imePadding()
                .padding(24.dp), // Removed verticalScroll to allow weight to push content down
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(8.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logo), 
                        contentDescription = null, 
                        tint = Color.Unspecified, 
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArabic) "مساعدك الذكي لكل شيء" else "Your smart assistant for everything",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // Pushes the rest to the bottom

            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp)
                    .verticalScroll(rememberScrollState()), // Moved scroll state here
                contentAlignment = Alignment.TopCenter
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
                            // Onboarding
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                var activeSlideIndex by remember { mutableStateOf(0) }
                                val activeSlide = onboardingSlides[activeSlideIndex]

                                AnimatedContent(
                                    targetState = activeSlideIndex,
                                    transitionSpec = {
                                        slideInHorizontally(
                                            initialOffsetX = { fullWidth -> if (targetState > initialState) fullWidth else -fullWidth },
                                            animationSpec = tween(400)
                                        ) togetherWith slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> if (targetState > initialState) -fullWidth else fullWidth },
                                            animationSpec = tween(400)
                                        )
                                    },
                                    label = "onboarding_animation"
                                ) { idx ->
                                    val slide = onboardingSlides[idx]
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = slide.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Text(
                                            text = slide.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = slide.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 20.sp
                                        )
                                    }
                                }

                                // Indicator dots
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 20.dp)
                                ) {
                                    onboardingSlides.forEachIndexed { idx, _ ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (activeSlideIndex == idx) 24.dp else 8.dp, 8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (activeSlideIndex == idx) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                )
                                                .clickable { activeSlideIndex = idx }
                                        )
                                    }
                                }

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
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
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
                            // Main Email Input Screen
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Google Login Button
                                Button(
                                    onClick = {
                                        val webClientId = context.getString(R.string.default_web_client_id)
                                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                            .requestIdToken(webClientId)
                                            .requestEmail()
                                            .build()
                                        val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                                        googleSignInClient.signOut()
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isArabic) "المتابعة باستخدام Google" else "Continue with Google",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_google),
                                                contentDescription = "Google Icon",
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // Divider
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = if (isArabic) "أو" else "OR",
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                                }

                                // Email Input
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { 
                                        emailInput = it 
                                        emailSignInError = null 
                                    },
                                    placeholder = { Text(if (isArabic) "أدخل بريدك الإلكتروني" else "Enter your email") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { 
                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                            isLoading = true
                                            firebaseAuth.fetchSignInMethodsForEmail(emailInput)
                                                .addOnCompleteListener { task ->
                                                    isLoading = false
                                                    if (task.isSuccessful) {
                                                        val isNewUser = task.result?.signInMethods?.isEmpty() ?: true
                                                        if (isNewUser) {
                                                            currentStep = 2 // Sign up
                                                        } else {
                                                            currentStep = 3 // Sign in
                                                        }
                                                    } else {
                                                        emailSignInError = task.exception?.localizedMessage ?: "Error verifying email"
                                                    }
                                                }
                                        }
                                    }),
                                    singleLine = true,
                                    isError = emailSignInError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )

                                if (emailSignInError != null) {
                                    Text(
                                        text = emailSignInError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 16.dp)
                                    )
                                }

                                // Continue Button
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches(),
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                                ) {
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            firebaseAuth.fetchSignInMethodsForEmail(emailInput)
                                                .addOnCompleteListener { task ->
                                                    isLoading = false
                                                    if (task.isSuccessful) {
                                                        val isNewUser = task.result?.signInMethods?.isEmpty() ?: true
                                                        if (isNewUser) {
                                                            currentStep = 2 // Sign up
                                                        } else {
                                                            currentStep = 3 // Sign in
                                                        }
                                                    } else {
                                                        emailSignInError = task.exception?.localizedMessage ?: "Error verifying email"
                                                    }
                                                }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp),
                                        shape = RoundedCornerShape(28.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        } else {
                                            Text(
                                                text = if (isArabic) "متابعة" else "Continue",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                // Legal Text
                                Text(
                                    text = if (isArabic) "بالمتابعة، أنت توافق على شروط الاستخدام وسياسة الخصوصية" else "By continuing, you agree to the Terms of Use and Privacy Policy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )
                            }
                        }

                        2 -> {
                            // Short Sign Up Form (Name + Password)
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
                                        text = if (isArabic) "إنشاء حساب" else "Create Account",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it; nameSignUpError = null },
                                    placeholder = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    isError = nameSignUpError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )
                                if (nameSignUpError != null) {
                                    Text(text = nameSignUpError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                                }

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it; passwordSignUpError = null },
                                    placeholder = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    isError = passwordSignUpError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )
                                if (passwordSignUpError != null) {
                                    Text(text = passwordSignUpError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                                }

                                Button(
                                    onClick = {
                                        if (nameInput.trim().isEmpty()) {
                                            nameSignUpError = if (isArabic) "الاسم مطلوب" else "Name is required"
                                            return@Button
                                        }
                                        if (passwordInput.length < 6) {
                                            passwordSignUpError = if (isArabic) "كلمة المرور قصيرة جداً (6 أحرف على الأقل)" else "Password too short (min 6 chars)"
                                            return@Button
                                        }
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                val authResult = firebaseAuth.createUserWithEmailAndPassword(emailInput, passwordInput).await()
                                                val user = authResult.user
                                                val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                    .setDisplayName(nameInput.trim())
                                                    .build()
                                                user?.updateProfile(profileUpdates)?.await()
                                                
                                                settingsViewModel.registerUser(emailInput, nameInput.trim(), passwordInput)
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, nameInput.trim(), rememberMe)
                                                
                                                Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } catch (e: Exception) {
                                                android.util.Log.e("LoginScreen", "Firebase Sign Up Error", e)
                                                passwordSignUpError = when (e) {
                                                    is FirebaseAuthWeakPasswordException -> if (isArabic) "كلمة المرور ضعيفة جداً" else "Password is too weak"
                                                    is FirebaseAuthInvalidCredentialsException -> if (isArabic) "صيغة البريد الإلكتروني غير صحيحة" else "Invalid email format"
                                                    is FirebaseAuthUserCollisionException -> if (isArabic) "البريد الإلكتروني مستخدم بالفعل" else "Email is already in use"
                                                    else -> e.localizedMessage ?: (if (isArabic) "فشل إنشاء الحساب" else "Sign up failed")
                                                }
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text(text = if (isArabic) "تسجيل حساب جديد" else "Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        3 -> {
                            // Password Login Screen
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
                                        text = if (isArabic) "مرحباً بعودتك" else "Welcome Back",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it; passwordSignInError = null },
                                    placeholder = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    isError = passwordSignInError != null,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        errorContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )
                                if (passwordSignInError != null) {
                                    Text(text = passwordSignInError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(
                                            checked = rememberMe,
                                            onCheckedChange = { rememberMe = it },
                                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(text = if (isArabic) "تذكرني" else "Remember me", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    TextButton(onClick = { currentStep = 4 }) {
                                        Text(text = if (isArabic) "نسيت كلمة المرور؟" else "Forgot password?", color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (passwordInput.isEmpty()) {
                                            passwordSignInError = if (isArabic) "كلمة المرور مطلوبة" else "Password required"
                                            return@Button
                                        }
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                val authResult = firebaseAuth.signInWithEmailAndPassword(emailInput, passwordInput).await()
                                                val user = authResult.user
                                                val displayName = user?.displayName ?: "User"
                                                
                                                val localUser = settingsViewModel.getUserByEmail(emailInput)
                                                if (localUser == null) {
                                                    settingsViewModel.registerUser(emailInput, displayName, passwordInput)
                                                }
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, displayName, rememberMe)
                                                
                                                Toast.makeText(context, if (isArabic) "مرحباً بك $displayName" else "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            } catch (e: Exception) {
                                                android.util.Log.e("LoginScreen", "Firebase Sign In Error", e)
                                                passwordSignInError = when (e) {
                                                    is FirebaseAuthInvalidCredentialsException -> if (isArabic) "البريد الإلكتروني أو كلمة المرور غير صحيحة" else "Invalid email or password"
                                                    is FirebaseAuthInvalidUserException -> if (isArabic) "الحساب غير موجود" else "User not found"
                                                    else -> e.localizedMessage ?: (if (isArabic) "فشل تسجيل الدخول" else "Sign in failed")
                                                }
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(28.dp)
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text(text = if (isArabic) "تسجيل الدخول" else "Sign In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
4 -> {
                            // Forgot Password Screen
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
                                        text = if (isArabic) "نسيت كلمة المرور" else "Forgot Password",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = if (isArabic) "أدخل بريدك الإلكتروني وسيتم إرسال رابط إعادة التعيين." else "Enter your email address and a reset link will be sent.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = forgotEmailInput,
                                    onValueChange = { forgotEmailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Rounded.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )

                                Button(
                                    onClick = {
                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(forgotEmailInput).matches()) {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    firebaseAuth.sendPasswordResetEmail(forgotEmailInput).await()
                                                    Toast.makeText(context, if (isArabic) "تم إرسال رابط إعادة التعيين" else "Reset link sent", Toast.LENGTH_SHORT).show()
                                                    currentStep = 1 // Go back to login
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, e.localizedMessage, Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isLoading = false
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, if (isArabic) "بريد إلكتروني غير صالح" else "Invalid email", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    } else {
                                        Text(text = if (isArabic) "إرسال الرابط" else "Send Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
