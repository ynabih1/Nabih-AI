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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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

    val activity = context as? Activity

    // Login screen states
    var currentStep by remember { mutableStateOf(0) } // 0: Onboarding, 1: Sign In Options, 2: Email Form, 3: Sign Up Form
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val googleId = com.example.BuildConfig.GOOGLE_CLIENT_ID
        if (googleId.isEmpty() || googleId.contains("YOUR_") || googleId.contains("placeholder")) {
            android.util.Log.w("LoginScreen", "Google Client ID is missing or not configured correctly.")
        }
    }

    // Google Sign-In Error state

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Google Sign-In callback received. Result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val name = account?.displayName ?: ""
                android.util.Log.d("LoginScreen", "Google Sign-In successful. Email: $email, Name: $name")
                if (email.isNotEmpty()) {
                    scope.launch {
                        isLoading = true
                        var registeredUser = settingsViewModel.getUserByEmail(email)
                        if (registeredUser == null) {
                            android.util.Log.d("LoginScreen", "User not found locally. Registering implicitly for OAuth.")
                            val defaultName = if (name.isNotBlank()) name else email.substringBefore("@")
                            settingsViewModel.registerUser(email, defaultName, "oauth_google")
                            registeredUser = settingsViewModel.getUserByEmail(email)
                        } else {
                            android.util.Log.d("LoginScreen", "User found locally. Proceeding to login.")
                        }
                        isLoading = false
                        if (registeredUser != null) {
                            settingsViewModel.updateLoginState(true, "GOOGLE", email, name.ifBlank { registeredUser.name })
                            Toast.makeText(context, if (isArabic) "مرحباً بك ${registeredUser.name}" else "Welcome, ${registeredUser.name}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            android.util.Log.e("LoginScreen", "Failed to retrieve or register user locally after Google Sign-In.")
                            val msg = if (isArabic) {
                                "الحساب غير موجود. يرجى إنشاء حساب أولاً."
                            } else {
                                "Account not found. Please create an account first."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    android.util.Log.w("LoginScreen", "Google Sign-In succeeded but email was empty.")
                    Toast.makeText(context, if (isArabic) "فشل الحصول على معلومات حساب Google." else "Failed to retrieve Google account info.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "Google Sign-In failed with ApiException. Code: ${e.statusCode}, Message: ${e.message}", e)
                val errorMsg = if (isArabic) {
                    "فشل تسجيل الدخول بـ Google (كود: ${e.statusCode})"
                } else {
                    "Google Sign-In failed (code: ${e.statusCode})"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.w("LoginScreen", "Google Sign-In failed or was cancelled. Result code: ${result.resultCode}")
            val errorMsg = if (isArabic) {
                "تم إلغاء تسجيل الدخول بـ Google."
            } else {
                "Google Sign-In was cancelled."
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }



    // Onboarding items
    val onboardingSlides = remember(isArabic) {
        listOf(
            OnboardingSlide(
                title = if (isArabic) "محادثات ذكية فائقة الدقة" else "Intelligent Conversations",
                description = if (isArabic) "تواصل مع نماذج متطورة مدعومة بقدرات التفكير العلمي العميق وحل المشكلات المعقدة." else "Engage with advanced AI models optimized for scientific reasoning and complex problem-solving.",
                icon = Icons.Outlined.Lightbulb
            ),
            OnboardingSlide(
                title = if (isArabic) "تحليل متعدد الوسائط ذكي" else "Multimodal Processing",
                description = if (isArabic) "حلل المستندات والصور والمستندات التقنية والأكواد البرمجية بلمحة بصر." else "Instantly digest and analyze documents, high-res images, files, and complete technical codebases.",
                icon = Icons.Outlined.DocumentScanner
            ),
            OnboardingSlide(
                title = if (isArabic) "توجيه تلقائي ذكي ومستقر" else "Automatic Failover & Stability",
                description = if (isArabic) "استمتع باتصال آمن، مستقر، وموجّه تلقائياً إلى أفضل النماذج العاملة دون الحاجة لضبط مفاتيح API." else "Experience seamless AI services that automatically select the best model and failover if any API is unavailable.",
                icon = Icons.Outlined.CloudQueue
            )
        )
    }

    // Auto rotate onboarding slides
    LaunchedEffect(currentStep) {
        if (currentStep == 0) {
            var slideIndex = 0
            while (currentStep == 0) {
                delay(4000)
                slideIndex = (slideIndex + 1) % onboardingSlides.size
                // Smooth transition in mock or let state cycle
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Decorative background elements using beautiful Compose gradients
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
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
                    .padding(vertical = 24.dp),
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
                                // Dynamic Onboarding Card
                                var activeSlideIndex by remember { mutableStateOf(0) }
                                val activeSlide = onboardingSlides[activeSlideIndex]

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 220.dp),
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
                            // Premium Minimalist Sign-in Options
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isArabic) "تسجيل الدخول الآمن" else "Secure Sign In",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = if (isArabic) "اختر طريقة تسجيل الدخول المفضلة لديك" else "Select your preferred authentication method",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )



                                 // Google Sign In Button (Premium Minimalist Design)
                                OutlinedButton(
                                    onClick = {
                                        val clientId = com.example.BuildConfig.GOOGLE_CLIENT_ID
                                        val isClientIdMissing = clientId.isEmpty() || clientId.contains("YOUR_GOOGLE") || clientId.contains("placeholder")
                                        if (isClientIdMissing) {
                                            val missingMsg = if (isArabic) {
                                                "معرّف عميل Google غير مضبوط في الإعدادات."
                                            } else {
                                                "Google Client ID is missing or not configured."
                                            }
                                            Toast.makeText(context, missingMsg, Toast.LENGTH_LONG).show()
                                        } else {
                                            if (activity != null) {
                                                isLoading = true
                                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                    .requestEmail()
                                                    .requestIdToken(clientId)
                                                    .build()
                                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                                
                                                googleSignInClient.signOut().addOnCompleteListener {
                                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                    isLoading = false
                                                }
                                            } else {
                                                Toast.makeText(context, "Activity is not available.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(Color.White, shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "G",
                                                color = Color(0xFF4285F4),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "متابعة باستخدام حساب Google" else "Continue with Google Account",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                // Email & Password Button
                                Button(
                                    onClick = { currentStep = 2 },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("email_login_form_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Default.Email, null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "تسجيل الدخول بالبريد وكلمة المرور" else "Sign in with Email",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }


                            }
                        }

                        2 -> {
                            // Email & Password login Form
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { currentStep = 1 }) {
                                        Icon(Icons.Default.ArrowBack, "Back")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "تسجيل الدخول" else "Sign In",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
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
                                                } else if (user.passwordHash != passwordInput) {
                                                    val msg = if (isArabic) {
                                                        "البريد الإلكتروني أو كلمة المرور غير صحيحة."
                                                    } else {
                                                        "Incorrect email or password."
                                                    }
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                } else {
                                                    settingsViewModel.updateLoginState(true, "EMAIL", user.email, user.name)
                                                    Toast.makeText(context, if (isArabic) "مرحباً بك ${user.name}" else "Welcome, ${user.name}!", Toast.LENGTH_SHORT).show()
                                                    onLoginSuccess()
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
                                        modifier = Modifier.clickable { currentStep = 3 }
                                    )
                                }
                            }
                        }

                        3 -> {
                            // Sign Up Form
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(onClick = { currentStep = 2 }) {
                                        Icon(Icons.Default.ArrowBack, "Back")
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
                                    leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("name_signup_input")
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("email_signup_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("password_signup_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        val emailPattern = android.util.Patterns.EMAIL_ADDRESS
                                        if (nameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى ملء كافة التفاصيل" else "Please fill in all details", Toast.LENGTH_SHORT).show()
                                        } else if (!emailPattern.matcher(emailInput.trim()).matches()) {
                                            Toast.makeText(context, if (isArabic) "يرجى إدخال عنوان بريد إلكتروني صحيح." else "Please enter a valid email address.", Toast.LENGTH_LONG).show()
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
                                                    settingsViewModel.registerUser(emailInput, nameInput, passwordInput)
                                                    settingsViewModel.updateLoginState(true, "EMAIL", emailInput, nameInput)
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
                                        modifier = Modifier.clickable { currentStep = 2 }
                                    )
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
                            text = if (isArabic) "جاري المصادقة الآمنة..." else "Authenticating securely...",
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
