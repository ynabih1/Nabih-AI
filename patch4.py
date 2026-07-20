with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

pass_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                    )"""
new_pass_block = """                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val passwordIndicatorColor by animateColorAsState(
                                        targetValue = if (isPasswordSignUpFocused2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                        label = "passwordIndicator"
                                    )
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
                                            focusedIndicatorColor = passwordIndicatorColor,
                                            unfocusedIndicatorColor = passwordIndicatorColor,
                                            errorIndicatorColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .onFocusChanged { isPasswordSignUpFocused2 = it.isFocused }
                                            .testTag("password_signup_input")
                                    )"""
content = content.replace(pass_block, new_pass_block)

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
