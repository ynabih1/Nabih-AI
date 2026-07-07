with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

old_code = """@Composable
fun BottomInputArea(
    isArabic: Boolean,
    onSend: (String) -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Row("""

new_code = """@Composable
fun BottomInputArea(
    isArabic: Boolean,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit,
    attachedImageUri: android.net.Uri? = null,
    attachedDocUri: android.net.Uri? = null,
    attachedDocName: String? = null,
    onAttachImage: (android.net.Uri?) -> Unit = {},
    onAttachDocument: (android.net.Uri?, String?) -> Unit = { _, _ -> },
    onRemoveAttachment: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var text by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onAttachImage(uri)
        }
        showAttachmentMenu = false
    }

    val docPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            var name = "Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
            onAttachDocument(uri, name)
        }
        showAttachmentMenu = false
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 8.dp
    ) {
        Column {
            // Attachment Preview Area
            if (attachedImageUri != null || attachedDocUri != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (attachedImageUri != null) Icons.Outlined.Image else Icons.Outlined.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (attachedImageUri != null) (if (isArabic) "صورة مرفقة" else "Image attached") else attachedDocName ?: "Document",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    IconButton(onClick = onRemoveAttachment, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row("""

code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

