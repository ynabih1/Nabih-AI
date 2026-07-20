filepath = "app/src/main/java/com/example/auth/LoginScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# Locate // Legal Text and // Full Screen Loading overlay
start_marker = "// Legal Text"
end_marker = "        // Full Screen Loading overlay"

start_idx = content.find(start_marker)
end_idx = content.find(end_marker)

if start_idx != -1 and end_idx != -1:
    print(f"Found markers! replacing from index {start_idx} to {end_idx}")
    
    bottom_replacement = """// Footer / Bottom Brand Details (moved closer to card)
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
        } // End of CompositionLocalProvider
    } // End of Box
"""
    new_content = content[:start_idx] + bottom_replacement + content[end_idx:]
    
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(new_content)
    print("Surgical bottom patching completed successfully!")
else:
    print(f"Error: Markers not found. start_idx: {start_idx}, end_idx: {end_idx}")
