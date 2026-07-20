with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

text = text.replace('import androidx.compose.ui.graphics.Color', 'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush')

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
