with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

code = code.replace(
    'import androidx.compose.material.icons.outlined.*',
    'import androidx.compose.material.icons.outlined.*\nimport androidx.compose.material.icons.automirrored.outlined.*'
)
code = code.replace(
    'import androidx.compose.material.icons.filled.*',
    'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.automirrored.filled.*'
)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)
