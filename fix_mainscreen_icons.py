import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# Replace AutoAwesome in header
code = re.sub(r'Icon\(\n\s*imageVector = Icons\.Default\.AutoAwesome,\n\s*contentDescription = null,\n\s*tint = MaterialTheme\.colorScheme\.primary,\n\s*modifier = Modifier\.size\(28\.dp\)\n\s*\)', r'Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(28.dp))', code)

# Replace AutoAwesome in empty state
code = re.sub(r'Icon\(\n\s*imageVector = Icons\.Default\.AutoAwesome,\n\s*contentDescription = null,\n\s*tint = MaterialTheme\.colorScheme\.primary,\n\s*modifier = Modifier\.size\(48\.dp\)\n\s*\)', r'Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(64.dp))', code)

# Replace in MessageItem
code = re.sub(r'Icon\(Icons\.Default\.AutoAwesome, null, tint = MaterialTheme\.colorScheme\.primary, modifier = Modifier\.size\(18\.dp\)\)', r'Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(18.dp))', code)

# Also in the new chat greeting/empty area if there's another
code = re.sub(r'Icons\.Default\.AutoAwesome,\n\s*contentDescription = null', r'painterResource(id = R.drawable.logo),\ncontentDescription = null', code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

