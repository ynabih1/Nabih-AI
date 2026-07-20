with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

# Fix spacedBy(16.dp) to 24.dp for forms
content = content.replace("verticalArrangement = Arrangement.spacedBy(16.dp),\n                                modifier = Modifier.fillMaxWidth()", "verticalArrangement = Arrangement.spacedBy(24.dp),\n                                modifier = Modifier.fillMaxWidth()")

# Remove the extra Spacer(modifier = Modifier.height(8.dp)) in Sign In
content = content.replace("                                Spacer(modifier = Modifier.height(8.dp))\n\n                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {\n                                    val passwordIndicatorColor", "                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {\n                                    val passwordIndicatorColor")

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
