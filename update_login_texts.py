with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

import re

# Remove legal text
legal_block = """                                // Legal Text
                                Text(
                                    text = if (isArabic) "بالمتابعة، أنت توافق على شروط الاستخدام وسياسة الخصوصية" else "By continuing, you agree to the Terms of Use and Privacy Policy",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                )"""
if legal_block in text:
    text = text.replace(legal_block, "")
    print("Legal block removed")
else:
    print("Legal block NOT FOUND")

# Remove footer
footer_block = """            // Footer / Bottom Brand Details
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
            }"""
if footer_block in text:
    text = text.replace(footer_block, "")
    print("Footer block removed")
else:
    print("Footer block NOT FOUND")

with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
    f.write(text)
