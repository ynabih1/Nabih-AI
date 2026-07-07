import re

with open('app/src/main/java/com/example/ui/screen/AccountScreen.kt', 'r') as f:
    content = f.read()

edit_profile_btn = """                // Actions
                OutlinedButton(
                    onClick = { 
                        Toast.makeText(context, if (isArabic) "تعديل الملف الشخصي قريباً" else "Edit Profile coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isArabic) "تعديل الملف الشخصي" else "Edit Profile")
                }
                
                OutlinedButton("""

content = content.replace('                // Actions\n                OutlinedButton(', edit_profile_btn)

with open('app/src/main/java/com/example/ui/screen/AccountScreen.kt', 'w') as f:
    f.write(content)
