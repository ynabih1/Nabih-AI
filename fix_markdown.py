with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'r') as f:
    text = f.read()

text = text.replace('"‎\${matchResult.value}‎"', '"‎${matchResult.value}‎"')

with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'w') as f:
    f.write(text)
