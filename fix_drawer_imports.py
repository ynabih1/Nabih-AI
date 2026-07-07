with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

# Add missing imports if they don't exist
imports = [
    "import com.example.data.model.AppSettings",
    "import com.example.data.model.AppLanguage"
]

for imp in imports:
    if imp not in content:
        content = content.replace("import com.example.data.model.AiModel", f"import com.example.data.model.AiModel\n{imp}")

# Check if AiModel is imported
if "import com.example.data.model.AiModel" not in content and imports[0] not in content:
    content = content.replace("import androidx.compose", f"{imports[0]}\n{imports[1]}\nimport androidx.compose", 1)


# Fix the username text issue
content = content.replace(
    'label = { Text(if (settings.isLoggedIn) (settings.userName.ifEmpty { if (isArabic) "الحساب" else "Account" }) else (if (isArabic) "تسجيل الدخول" else "Sign In")) }',
    'label = { val txt = if (settings.isLoggedIn) (if (settings.userName.isEmpty()) (if (isArabic) "الحساب" else "Account") else settings.userName) else (if (isArabic) "تسجيل الدخول" else "Sign In"); Text(txt) }'
)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
