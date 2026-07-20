import re

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

start_idx = content.find("when (step) {")
end_idx = content.find("4 -> {", start_idx)

if start_idx != -1 and end_idx != -1:
    new_blocks = """when (step) {
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
                                        containerColor = MaterialTheme.colorScheme.onBackground,
                                        contentColor = MaterialTheme.colorScheme.background
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
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_google),
                                            contentDescription = "Google Icon",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(24.dp)
                                        )
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
                                                
                                                settingsViewModel.registerUser(emailInput, nameInput.trim(), "email_password", passwordInput)
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, nameInput.trim(), rememberMe)
                                                
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
                                                    settingsViewModel.registerUser(emailInput, displayName, "email_password", passwordInput)
                                                }
                                                settingsViewModel.updateLoginState(true, "EMAIL", emailInput, displayName, rememberMe)
                                                
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
"""
    
    old_blocks = content[start_idx:end_idx]
    content = content.replace(old_blocks, new_blocks)
    
    with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
        f.write(content)
    print("Successfully replaced AnimatedContent blocks 0, 1, 2, 3")
else:
    print("Could not find start or end index for replacing blocks")
