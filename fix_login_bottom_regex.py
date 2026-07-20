import re

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

pattern = r'            Spacer\(modifier = Modifier\.height\(48\.dp\)\)\s+\}\s+\}\s+// Full Screen Loading overlay'

new_bottom = """            // Legal Text
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

if re.search(pattern, text):
    text = re.sub(pattern, new_bottom, text)
    print("Replaced bottom area")
else:
    print("Bottom area not found")

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
