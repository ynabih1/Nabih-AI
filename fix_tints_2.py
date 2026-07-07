import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

code = re.sub(r'painterResource\(id = R\.drawable\.logo\),contentDescription = null,\n\s*modifier = Modifier\.size\(64\.dp\),\n\s*tint = MaterialTheme\.colorScheme\.primary\.copy\(alpha = 0\.5f\)',
              r'painter = painterResource(id = R.drawable.logo), contentDescription = null,\n            modifier = Modifier.size(64.dp),\n            tint = androidx.compose.ui.graphics.Color.Unspecified', code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

