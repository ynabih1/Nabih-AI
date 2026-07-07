import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

match = re.search(r'CenterAlignedTopAppBar\(.*?\)', code, re.DOTALL)
if match:
    print(match.group(0))
