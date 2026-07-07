import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

old_code = """    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)"""

new_code = """    val selectedModel by chatViewModel.selectedModel.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)"""

code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

