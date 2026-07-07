with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    code = f.read()

code = code.replace("androidx.lifecycle.viewModelScope\n                                        settingsViewModel.viewModelScope.launch", "settingsViewModel.viewModelScope.launch")

code = code.replace("import androidx.lifecycle.lifecycleScope\nimport kotlinx.coroutines.launch", "import androidx.lifecycle.viewModelScope\nimport kotlinx.coroutines.launch")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(code)
