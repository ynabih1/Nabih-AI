with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = 'val cleanText = firstLine.replace(Regex("\\\\[REASONING MODE:.*?\\\\]"), "").trim()'
replacement = 'val cleanText = firstLine.replace(Regex("\\\\[REASONING MODE:.*?\\\\]", RegexOption.IGNORE_CASE), "").trim()'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed MainScreen!")
else:
    print("Not found in MainScreen!")
