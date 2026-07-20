import os

files_to_check = [
    'app/src/main/java/com/example/auth/LoginScreen.kt',
    'app/src/main/java/com/example/chat/MainScreen.kt'
]

for filepath in files_to_check:
    if os.path.exists(filepath):
        with open(filepath, 'r') as f:
            text = f.read()
        
        # LoginScreen
        old_box_login = """                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.logo), 
                        contentDescription = null, 
                        tint = Color.Unspecified, 
                        modifier = Modifier.size(60.dp)
                    )
                }"""
        new_box_login = """                Icon(
                    painter = painterResource(id = R.drawable.logo), 
                    contentDescription = null, 
                    tint = Color.Unspecified, 
                    modifier = Modifier.size(90.dp)
                )"""
        
        if old_box_login in text:
            text = text.replace(old_box_login, new_box_login)
            print(f"Removed circle in {filepath} (LoginScreen pattern)")

        # MainScreen
        old_box_main = """        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo), 
                contentDescription = null, 
                tint = Color.Unspecified, 
                modifier = Modifier.size(64.dp)
            )
        }"""
        new_box_main = """        Icon(
            painter = painterResource(id = R.drawable.logo), 
            contentDescription = null, 
            tint = Color.Unspecified, 
            modifier = Modifier.size(100.dp)
        )"""

        if old_box_main in text:
            text = text.replace(old_box_main, new_box_main)
            print(f"Removed circle in {filepath} (MainScreen pattern)")

        with open(filepath, 'w') as f:
            f.write(text)

