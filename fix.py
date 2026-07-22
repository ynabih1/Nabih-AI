import base64

with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'rb') as f:
    content = f.read()

target = b'            }\n        }\n    }\n}\n\n        attachedBase64Image: String? = null,'
replacement = b'            }\n        }\n    }\n}\n\n// --- 8. Unified Routing Engine ---\nobject AiRouter {\n    fun routeStreaming(\n        context: android.content.Context,\n        registryModel: com.example.model.ModelMetadata,\n        settings: com.example.model.AppSettings,\n        systemPrompt: String,\n        prompt: String,\n        history: List<com.example.data.repository.Message>,\n        attachedBase64Image: String? = null,'

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'wb') as f:
        f.write(content)
    print("Fixed!")
else:
    print("Not found! Let me try another pattern")
    target2 = b'            }\n        }\n    }\n}\n        attachedBase64Image: String? = null,'
    if target2 in content:
        content = content.replace(target2, replacement)
        with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'wb') as f:
            f.write(content)
        print("Fixed 2!")
    else:
        print("Still not found")
