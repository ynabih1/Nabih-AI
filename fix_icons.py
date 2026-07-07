import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("Icons.AutoMirrored.Filled.ArrowBack", "Icons.Default.ArrowBack")
code = code.replace("Icons.AutoMirrored.Outlined.KeyboardArrowRight", "Icons.Outlined.KeyboardArrowRight")
code = code.replace("PaddingValues(vertical = 16.dp, bottom = 48.dp)", "PaddingValues(top = 16.dp, bottom = 48.dp)")


with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

