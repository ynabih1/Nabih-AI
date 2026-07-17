import re

path = 'app/src/main/java/com/example/feature/chat/MainScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Make New Chat icon Icons.Outlined.AddComment
content = content.replace('Icon(Icons.Outlined.Add,', 'Icon(Icons.Outlined.AddComment,')

# Make Conversations icon Icons.Outlined.ChatBubbleOutline and size 22.dp
content = content.replace('Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(20.dp))', 'Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(22.dp))')

with open(path, 'w') as f:
    f.write(content)
