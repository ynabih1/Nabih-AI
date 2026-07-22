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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.focus.onFocusChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

enum class PasswordStrength(val labelEn: String, val labelAr: String, val color: Color, val progress: Float) {
    EMPTY("Empty", "فارغة", Color.Transparent, 0f),
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
    // 1: Sign In Form, 2: Sign Up Form, 3: Forgot Password, 4: Verification & Reset
    var currentStep by remember { mutableStateOf(1) }
    
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
    val webClientId = context.getString(com.example.R.string.default_web_client_id)

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

    val verifyEmailAndNavigate = { email: String ->
        val trimmedEmail = email.trim()
        scope.launch {
            isLoading = true
            emailSignInError = null
            try {
                val result = firebaseAuth.fetchSignInMethodsForEmail(trimmedEmail).await()
                val methods = result.signInMethods
                if (methods.isNullOrEmpty()) {
                    android.util.Log.d("EmailCheck", "RESULT: email NOT registered -> going to CREATE ACCOUNT screen")
                    currentStep = 2
                } else {
                    android.util.Log.d("EmailCheck", "RESULT: email ALREADY registered -> going to SIGN IN screen")
                    currentStep = 3
                }
            } catch (e: Exception) {
                android.util.Log.e("EmailCheck", "Unexpected: ${e.javaClass.simpleName} - ${e.message}")
                emailSignInError = if (isArabic) "تعذر التحقق من البريد الإلكتروني، حاول مرة أخرى" else "Failed to verify email, try again"
            } finally {
                isLoading = false
            }
        }
    }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )

        CompositionLocalProvider(LocalLayoutDirection provides (if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                val screenHeight = maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = screenHeight),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Header: App Logo & Name (Visual padding adjusted)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = if (isArabic) com.example.ui.theme.ArabicFamily else com.example.ui.theme.BodySansFamily
                                ),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = if (isArabic) "مساعدك الذكي لكل شيء" else "Your smart assistant for everything",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = if (isArabic) com.example.ui.theme.ArabicFamily else com.example.ui.theme.BodySansFamily
                                ),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Fixed space between header and interactive group
                        Spacer(modifier = Modifier.height(32.dp))

                        // Main Interactive Content Area (Card component)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 480.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    Color.Transparent
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                                BorderStroke(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                )
                            } else {
                                null
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
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
                                        1 -> {
                                            // Main Email Input Screen
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                // Google Login Button
                                                Button(
                                                    onClick = {
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
                                                        containerColor = Color.White,
                                                        contentColor = Color.Black
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                    elevation = ButtonDefaults.buttonElevation(
                                                        defaultElevation = 1.dp,
                                                        pressedElevation = 2.dp
                                                    )
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_google),
                                                            contentDescription = "Google Icon",
                                                            tint = Color.Unspecified,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = if (isArabic) "المتابعة باستخدام Google" else "Continue with Google",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                // Divider (high visibility outline, clearer but soft)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically, 
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                ) {
                                                    HorizontalDivider(
                                                        modifier = Modifier.weight(1f), 
                                                        color = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else Color(0xFFD1CFC7)
                                                    )
                                                    Text(
                                                        text = if (isArabic) "أو" else "OR",
                                                        modifier = Modifier.padding(horizontal = 16.dp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                    HorizontalDivider(
                                                        modifier = Modifier.weight(1f), 
                                                        color = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.outline else Color(0xFFD1CFC7)
                                                    )
                                                }

                                                // Email Input (White background, clear BorderSubtle, Primary focus transition)
                                                OutlinedTextField(
                                                    value = emailInput,
                                                    onValueChange = { 
                                                        emailInput = it 
                                                        emailSignInError = null 
                                                    },
                                                    placeholder = { Text(if (isArabic) "أدخل بريدك الإلكتروني" else "Enter your email") },
                                                    leadingIcon = {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Email,
                                                            contentDescription = null,
                                                            tint = if (isEmailSignInFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                        )
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(onDone = { 
                                                        if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                                                            verifyEmailAndNavigate(emailInput)
                                                        }
                                                    }),
                                                    singleLine = true,
                                                    isError = emailSignInError != null,
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        disabledContainerColor = MaterialTheme.colorScheme.surface,
                                                        errorContainerColor = MaterialTheme.colorScheme.surface,
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                                                        errorBorderColor = MaterialTheme.colorScheme.error
                                                    ),
                                                    shape = RoundedCornerShape(28.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(56.dp)
                                                        .onFocusChanged { isEmailSignInFocused = it.isFocused }
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
                                                            verifyEmailAndNavigate(emailInput)
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
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                                                focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
                                                focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
                                                        val trimmedEmail = emailInput.trim()
                                                        val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                        val user = authResult.user
                                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                            .setDisplayName(nameInput.trim())
                                                            .build()
                                                        user?.updateProfile(profileUpdates)?.await()
                                                        
                                                        settingsViewModel.registerUser(trimmedEmail, nameInput.trim(), hashPassword(passwordInput))
                                                        settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, nameInput.trim(), rememberMe)
                                                        
                                                        Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("LoginScreen", "Firebase Sign Up Error", e)
                                                        passwordSignUpError = e.localizedMessage ?: "Sign up failed"
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
                                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
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
                                                focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
                                                        val trimmedEmail = emailInput.trim()
                                                        val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                        val user = authResult.user
                                                        val displayName = user?.displayName ?: "User"
                                                        
                                                        val localUser = settingsViewModel.getUserByEmail(trimmedEmail)
                                                        if (localUser == null) {
                                                            settingsViewModel.registerUser(trimmedEmail, displayName, hashPassword(passwordInput))
                                                        }
                                                        settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, displayName, rememberMe)
                                                        
                                                        Toast.makeText(context, if (isArabic) "مرحباً بك $displayName" else "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                                                        onLoginSuccess()
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("LoginScreen", "Firebase Sign In Error", e)
                                                        passwordSignInError = e.localizedMessage ?: "Sign in failed"
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
                                                focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
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
                }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
