with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith("import androidx.compose.material.icons.outlined.Lock"):
        new_lines.append("package com.example.ui.screen\n")
        new_lines.append("import androidx.compose.material.icons.outlined.Lock\n")
        new_lines.append("import androidx.compose.material.icons.outlined.Check\n")
    elif line.startswith("import androidx.compose.material.icons.outlined.Check"):
        pass # already added
    elif line.startswith("package com.example.ui.screen"):
        pass # already added
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.writelines(new_lines)

