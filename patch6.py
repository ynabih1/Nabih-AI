with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

target = """                                val isSignUpButtonEnabled = nameInput.isNotBlank() && emailInput.isNotBlank() && passwordInput.isNotBlank()

                                Button("""

replacement = """                                val isSignUpButtonEnabled = nameInput.isNotBlank() && emailInput.isNotBlank() && passwordInput.isNotBlank()

                                val signUpButtonContainerColor by animateColorAsState(
                                    targetValue = if (isSignUpButtonEnabled && !isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    animationSpec = tween(300),
                                    label = "signUpContainerColor"
                                )
                                val signUpButtonContentColor by animateColorAsState(
                                    targetValue = if (isSignUpButtonEnabled && !isLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                    animationSpec = tween(300),
                                    label = "signUpContentColor"
                                )
                                val signUpButtonElevation by androidx.compose.animation.core.animateDpAsState(
                                    targetValue = if (isSignUpButtonEnabled && !isLoading) 2.dp else 0.dp,
                                    label = "signUpElevation"
                                )

                                Button("""

content = content.replace(target, replacement)

target_button = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("submit_signup_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {"""

replacement_button = """                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .shadow(signUpButtonElevation, RoundedCornerShape(14.dp))
                                        .testTag("submit_signup_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = signUpButtonContainerColor,
                                        contentColor = signUpButtonContentColor,
                                        disabledContainerColor = signUpButtonContainerColor,
                                        disabledContentColor = signUpButtonContentColor
                                    )
                                ) {"""

content = content.replace(target_button, replacement_button)

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
