import re

with open('app/src/main/java/com/example/ui/screen/LoginScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('"Nabih User"', '""')
content = content.replace('"Nabih Microsoft User"', '""')
content = content.replace('"Secured Passkey Session"', '""')
content = content.replace('"Guest User"', '""')
content = content.replace('"Nabih Core User"', '""')

with open('app/src/main/java/com/example/ui/screen/LoginScreen.kt', 'w') as f:
    f.write(content)
