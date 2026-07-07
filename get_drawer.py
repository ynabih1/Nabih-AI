import re
with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

match = re.search(r'(@Composable\s*fun MainDrawerContent.*?)^}', content, re.DOTALL | re.MULTILINE)
if match:
    print(match.group(1) + "}")
