with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = 'var activePrompt = prompt.replace(Regex("\\\\[REASONING MODE:.*?\\\\]", RegexOption.IGNORE_CASE), "").trim()'
replacement = 'var activePrompt = prompt.replace(Regex("\\\\[REASONING MODE:.*?\\\\]", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "").trim()'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed ChatRepo!")
else:
    print("Not found in ChatRepo!")
