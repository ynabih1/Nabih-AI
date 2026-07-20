import re

filepath = "app/src/main/java/com/example/auth/LoginScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add LayoutDirection imports
imports_pattern = "import androidx.compose.ui.platform.LocalConfiguration"
if imports_pattern in content:
    new_imports = "import androidx.compose.ui.platform.LocalConfiguration\nimport androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.unit.LayoutDirection"
    content = content.replace(imports_pattern, new_imports)

# 2. Add CompositionLocalProvider to the root Column
box_start_pattern = """    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)                ) {
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
        Column("""

# Let's check if we can find a slightly broader pattern for the root Column
# We will insert CompositionLocalProvider right before the root Column
target_col = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {"""

replacement_col = """        CompositionLocalProvider(LocalLayoutDirection provides (if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {"""

if target_col in content:
    content = content.replace(target_col, replacement_col)
else:
    # Try a looser match with any spacing or minor difference
    print("Warning: target_col not matched directly, attempting loose regex match")
    col_re = re.compile(r"Column\(\s*modifier\s*=\s*Modifier\s*\.fillMaxSize\(\)\s*\.statusBarsPadding\(\)\s*\.navigationBarsPadding\(\)\s*\.imePadding\(\)\s*\.padding\(24\.dp\)\s*\.verticalScroll\(rememberScrollState\(\)\),\s*horizontalAlignment\s*=\s*Alignment\.CenterHorizontally\s*\)\s*\{")
    content = col_re.sub("""CompositionLocalProvider(LocalLayoutDirection provides (if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {""", content)

# 3. Adjust spacing of Logo area and Card
# Remove padding top on Logo Column so that logo starts exactly after 12% space
logo_col_target = """            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 24.dp)
            ) {"""

logo_col_replacement = """            // Header: App Logo & Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
            ) {"""

if logo_col_target in content:
    content = content.replace(logo_col_target, logo_col_replacement)

# Adjust height between header and card from screenHeight * 0.08f to screenHeight * 0.05f to visually center the card
spacer_between_target = "Spacer(modifier = Modifier.height(screenHeight * 0.08f))"
spacer_between_replacement = "Spacer(modifier = Modifier.height(screenHeight * 0.05f))"
if spacer_between_target in content:
    content = content.replace(spacer_between_target, spacer_between_replacement)

# 4. Modify Google login button style (Dark color, elevation, padding/alignment)
google_button_target = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onBackground,
                                        contentColor = MaterialTheme.colorScheme.background
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)"""

google_button_replacement = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("google_login_button"),
                                    shape = RoundedCornerShape(28.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF171717),
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(
                                        defaultElevation = 4.dp,
                                        pressedElevation = 6.dp,
                                        focusedElevation = 4.dp,
                                        hoveredElevation = 4.dp
                                    )"""

if google_button_target in content:
    content = content.replace(google_button_target, google_button_replacement)

# 5. Modify Divider line visibility
divider_target = """                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Text(
                                        text = if (isArabic) "أو" else "OR",
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                }"""

divider_replacement = """                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                    Text(
                                        text = if (isArabic) "أو" else "OR",
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                                }"""

if divider_target in content:
    content = content.replace(divider_target, divider_replacement)

# 6. Update Email Input with border and dynamic leading icon
email_input_target = """                                // Email Input
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
                                        focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                )"""

email_input_replacement = """                                // Email Input
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
                                        focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        disabledContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        errorContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color.White,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                        errorBorderColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .onFocusChanged { isEmailSignInFocused = it.isFocused }
                                )"""

if email_input_target in content:
    content = content.replace(email_input_target, email_input_replacement)

# 7. Move Legal Text inside Step 1 Card Column
# Let's find the closing brace of the continue button animated visibility in step 1 Column
continue_button_pattern = """                                // Continue Button
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
                                }"""

continue_button_pattern_with_legal = continue_button_pattern + """
                                // Legal Text (Terms of Use and Privacy Policy links styled)
                                Spacer(modifier = Modifier.height(16.dp))
                                val annotatedString = buildAnnotatedString {
                                    val baseText = if (isArabic) "بالمتابعة، أنت توافق على شروط الاستخدام وسياسة الخصوصية" else "By continuing, you agree to the Terms of Use and Privacy Policy"
                                    val termsText = if (isArabic) "شروط الاستخدام" else "Terms of Use"
                                    val privacyText = if (isArabic) "سياسة الخصوصية" else "Privacy Policy"
                                    
                                    append(baseText)
                                    
                                    val termsStart = baseText.indexOf(termsText)
                                    if (termsStart != -1) {
                                        addStyle(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            start = termsStart,
                                            end = termsStart + termsText.length
                                        )
                                    }
                                    
                                    val privacyStart = baseText.indexOf(privacyText)
                                    if (privacyStart != -1) {
                                        addStyle(
                                            style = SpanStyle(
                                                color = MaterialTheme.colorScheme.primary,
                                                textDecoration = TextDecoration.Underline,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            start = privacyStart,
                                            end = privacyStart + privacyText.length
                                        )
                                    }
                                }
                                Text(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                )"""

if continue_button_pattern in content:
    content = content.replace(continue_button_pattern, continue_button_pattern_with_legal)

# 8. Remove the old Legal Text block from the bottom of the column, and close CompositionLocalProvider
# Let's inspect the exact lines at the bottom of the Column
bottom_section_target = """            // Legal Text
            Spacer(modifier = Modifier.height(24.dp))
            val annotatedString = buildAnnotatedString {
                val baseText = if (isArabic) "بالمتابعة، أنت توافق على شروط الاستخدام وسياسة الخصوصية" else "By continuing, you agree to the Terms of Use and Privacy Policy"
                val termsText = if (isArabic) "شروط الاستخدام" else "Terms of Use"
                val privacyText = if (isArabic) "سياسة الخصوصية" else "Privacy Policy"
                                append(baseText)
                                val termsStart = baseText.indexOf(termsText)
                if (termsStart != -1) {
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = termsStart,
                        end = termsStart + termsText.length
                    )
                }
                                val privacyStart = baseText.indexOf(privacyText)
                if (privacyStart != -1) {
                    addStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = privacyStart,
                        end = privacyStart + privacyText.length
                    )
                }
            }
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            // Footer / Bottom Brand Details
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nabih Secure Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "تشفير محلي آمن بالكامل 256-بت" else "Full 256-bit Local Secure Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
                        Spacer(modifier = Modifier.height(screenHeight * 0.1f))                        }"""

bottom_section_replacement = """            // Footer / Bottom Brand Details (moved closer to card)
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Nabih Secure Authentication",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isArabic) "تشفير محلي آمن بالكامل 256-بت" else "Full 256-bit Local Secure Encryption",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    fontSize = 10.sp
                )
            }
            
            // Bottom balanced spacer to mirror top perfectly!
            Spacer(modifier = Modifier.height(screenHeight * 0.12f))
            
            } // End of Column
        } // End of CompositionLocalProvider"""

# To make replacement more robust, let's normalize whitespaces when matching or use exact replace if spacing matches
if bottom_section_target in content:
    content = content.replace(bottom_section_target, bottom_section_replacement)
else:
    # Try alternate target version without raw formatting quirks
    print("Warning: bottom_section_target not matched directly, trying simpler replace")
    
    # We will locate "val annotatedString = buildAnnotatedString" from the bottom and perform surgical replacement
    # Let's find from where legal text starts to the end of the column
    start_idx = content.find("// Legal Text\n            Spacer(modifier = Modifier.height(24.dp))")
    if start_idx != -1:
        # find the end of column (right before loading overlay code)
        end_idx = content.find("// Full Screen Loading overlay")
        if end_idx != -1:
            # We replace everything from start_idx to end_idx with our beautiful footer section + CompositionLocalProvider close
            # We just need to make sure the closing bracket of Box / other is preserved.
            # Let's see what is right before the overlay:
            # Column finishes with a curly brace, and we close CompositionLocalProvider, and then we close Box?
            # Wait, let's print the actual section around the column end.
            pass

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("Patching script execution completed.")
