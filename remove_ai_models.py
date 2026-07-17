import re

path = 'app/src/main/java/com/example/feature/chat/MainScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# Pattern to remove AI model section from item { HorizontalDivider ... } down to the end of models.forEach { ... }
# Notice it ends right before `            }` of LazyColumn
pattern = r"""                item \{\s*HorizontalDivider\(color = MaterialTheme\.colorScheme\.outlineVariant\.copy\(alpha = 0\.5f\), modifier = Modifier\.padding\(vertical = 12\.dp\)\)\s*Text\(\s*text = if \(isArabic\) "نموذج الذكاء الاصطناعي" else "AI Model",.*?models\.forEach \{ \(model, isUnlocked, displayName\) ->.*?\}\s*\}\s*\}"""

if re.search(pattern, content, re.DOTALL):
    content = re.sub(pattern, "", content, flags=re.DOTALL)
    print("Removed AI models list")
else:
    print("AI models list pattern not found")

with open(path, 'w') as f:
    f.write(content)
