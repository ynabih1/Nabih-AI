with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

# Add focus state variables at the top (lines 200+)
content = content.replace("var isPasswordSignInFocused by remember { mutableStateOf(false) }", "var isPasswordSignInFocused by remember { mutableStateOf(false) }\n    var isNameSignUpFocused by remember { mutableStateOf(false) }\n    var isEmailSignUpFocused by remember { mutableStateOf(false) }\n    var isPasswordSignUpFocused2 by remember { mutableStateOf(false) }")

# Update Name Field
name_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    )"""
new_name_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val nameIndicatorColor by animateColorAsState(
                                        targetValue = if (isNameSignUpFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        label = "nameIndicator"
                                    )
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
                                            focusedIndicatorColor = nameIndicatorColor,
                                            unfocusedIndicatorColor = nameIndicatorColor,
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .onFocusChanged { isNameSignUpFocused = it.isFocused }
                                            .testTag("name_signup_input")
                                    )"""
content = content.replace(name_block, new_name_block)


# Update Email Field
email_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    )"""
new_email_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val emailIndicatorColor by animateColorAsState(
                                        targetValue = if (isEmailSignUpFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        label = "emailIndicator"
                                    )
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
                                            focusedIndicatorColor = emailIndicatorColor,
                                            unfocusedIndicatorColor = emailIndicatorColor,
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .onFocusChanged { isEmailSignUpFocused = it.isFocused }
                                            .testTag("email_signup_input")
                                    )"""
content = content.replace(email_block, new_email_block)

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
