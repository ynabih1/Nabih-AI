with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

import re

# Since I just need to remove everything from `val suggestions =` to `FlowRow(...) { ... }`, I can do regex.

pattern = re.compile(r'@OptIn\(ExperimentalLayoutApi::class\)\s*@Composable\s*fun EmptyChatState\(isArabic: Boolean, onSuggestionClick: \(String\) -> Unit\) \{.*?(?=// Renders visual markdown nodes)', re.DOTALL)

new_func = """@Composable
fun EmptyChatState(isArabic: Boolean, onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .shadow(12.dp, CircleShape, spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.logo), 
                contentDescription = null, 
                tint = Color.Unspecified, 
                modifier = Modifier.size(64.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = if (isArabic) "مرحباً! كيف يمكنني مساعدتك اليوم؟" else "Hello! How can I help you today?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

"""

text = pattern.sub(new_func, text)

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
