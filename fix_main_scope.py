with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    code = f.read()

code = code.replace("androidx.lifecycle.lifecycleScope.launch", "androidx.lifecycle.viewModelScope.launch")
# Actually, settingsViewModel.viewModelScope is better but viewModelScope is protected or internal? 
# Wait, viewModelScope is a public extension property on ViewModel! 
code = code.replace("androidx.lifecycle.lifecycleScope.launch", "settingsViewModel.viewModelScope.launch")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(code)
