with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    var isPasswordSignUpFocused2 by remember { mutableStateOf(false) }", "")
content = content.replace("isPasswordSignUpFocused2", "isPasswordSignUpFocused")
content = content.replace("    var isNameSignUpFocused by remember { mutableStateOf(false) }\n", "")
content = content.replace("    var isEmailSignUpFocused by remember { mutableStateOf(false) }\n", "")

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
