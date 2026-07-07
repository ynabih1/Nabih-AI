with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    code = f.read()

code = code.replace("androidx.lifecycle.viewModelScope.launch", "androidx.lifecycle.viewModelScope\n                                        settingsViewModel.viewModelScope.launch")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(code)
