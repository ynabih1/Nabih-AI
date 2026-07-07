import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("androidx.compose.foundation.layout.Box", "Box")
code = code.replace("androidx.compose.ui.draw.clip", "Modifier.clip")
code = code.replace("androidx.compose.foundation.shape.CircleShape", "androidx.compose.foundation.shape.CircleShape")
code = code.replace("androidx.compose.ui.res.painterResource", "painterResource")
code = code.replace("androidx.compose.ui.graphics.Color.Unspecified", "androidx.compose.ui.graphics.Color.Unspecified")

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

