import re

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

# We want to replace from `4 -> {` up to `} // end of step 4`
# Let's use regex
pattern = re.compile(r'4 -> \{.*?(?=\n                    }\n                }\n            \})', re.DOTALL)

new_step_4 = """4 -> {
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
                        }"""

content = pattern.sub(new_step_4, content)

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
print("Step 4 replaced.")
