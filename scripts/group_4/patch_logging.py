import sys

filepath = "app/src/main/java/com/example/feature/chat/ChatRepository.kt"
content = open(filepath).read()

# Replace simple log with more detailed stack trace
target = 'android.util.Log.e("ChatRepository", "API Request failed on try $currentTry for model $currentModelId", e)'
replacement = 'android.util.Log.e("ChatRepository", "API Request failed on try $currentTry for model $currentModelId. Details: ${e.stackTraceToString()}", e)'

if target in content:
    content = content.replace(target, replacement)
    print("Patched successfully")
else:
    print("Target not found")

with open(filepath, "w") as f:
    f.write(content)

