import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("import androidx.compose.material.icons.outlined.Lockimport androidx.compose.material.icons.outlined.Checkpackage com.example.ui.screen", "package com.example.ui.screen\nimport androidx.compose.material.icons.outlined.Lock\nimport androidx.compose.material.icons.outlined.Check")

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

