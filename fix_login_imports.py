with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

imports_to_add = """import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalConfiguration
"""

if "import androidx.compose.ui.text.buildAnnotatedString" not in text:
    text = text.replace("import androidx.compose.ui.Alignment", imports_to_add + "import androidx.compose.ui.Alignment")
    with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
        f.write(text)
    print("Added imports")
