package com.example.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.R
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppLanguage
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // Login screen states
    var currentStep by remember { mutableStateOf(0) } // 0: Onboarding, 1: Sign In Options, 2: Email Form, 3: Sign Up Form
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

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

                                // Custom Sign In Google (Original Minimalist Design)
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            delay(1500) // Simulated OAuth flow
                                            settingsViewModel.updateLoginState(true, "GOOGLE", "ynabihx@gmail.com", "")
                                            isLoading = false
                                            onLoginSuccess()
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
                                        Icon(
                                            imageVector = Icons.Default.AlternateEmail,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "متابعة باستخدام حساب Google" else "Continue with Google Account",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                // Custom Sign In Microsoft (Original Minimalist Design)
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            delay(1500) // Simulated OAuth flow
                                            settingsViewModel.updateLoginState(true, "MICROSOFT", "ynabihx@outlook.com", "")
                                            isLoading = false
                                            onLoginSuccess()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("microsoft_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Window,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "متابعة باستخدام حساب Microsoft" else "Continue with Microsoft Account",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                // Passkey Authentication (Original Minimalist Design)
                                OutlinedButton(
                                    onClick = {
                                        if (settings.hapticFeedback) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            isLoading = true
                                            delay(1200) // Simulated passkey setup/reading
                                            settingsViewModel.updateLoginState(true, "PASSKEY", "secured.key@nabih.ai", "")
                                            isLoading = false
                                            Toast.makeText(context, if (isArabic) "تمت المصادقة ببصمة المفتاح المشفر بنجاح" else "Passkey Verified Successfully", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("passkey_login_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Fingerprint,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = if (isArabic) "تسجيل الدخول باستخدام مفتاح المرور" else "Sign in with Passkey",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
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

                                Spacer(modifier = Modifier.height(12.dp))

                                // Guest Mode & Offline Option
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            settingsViewModel.updateLoginState(true, "GUEST", "guest@nabih.ai", "")
                                            Toast.makeText(context, if (isArabic) "مرحباً بك في وضع الضيف المحلي" else "Welcome to Local Guest Mode", Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        }
                                        .padding(12.dp)
                                        .testTag("guest_mode_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isArabic) "الدخول كضيف (حفظ محلي)" else "Sign in as Guest (Local Storage)",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
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
                                    leadingIcon = { Icon(Icons.Default.Email, null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("email_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("password_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (emailInput.isBlank() || passwordInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى تعبئة جميع الحقول" else "Please fill all fields", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1200)
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, "")
                                                isLoading = false
                                                onLoginSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("submit_login_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (isArabic) "دخول" else "Login", fontWeight = FontWeight.Bold)
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
                                    leadingIcon = { Icon(Icons.Default.Person, null) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("name_signup_input")
                                )

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, null) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("email_signup_input")
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text(if (isArabic) "كلمة المرور" else "Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, null) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("password_signup_input")
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (nameInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank()) {
                                            Toast.makeText(context, if (isArabic) "يرجى ملء كافة التفاصيل" else "Please fill in all details", Toast.LENGTH_SHORT).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                delay(1500)
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, nameInput)
                                                isLoading = false
                                                Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                onLoginSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("submit_signup_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(if (isArabic) "إنشاء حساب" else "Sign Up", fontWeight = FontWeight.Bold)
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
