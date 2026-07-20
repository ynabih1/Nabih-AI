import os

files_to_check = [
    'app/src/main/java/com/example/auth/LoginScreen.kt',
    'app/src/main/java/com/example/chat/MainScreen.kt'
]

for filepath in files_to_check:
    if os.path.exists(filepath):
        with open(filepath, 'r') as f:
            text = f.read()
        
        # Replace shadow in LoginScreen
        old_box_login = """                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(8.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {"""
        new_box_login = """                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {"""
        
        if old_box_login in text:
            text = text.replace(old_box_login, new_box_login)
            print(f"Removed shadow in {filepath} (LoginScreen pattern)")

        # Replace shadow in MainScreen (EmptyChatState)
        old_box_main = """        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {"""
        new_box_main = """        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {"""

        if old_box_main in text:
            text = text.replace(old_box_main, new_box_main)
            print(f"Removed shadow in {filepath} (MainScreen pattern)")

        with open(filepath, 'w') as f:
            f.write(text)

