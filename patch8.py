with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

content = content.replace("    var isPasswordSignUpFocused by remember { mutableStateOf(false) }", "    var isPasswordSignUpFocused by remember { mutableStateOf(false) }\n    var isNameSignUpFocused by remember { mutableStateOf(false) }\n    var isEmailSignUpFocused by remember { mutableStateOf(false) }")

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
