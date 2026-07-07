with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

old_code = """    val docPicker = androidx.activity.compose.rememberLauncherForActivityResult(
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
    }"""

new_code = """    val docPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            var name = "Document"
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
                }
            }
            
            // Validate size (e.g., max 10MB)
            if (size > 10 * 1024 * 1024) {
                android.widget.Toast.makeText(context, if (isArabic) "حجم الملف كبير جداً (الحد الأقصى 10 ميغابايت)" else "File size too large (Max 10MB)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                onAttachDocument(uri, name)
            }
        }
        showAttachmentMenu = false
    }

    // Add similar validation for imagePicker
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            var size = 0L
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
            if (size > 10 * 1024 * 1024) {
                android.widget.Toast.makeText(context, if (isArabic) "حجم الصورة كبير جداً (الحد الأقصى 10 ميغابايت)" else "Image size too large (Max 10MB)", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                onAttachImage(uri)
            }
        }
        showAttachmentMenu = false
    }"""

# Replace old image picker as well
image_picker_old = """    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onAttachImage(uri)
        }
        showAttachmentMenu = false
    }"""

code = code.replace(image_picker_old, "")
code = code.replace(old_code, new_code)

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

