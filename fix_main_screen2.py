import os

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

code = code.replace("import com.example.data.model.Conversation", "import com.example.data.database.Conversation")
code = code.replace("import com.example.ui.state.ChatUiState", "import com.example.ui.viewmodel.ChatUiState\nimport com.example.data.database.Message")
code = code.replace("ChatUiState.Empty", "ChatUiState.Idle")

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

