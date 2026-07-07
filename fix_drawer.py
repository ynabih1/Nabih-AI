import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

old_label = 'label = { val txt = if (settings.isLoggedIn) (if (settings.userName.isEmpty()) (if (isArabic) "الحساب" else "Account") else settings.userName) else (if (isArabic) "تسجيل الدخول" else "Sign In"); Text(txt) }'
new_label = 'label = { val txt = if (settings.isLoggedIn) (if (settings.userName.isEmpty()) (if (settings.userEmail.isEmpty()) (if (isArabic) "الحساب" else "Account") else settings.userEmail) else settings.userName) else (if (isArabic) "تسجيل الدخول" else "Sign In"); Text(txt, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }'

content = content.replace(old_label, new_label)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
