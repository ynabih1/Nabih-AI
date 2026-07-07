import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

code = re.sub(r'painter = painterResource\(id = R\.drawable\.logo\),contentDescription = null,\n\s*tint = MaterialTheme\.colorScheme\.primary,',
              r'painter = painterResource(id = R.drawable.logo), contentDescription = null,\n                    tint = androidx.compose.ui.graphics.Color.Unspecified,', code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

