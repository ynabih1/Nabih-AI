import re

with open('app/src/main/java/com/example/ui/screen/FeatureScreens.kt', 'r') as f:
    content = f.read()

content = re.sub(r'@Composable\s*fun AccountScreen\(onNavigateBack: \(\) -> Unit, isArabic: Boolean\) \{.*?\n\}\n', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screen/FeatureScreens.kt', 'w') as f:
    f.write(content)
