with open("app/src/main/java/com/example/core/theme/Color.kt", "r") as f:
    content = f.read()

new_colors = """// Ultra-Premium Minimalist Theme
val AccentPrimary = Color(0xFF000000)     // Pure Black for buttons in light mode
val BackgroundScreen = Color(0xFFFFFFFF)  // Pure White
val BackgroundChatBox = Color(0xFFF7F7F8) // Light gray for inputs
val BorderColor = Color(0xFFE5E5E5)       // Soft border
val TextPrimary = Color(0xFF000000)       // Pure Black text
val TextSecondary = Color(0xFF6E6E80)     // Soft gray text
val AccentPrimaryDark = Color(0xFFFFFFFF)      // Pure White for buttons in dark mode
val BackgroundScreenDark = Color(0xFF000000)   // Pure Black
val BackgroundChatBoxDark = Color(0xFF202123)  // Dark gray for inputs
val BorderColorDark = Color(0xFF323232)
val TextPrimaryDark = Color(0xFFFFFFFF)
val TextSecondaryDark = Color(0xFFC5C5D2)"""

import re
content = re.sub(r'// Claude-style Premium Theme Custom Colors.*', new_colors, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/core/theme/Color.kt", "w") as f:
    f.write(content)
