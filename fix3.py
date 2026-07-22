with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """        if (!success) {
            if (isNabihRequest) {
                // Nabih Ultra fallback when cloud request encounters error
                generateNabihUltraNativeStream(prompt, history, isArabic).collect { emit(it) }
            } else {
                val translatedError = lastException?.let { e -> 
                    AiErrorTranslator.translate(throwable = e, isArabic = isArabic)
                } ?: (if (isArabic) "فشلت عملية الاتصال بمزود الذكاء الاصطناعي." else "Failed to connect to AI provider.")
                throw Exception(translatedError)
            }
        }"""

replacement = """        if (!success) {
            val translatedError = lastException?.let { e -> 
                AiErrorTranslator.translate(throwable = e, isArabic = isArabic)
            } ?: (if (isArabic) "فشلت عملية الاتصال بمزود الذكاء الاصطناعي." else "Failed to connect to AI provider.")
            throw Exception(translatedError)
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed!")
else:
    print("Not found!")
