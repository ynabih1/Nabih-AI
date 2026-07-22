with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = "history: List<com.example.data.repository.Message>,"
replacement = "history: List<com.example.data.local.Message>,"

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed!")
else:
    print("Not found!")
