import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

profile_header = """
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(100.dp)
                            .androidx.compose.ui.draw.clip(androidx.compose.foundation.shape.CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.logo),
                            contentDescription = "Profile Logo",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nabih AI Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = settings.userEmail.ifEmpty { "user@nabih.ai" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
"""

code = code.replace("        ) {\n            // Theme Selector Card", "        ) {\n" + profile_header + "\n            // Theme Selector Card")

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

