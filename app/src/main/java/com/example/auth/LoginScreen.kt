package com.example.auth

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.models.AppLanguage
import com.example.settings.profile.SettingsViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import kotlin.math.cos
import kotlin.math.sin

fun hashPassword(password: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
    return hashBytes.fold("") { str, it -> str + "%02x".format(it) }
}

/**
 * Elevated editorial art representation of the official Nabih AI app icon:
 * Interconnected neural constellation, luminous chat intelligence node, and glowing orbital rings.
 */
@Composable
fun NabihArtIllustration(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon_art_anim")
    
    // Breathing & pulse cycle
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    // Orbital rotation
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryBrandColor = Color(0xFF2563EB) // Nabih Royal Blue
    val secondaryBrandColor = Color(0xFF38BDF8) // Luminous Cyan / Sky Blue
    val deepBlueColor = Color(0xFF1D4ED8) // Deep Blue Accent
    val lightBlueColor = Color(0xFF60A5FA) // Light Blue Glow
    val glowColor = if (isDark) Color(0xFF1E40AF).copy(alpha = 0.25f) else Color(0xFF3B82F6).copy(alpha = 0.18f)
    val ringColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFF93C5FD).copy(alpha = 0.5f)

    Canvas(modifier = modifier.size(190.dp, 160.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val breath = (sin(pulse) + 1f) / 2f // 0..1
        val floatOffset = sin(pulse) * 3.5f

        // 1. Ambient Background Glow Halo (All Blue)
        drawCircle(
            color = glowColor,
            radius = (w * 0.42f) + (breath * 10f),
            center = Offset(cx, cy + floatOffset)
        )

        // 2. Elegant Dashed Orbital Guide Ring (Blue Accent)
        drawCircle(
            color = ringColor,
            radius = w * 0.40f,
            center = Offset(cx, cy + floatOffset),
            style = Stroke(
                width = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 16f), pulse * 8f)
            )
        )

        // 3. Floating Orbital Micro-Sparks (Pure Blue shades)
        val starRadius = w * 0.44f
        val starAngleRad = Math.toRadians(rotationAngle.toDouble())
        val s1x = cx + (starRadius * cos(starAngleRad)).toFloat()
        val s1y = cy + floatOffset + (starRadius * sin(starAngleRad)).toFloat()
        val s2x = cx - (starRadius * cos(starAngleRad)).toFloat()
        val s2y = cy + floatOffset - (starRadius * sin(starAngleRad)).toFloat()

        drawCircle(lightBlueColor, radius = 3.5f + (breath * 1.5f), center = Offset(s1x, s1y))
        drawCircle(secondaryBrandColor, radius = 3.0f, center = Offset(s2x, s2y))

        // Small stationary decorative star at top right (Blue)
        drawCircle(secondaryBrandColor, radius = 4f, center = Offset(w * 0.82f, h * 0.18f + floatOffset))

        // 4. Transform Matrix for the Canonical Nabih AI App Logo Icon
        // Based on 200x200 viewport of @drawable/logo
        val scale = (w * 0.58f) / 200f
        val originX = cx - (100f * scale)
        val originY = (cy + floatOffset) - (105f * scale)

        fun mapPoint(x: Float, y: Float): Offset {
            return Offset(originX + (x * scale), originY + (y * scale))
        }

        // Connection Paths matching logo.xml
        val conn1 = Path().apply {
            val p1 = mapPoint(65f, 65f)
            val p2 = mapPoint(140f, 60f)
            val c1 = mapPoint(100f, 65f)
            val c2 = mapPoint(140f, 30f)
            moveTo(p1.x, p1.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }

        val conn2 = Path().apply {
            val p1 = mapPoint(65f, 65f)
            val p2 = mapPoint(100f, 160f)
            val c1 = mapPoint(50f, 100f)
            val c2 = mapPoint(100f, 130f)
            moveTo(p1.x, p1.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }

        val conn3 = Path().apply {
            val p1 = mapPoint(100f, 160f)
            val p2 = mapPoint(150f, 130f)
            val c1 = mapPoint(130f, 160f)
            val c2 = mapPoint(140f, 120f)
            moveTo(p1.x, p1.y)
            cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
        }

        val strokeWidthVal = 24f * scale

        // Draw dynamic connectors (Blue)
        drawPath(
            path = conn1,
            color = primaryBrandColor,
            style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = conn2,
            color = primaryBrandColor,
            style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = conn3,
            color = primaryBrandColor,
            style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw the 4 Nodes (All Blue):
        // Top-Left Master Node (r = 40)
        val pTopLeft = mapPoint(65f, 65f)
        drawCircle(
            color = primaryBrandColor,
            radius = 40f * scale,
            center = pTopLeft
        )
        // Inner highlight core (Cyan Blue)
        drawCircle(
            color = secondaryBrandColor,
            radius = 16f * scale,
            center = pTopLeft
        )

        // Top-Right Node (r = 25)
        val pTopRight = mapPoint(140f, 60f)
        drawCircle(
            color = primaryBrandColor,
            radius = 25f * scale,
            center = pTopRight
        )
        drawCircle(
            color = lightBlueColor,
            radius = 10f * scale,
            center = pTopRight
        )

        // Bottom-Center Node (r = 25)
        val pBottomCenter = mapPoint(100f, 160f)
        drawCircle(
            color = primaryBrandColor,
            radius = 25f * scale,
            center = pBottomCenter
        )
        drawCircle(
            color = deepBlueColor,
            radius = 11f * scale,
            center = pBottomCenter
        )

        // Bottom-Right Chat Bubble Node (r = 40)
        val pChat = mapPoint(150f, 130f)
        drawCircle(
            color = primaryBrandColor,
            radius = 40f * scale,
            center = pChat
        )

        // Chat Bubble Tail matching logo.xml (M 130 160 L 130 190 L 155 168 Z)
        val tailPath = Path().apply {
            val t1 = mapPoint(130f, 160f)
            val t2 = mapPoint(130f, 190f)
            val t3 = mapPoint(155f, 168f)
            moveTo(t1.x, t1.y)
            lineTo(t2.x, t2.y)
            lineTo(t3.x, t3.y)
            close()
        }
        drawPath(path = tailPath, color = primaryBrandColor)

        // Inner chat core (Luminous Cyan Blue)
        drawCircle(
            color = secondaryBrandColor,
            radius = 16f * scale,
            center = pChat
        )
    }
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
    val isArabic = false // Interface locked to English
    val isDark = isSystemInDarkTheme()

    // Palette inspired by Claude warm minimalism
    val canvasBg = if (isDark) Color(0xFF141312) else Color(0xFFFAF7F2)
    val textPrimary = if (isDark) Color(0xFFF4EFE6) else Color(0xFF1D1B19)
    val textSecondary = if (isDark) Color(0xFFA29C91) else Color(0xFF756F64)
    val dividerColor = if (isDark) Color(0xFF38342F) else Color(0xFFDFD9CE)
    val inputBorderColor = if (isDark) Color(0xFF3F3B35) else Color(0xFFD6CFC3)
    val inputBgColor = if (isDark) Color(0xFF1F1E1B) else Color(0xFFFFFFFF)
    val primaryButtonBg = if (isDark) Color(0xFFEDE8DF) else Color(0xFF191816)
    val primaryButtonText = if (isDark) Color(0xFF191816) else Color(0xFFFAF7F2)

    // 1: Step 1 Email Form, 2: Sign Up, 3: Password Sign In, 4: Forgot Password
    var currentStep by remember { mutableStateOf(1) }

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var forgotEmailInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var resetEmailSent by remember { mutableStateOf(false) }

    val firebaseAuth = remember { FirebaseAuth.getInstance() }
    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "24318168756-i7hlejdrh54oddfhbhigbj8ar2jv3khd.apps.googleusercontent.com"
        } catch (e: Exception) {
            "24318168756-i7hlejdrh54oddfhbhigbj8ar2jv3khd.apps.googleusercontent.com"
        }
    }

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

                        val localUser = settingsViewModel.getUserByEmail(email)
                        if (localUser == null) {
                            settingsViewModel.registerUser(email, displayName, "google_auth")
                        }

                        settingsViewModel.updateLoginState(true, "GOOGLE", email, displayName, true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        Toast.makeText(context, if (isArabic) "مرحباً بك $displayName" else "Welcome, $displayName!", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    } catch (e: Exception) {
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
            val msg = when (e.statusCode) {
                com.google.android.gms.common.api.CommonStatusCodes.NETWORK_ERROR, 7 -> {
                    if (isArabic) "تعذر الاتصال بجوجل، تحقق من اتصالك بالإنترنت" else "Unable to connect to Google. Check internet connection."
                }
                com.google.android.gms.common.api.CommonStatusCodes.CANCELED, 12501 -> null
                else -> if (isArabic) "فشل تسجيل الدخول عبر جوجل" else "Google Sign-In failed"
            }
            if (msg != null) Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    var nameSignUpError by remember { mutableStateOf<String?>(null) }
    var emailSignInError by remember { mutableStateOf<String?>(null) }
    var passwordSignUpError by remember { mutableStateOf<String?>(null) }
    var passwordSignInError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }

    var isEmailSignInFocused by remember { mutableStateOf(false) }

    LaunchedEffect(currentStep) {
        nameSignUpError = null
        emailSignInError = null
        passwordSignUpError = null
        passwordSignInError = null
        forgotPasswordError = null
        resetEmailSent = false
    }

    val verifyEmailAndNavigate = { email: String ->
        val trimmedEmail = email.trim()
        if (!isLoading) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            scope.launch {
                isLoading = true
                emailSignInError = null
                try {
                    val localUser = settingsViewModel.getUserByEmail(trimmedEmail)
                    if (localUser != null) {
                        currentStep = 3
                        return@launch
                    }
                    val result = kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        firebaseAuth.fetchSignInMethodsForEmail(trimmedEmail).await()
                    }
                    val methods = result?.signInMethods
                    if (!methods.isNullOrEmpty()) {
                        currentStep = 3
                    } else {
                        currentStep = 2
                    }
                } catch (e: Exception) {
                    currentStep = 2
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
    ) {
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
                        .padding(horizontal = 28.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 440.dp)
                            .heightIn(min = screenHeight - 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // 1. Top Nabih AI Branding with official App Icon
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "Nabih AI App Icon",
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = textPrimary
                            )
                        }

                        // 2. Center Hero Art & Editorial Headline
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            // Powerful hand-drawn neural problem-solving artwork
                            NabihArtIllustration(
                                isDark = isDark,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            // Editorial Serif Headline matching Claude style
                            Text(
                                text = if (isArabic) "مساعدك الذكي لكل شيء" else "Your smart assistant for everything",
                                style = MaterialTheme.typography.headlineMedium,
                                fontSize = 26.sp,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Normal,
                                color = textPrimary,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )
                        }

                        // 3. Bottom Auth Flow (Matching Claude layout precisely)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            AnimatedContent(
                                targetState = currentStep,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                                    } else {
                                        (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                                    }
                                },
                                label = "claude_auth_step_transition"
                            ) { step ->
                                when (step) {
                                    1 -> {
                                        // STEP 1: Main Landing (Google Button + "OR" Divider + Outlined Email Input)
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Pill Google Login Button
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                        .requestIdToken(webClientId)
                                                        .requestEmail()
                                                        .build()
                                                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                                    googleSignInClient.signOut()
                                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .testTag("google_login_button"),
                                                shape = RoundedCornerShape(26.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = primaryButtonBg,
                                                    contentColor = primaryButtonText
                                                ),
                                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = if (isArabic) "المتابعة باستخدام Google" else "Continue with Google",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_google),
                                                        contentDescription = "Google Icon",
                                                        tint = Color.Unspecified,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Clean Minimalist OR Divider
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                            ) {
                                                HorizontalDivider(
                                                    modifier = Modifier.weight(1f),
                                                    color = dividerColor,
                                                    thickness = 1.dp
                                                )
                                                Text(
                                                    text = if (isArabic) "أو" else "OR",
                                                    color = textSecondary,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 14.dp)
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.weight(1f),
                                                    color = dividerColor,
                                                    thickness = 1.dp
                                                )
                                            }

                                            // Rounded Email Input Field
                                            OutlinedTextField(
                                                value = emailInput,
                                                onValueChange = {
                                                    emailInput = it
                                                    emailSignInError = null
                                                },
                                                placeholder = {
                                                    Text(
                                                        text = if (isArabic) "أدخل بريدك الإلكتروني" else "Enter your email",
                                                        color = textSecondary.copy(alpha = 0.8f),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontSize = 15.sp
                                                    )
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                                                keyboardActions = KeyboardActions(onDone = {
                                                    if (android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches()) {
                                                        verifyEmailAndNavigate(emailInput)
                                                    }
                                                }),
                                                singleLine = true,
                                                isError = emailSignInError != null,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor,
                                                    unfocusedContainerColor = inputBgColor,
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textPrimary,
                                                    unfocusedTextColor = textPrimary
                                                ),
                                                shape = RoundedCornerShape(26.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(52.dp)
                                                    .onFocusChanged { isEmailSignInFocused = it.isFocused }
                                            )

                                            if (emailSignInError != null) {
                                                Text(
                                                    text = emailSignInError!!,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }

                                            // Sleek Continue Button when Email is entered
                                            AnimatedVisibility(
                                                visible = android.util.Patterns.EMAIL_ADDRESS.matcher(emailInput.trim()).matches(),
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                Button(
                                                    onClick = { verifyEmailAndNavigate(emailInput) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(52.dp),
                                                    shape = RoundedCornerShape(26.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF2563EB),
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    if (isLoading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(22.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Text(
                                                            text = if (isArabic) "متابعة" else "Continue",
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    2 -> {
                                        // STEP 2: Create Account / Sign Up
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                IconButton(onClick = { currentStep = 1 }) {
                                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isArabic) "إنشاء حساب جديد" else "Create account",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textPrimary
                                                )
                                            }

                                            // Email (Readonly)
                                            OutlinedTextField(
                                                value = emailInput,
                                                onValueChange = {},
                                                readOnly = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor.copy(alpha = 0.5f),
                                                    unfocusedContainerColor = inputBgColor.copy(alpha = 0.5f),
                                                    focusedBorderColor = inputBorderColor,
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textSecondary,
                                                    unfocusedTextColor = textSecondary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            // Name
                                            OutlinedTextField(
                                                value = nameInput,
                                                onValueChange = { nameInput = it; nameSignUpError = null },
                                                placeholder = { Text(if (isArabic) "الاسم الكامل" else "Full name", color = textSecondary) },
                                                singleLine = true,
                                                isError = nameSignUpError != null,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor,
                                                    unfocusedContainerColor = inputBgColor,
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textPrimary,
                                                    unfocusedTextColor = textPrimary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            if (nameSignUpError != null) {
                                                Text(
                                                    text = nameSignUpError!!,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }

                                            // Password
                                            OutlinedTextField(
                                                value = passwordInput,
                                                onValueChange = { passwordInput = it; passwordSignUpError = null },
                                                placeholder = { Text(if (isArabic) "كلمة المرور (6 أحرف على الأقل)" else "Password (min 6 chars)", color = textSecondary) },
                                                trailingIcon = {
                                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                        Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = textSecondary)
                                                    }
                                                },
                                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                singleLine = true,
                                                isError = passwordSignUpError != null,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor,
                                                    unfocusedContainerColor = inputBgColor,
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textPrimary,
                                                    unfocusedTextColor = textPrimary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            if (passwordSignUpError != null) {
                                                Text(
                                                    text = passwordSignUpError!!,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (nameInput.trim().isEmpty()) {
                                                        nameSignUpError = if (isArabic) "يرجى إدخال الاسم الكامل" else "Please enter your name"
                                                        return@Button
                                                    }
                                                    if (passwordInput.length < 6) {
                                                        passwordSignUpError = if (isArabic) "كلمة المرور يجب أن لا تقل عن 6 أحرف" else "Password must be at least 6 characters"
                                                        return@Button
                                                    }
                                                    scope.launch {
                                                        isLoading = true
                                                        try {
                                                            val trimmedEmail = emailInput.trim()
                                                            val trimmedName = nameInput.trim()
                                                            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                            val user = authResult.user

                                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                                .setDisplayName(trimmedName)
                                                                .build()
                                                            user?.updateProfile(profileUpdates)

                                                            settingsViewModel.registerUser(trimmedEmail, trimmedName, hashPassword(passwordInput))
                                                            settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, trimmedName, rememberMe)

                                                            Toast.makeText(context, if (isArabic) "تم إنشاء الحساب بنجاح!" else "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                            onLoginSuccess()
                                                        } catch (e: Exception) {
                                                            passwordSignUpError = e.localizedMessage ?: "Sign up failed"
                                                        } finally {
                                                            isLoading = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(26.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2563EB),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                if (isLoading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                                } else {
                                                    Text(text = if (isArabic) "إنشاء الحساب" else "Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }

                                    3 -> {
                                        // STEP 3: Password Sign In
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                IconButton(onClick = { currentStep = 1 }) {
                                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isArabic) "مرحباً بعودتك" else "Welcome back",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textPrimary
                                                )
                                            }

                                            // Email (Readonly)
                                            OutlinedTextField(
                                                value = emailInput,
                                                onValueChange = {},
                                                readOnly = true,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor.copy(alpha = 0.5f),
                                                    unfocusedContainerColor = inputBgColor.copy(alpha = 0.5f),
                                                    focusedBorderColor = inputBorderColor,
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textSecondary,
                                                    unfocusedTextColor = textSecondary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            // Password
                                            OutlinedTextField(
                                                value = passwordInput,
                                                onValueChange = { passwordInput = it; passwordSignInError = null },
                                                placeholder = { Text(if (isArabic) "أدخل كلمة المرور" else "Enter password", color = textSecondary) },
                                                trailingIcon = {
                                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                                        Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = textSecondary)
                                                    }
                                                },
                                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                                singleLine = true,
                                                isError = passwordSignInError != null,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor,
                                                    unfocusedContainerColor = inputBgColor,
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textPrimary,
                                                    unfocusedTextColor = textPrimary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            if (passwordSignInError != null) {
                                                Text(
                                                    text = passwordSignInError!!,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(
                                                    text = if (isArabic) "نسيت كلمة المرور؟" else "Forgot password?",
                                                    color = Color(0xFF2563EB),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.clickable {
                                                        forgotEmailInput = emailInput
                                                        currentStep = 4
                                                    }
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (passwordInput.isEmpty()) {
                                                        passwordSignInError = if (isArabic) "يرجى إدخال كلمة المرور" else "Please enter password"
                                                        return@Button
                                                    }
                                                    scope.launch {
                                                        isLoading = true
                                                        try {
                                                            val trimmedEmail = emailInput.trim()
                                                            val localUser = settingsViewModel.getUserByEmail(trimmedEmail)
                                                            if (localUser != null && localUser.passwordHash == hashPassword(passwordInput)) {
                                                                settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, localUser.name, rememberMe)
                                                                Toast.makeText(context, if (isArabic) "تم تسجيل الدخول بنجاح" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                                onLoginSuccess()
                                                                return@launch
                                                            }

                                                            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, passwordInput).await()
                                                            val user = authResult.user
                                                            val displayName = user?.displayName ?: trimmedEmail.substringBefore("@")
                                                            settingsViewModel.updateLoginState(true, "EMAIL", trimmedEmail, displayName, rememberMe)
                                                            Toast.makeText(context, if (isArabic) "تم تسجيل الدخول بنجاح" else "Logged in successfully!", Toast.LENGTH_SHORT).show()
                                                            onLoginSuccess()
                                                        } catch (e: Exception) {
                                                            passwordSignInError = if (isArabic) "كلمة المرور غير صحيحة أو الحساب غير موجود" else "Invalid password or account does not exist"
                                                        } finally {
                                                            isLoading = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(26.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2563EB),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                if (isLoading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                                } else {
                                                    Text(text = if (isArabic) "تسجيل الدخول" else "Sign In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }

                                    4 -> {
                                        // STEP 4: Forgot Password
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                IconButton(onClick = { currentStep = 3 }) {
                                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = textPrimary)
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isArabic) "استعادة كلمة المرور" else "Reset Password",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontFamily = FontFamily.Serif,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textPrimary
                                                )
                                            }

                                            Text(
                                                text = if (isArabic) "أدخل بريدك الإلكتروني لإرسال رابط إعادة التعيين" else "Enter your email to receive a password reset link",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = textSecondary
                                            )

                                            OutlinedTextField(
                                                value = forgotEmailInput,
                                                onValueChange = { forgotEmailInput = it; forgotPasswordError = null },
                                                placeholder = { Text(if (isArabic) "البريد الإلكتروني" else "Email", color = textSecondary) },
                                                singleLine = true,
                                                isError = forgotPasswordError != null,
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = inputBgColor,
                                                    unfocusedContainerColor = inputBgColor,
                                                    focusedBorderColor = Color(0xFF2563EB),
                                                    unfocusedBorderColor = inputBorderColor,
                                                    focusedTextColor = textPrimary,
                                                    unfocusedTextColor = textPrimary
                                                ),
                                                shape = RoundedCornerShape(24.dp),
                                                modifier = Modifier.fillMaxWidth().height(52.dp)
                                            )

                                            if (forgotPasswordError != null) {
                                                Text(
                                                    text = forgotPasswordError!!,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(start = 12.dp)
                                                )
                                            }

                                            if (resetEmailSent) {
                                                Text(
                                                    text = if (isArabic) "تم إرسال رابط إعادة تعيين كلمة المرور إلى بريدك!" else "Reset link has been sent to your email!",
                                                    color = Color(0xFF16A34A),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            }

                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotEmailInput.trim()).matches()) {
                                                        forgotPasswordError = if (isArabic) "يرجى إدخال بريد إلكتروني صحيح" else "Please enter a valid email"
                                                        return@Button
                                                    }
                                                    scope.launch {
                                                        isLoading = true
                                                        try {
                                                            firebaseAuth.sendPasswordResetEmail(forgotEmailInput.trim()).await()
                                                            resetEmailSent = true
                                                        } catch (e: Exception) {
                                                            forgotPasswordError = e.localizedMessage ?: "Failed to send reset email"
                                                        } finally {
                                                            isLoading = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(26.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF2563EB),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                if (isLoading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                                } else {
                                                    Text(
                                                        text = if (isArabic) "إرسال الرابط" else "Send Link",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
