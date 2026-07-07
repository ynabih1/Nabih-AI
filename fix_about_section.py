with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

import re

old_about = """@Composable
fun AboutSection(isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "حول Nabih AI" else "About Nabih AI", Icons.Outlined.Info) {
        val context = LocalContext.current
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text(if (isArabic) "إصدار التطبيق" else "App Version") },
                trailingContent = { Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "شروط الخدمة" else "Terms of Service") },
                modifier = Modifier.clickable { Toast.makeText(context, "Terms of Service", Toast.LENGTH_SHORT).show() },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "سياسة الخصوصية" else "Privacy Policy") },
                modifier = Modifier.clickable { Toast.makeText(context, "Privacy Policy", Toast.LENGTH_SHORT).show() },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
        }
    }
}"""

new_about = """@Composable
fun AboutSection(isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "حول Nabih AI" else "About Nabih AI", Icons.Outlined.Info) {
        val context = LocalContext.current
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            ListItem(
                headlineContent = { Text(if (isArabic) "إصدار التطبيق" else "App Version") },
                trailingContent = { Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "شروط الخدمة" else "Terms of Service") },
                modifier = Modifier.clickable { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://policies.google.com/terms"))
                    context.startActivity(intent)
                },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
            
            ListItem(
                headlineContent = { Text(if (isArabic) "سياسة الخصوصية" else "Privacy Policy") },
                modifier = Modifier.clickable { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://policies.google.com/privacy"))
                    context.startActivity(intent)
                },
                trailingContent = { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null) }
            )
        }
    }
}"""

code = code.replace(old_about, new_about)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)
