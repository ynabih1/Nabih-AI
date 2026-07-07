import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# I want to add copy, share to MessageItem. Let's just update MessageItem directly

message_item = """@Composable
fun MessageItem(message: com.example.data.database.Message, isArabic: Boolean, isLoading: Boolean = false) {
    val isUser = message.role == "user"
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            modifier = Modifier.weight(1f, fill = false).clickable(enabled = !isLoading) { showMenu = true }
        ) {
            Box {
                if (isLoading) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(message.content, style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "نسخ" else "Copy") },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Copied Text", message.content)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (isArabic) "تم النسخ" else "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (isArabic) "مشاركة" else "Share") },
                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, message.content)
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, null))
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}"""

# We need to replace the old MessageItem
import re

code = re.sub(r'@Composable\nfun MessageItem.*?^}\n', message_item + "\n", code, flags=re.DOTALL | re.MULTILINE)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

