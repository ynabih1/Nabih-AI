import re

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

# Replace Column's verticalArrangement and first Spacer
old_col = """            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            // Header: App Logo & Name"""

new_col = """            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp
            
            // Push content down by ~12% of screen
            Spacer(modifier = Modifier.height(screenHeight * 0.12f))
            
            // Header: App Logo & Name"""

if old_col in text:
    text = text.replace(old_col, new_col)
    print("Replaced column start")
else:
    print("Column start not found")

# Replace Spacer before Card
old_spacer_before_card = """            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Interactive Content Area"""

new_spacer_before_card = """            }

            Spacer(modifier = Modifier.height(screenHeight * 0.08f))

            // Main Interactive Content Area"""

if old_spacer_before_card in text:
    text = text.replace(old_spacer_before_card, new_spacer_before_card)
    print("Replaced spacer before card")
else:
    print("Spacer before card not found")

# Replace Spacer after Card with Legal Text + Footer + Bottom Spacer
old_bottom = """            Spacer(modifier = Modifier.height(48.dp))
            }
                    }
        // Full Screen Loading overlay"""

new_bottom = """            }
            
            // Legal Text
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
            
            Spacer(modifier = Modifier.height(screenHeight * 0.1f))
            
            }
                    }
        // Full Screen Loading overlay"""

if old_bottom in text:
    text = text.replace(old_bottom, new_bottom)
    print("Replaced bottom area")
else:
    print("Bottom area not found")

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
