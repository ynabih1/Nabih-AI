with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

import re

empty_state_old = """@Composable
fun EmptyChatState(isArabic: Boolean, onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = if (isArabic) "كيف يمكنني مساعدتك اليوم؟" else "How can I assist you today?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}"""

empty_state_new = """@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmptyChatState(isArabic: Boolean, onSuggestionClick: (String) -> Unit) {
    val suggestions = if (isArabic) {
        listOf(
            "اكتب لي رسالة بريد إلكتروني احترافية لطلب إجازة",
            "لخص لي أهم الأحداث التاريخية في القرن العشرين",
            "اقترح لي وصفة عشاء صحية وسريعة",
            "اشرح لي مفهوم الحوسبة الكمية ببساطة"
        )
    } else {
        listOf(
            "Write a professional email requesting time off",
            "Summarize the key historical events of the 20th century",
            "Suggest a quick and healthy dinner recipe",
            "Explain quantum computing simply"
        )
    }

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
        Spacer(modifier = Modifier.height(48.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            suggestions.forEach { suggestion ->
                Surface(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSuggestionClick(suggestion) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}"""

text = text.replace(empty_state_old, empty_state_new)

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
