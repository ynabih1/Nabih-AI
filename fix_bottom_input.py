import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

# Modify BottomInputArea declaration
old_bottom_input = """fun BottomInputArea(
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
    ) {"""

new_bottom_input = """import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.provider.OpenableColumns

@Composable
fun BottomInputArea(
    isArabic: Boolean,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    isGenerating: Boolean,
    onStop: () -> Unit,
    attachedImageUri: Uri? = null,
    attachedDocUri: Uri? = null,
    attachedDocName: String? = null,
    onAttachImage: (Uri?) -> Unit = {},
    onAttachDocument: (Uri?, String?) -> Unit = { _, _ -> },
    onRemoveAttachment: () -> Unit = {}
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachImage(uri)
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var name = "Document"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
            onAttachDocument(uri, name)
        }
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
"""

code = code.replace(old_bottom_input, new_bottom_input)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

