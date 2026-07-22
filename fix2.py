with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = """        if (!isKeyValid) {
            if (isNabihRequest) {
                // Nabih Ultra operates keylessly with built-in native engine!
                generateNabihUltraNativeStream(prompt, history, isArabic).collect { emit(it) }
                return@flow
            } else {
                val missingKeyMsg = if (isArabic) {
                    "مفتاح API غير متوفر لهذا النموذج. يرجى إضافة مفتاح API في الإعدادات أو استخدام \\"نبيه ألترا\\" المجاني."
                } else {
                    "API Key is missing for this model. Please add a valid API key in Settings or use free \\"Nabih Ultra\\"."
                }
                throw Exception(missingKeyMsg)
            }
        }"""

replacement = """        if (!isKeyValid) {
            val missingKeyMsg = if (isArabic) {
                "لا يوجد مفتاح API مُفعّل حالياً. يرجى إضافة مفتاح من شاشة الإعدادات لاستخدام النماذج."
            } else {
                "No API Key is currently active. Please add an API key in Settings to use the models."
            }
            throw Exception(missingKeyMsg)
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print("Fixed!")
else:
    print("Not found!")
