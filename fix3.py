with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'r') as f:
    text = f.read()

text = text.replace('val englishRegex = Regex("([a-zA-Z0-9]+(?:[\\\\s_\\\\-.,:]+[a-zA-Z0-9]+)*)")', 'val englishRegex = Regex("""([a-zA-Z0-9]+(?:[\\s_\\-.,:]+[a-zA-Z0-9]+)*)""")')

with open('app/src/main/java/com/example/ui/components/MarkdownRenderer.kt', 'w') as f:
    f.write(text)
