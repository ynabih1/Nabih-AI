import re

with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'r') as f:
    text = f.read()
    
# Replace \\u200E with \u200E
text = text.replace('\\\\u200E', '\u200E')
text = text.replace('\\u200E', '\u200E')

with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'w') as f:
    f.write(text)
