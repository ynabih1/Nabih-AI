import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

code = code.replace(".Modifier.clip(androidx.compose.foundation.shape.CircleShape)", ".clip(androidx.compose.foundation.shape.CircleShape)")
code = code.replace("package com.example.ui.screen", "package com.example.ui.screen\n\nimport androidx.compose.ui.draw.clip\nimport androidx.compose.ui.res.painterResource\nimport com.example.R")

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

