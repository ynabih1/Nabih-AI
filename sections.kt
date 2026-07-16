
@Composable
fun AppearanceSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "سمة التطبيق" else "Theme & Appearance", Icons.Rounded.Palette) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isArabic) "اختر سمة الألوان المفضلة لديك:" else "Choose your preferred color theme:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppTheme.values().forEach { theme ->
                    val label = when(theme) {
                        AppTheme.LIGHT -> if (isArabic) "فاتح" else "Light"
                        AppTheme.DARK -> if (isArabic) "داكن" else "Dark"
                        AppTheme.SYSTEM -> if (isArabic) "النظام" else "System"
                    }
                    FilterChip(
                        selected = settings.theme == theme,
                        onClick = { viewModel.updateTheme(theme) },
                        label = { Text(label, modifier = Modifier.padding(vertical = 4.dp)) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "اللغة المعتمدة" else "System Language", Icons.Rounded.Language) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isArabic) "تغيير لغة واجهة المستخدم المعتمدة:" else "Change active language of user interface:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AppLanguage.values().forEach { language ->
                    FilterChip(
                        selected = settings.language == language,
                        onClick = { viewModel.updateLanguage(language) },
                        label = { Text(language.displayName, modifier = Modifier.padding(vertical = 4.dp)) },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }
            }
        }
    }
}

@Composable
fun StorageSettingsSection(isArabic: Boolean, onClearChatHistory: () -> Unit) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    SettingsSectionCard(if (isArabic) "سجل المحادثات والبيانات" else "Conversation Data & Storage", Icons.Rounded.Storage) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "سجل المحادثات" else "Conversation Data", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (isArabic) "حذف كل المحادثات المحفوظة محلياً بشكل نهائي." else "Permanently wipe all offline conversation records.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showClearDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(if (isArabic) "حذف المحادثات" else "Wipe Chats")
                }
            }
        }

        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text(if (isArabic) "تأكيد مسح كافة المحادثات" else "Wipe All Chats?", fontWeight = FontWeight.Bold) },
                text = { Text(if (isArabic) "هل أنت متأكد تماماً أنك تريد حذف كافة سجلات المحادثات المخزنة محلياً؟ لا يمكن التراجع عن هذا الإجراء." else "Are you absolutely certain you want to purge all local conversation database logs? This action is permanent and cannot be undone.") },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            showClearDialog = false
                            onClearChatHistory()
                            Toast.makeText(context, if (isArabic) "تم حذف كافة المحادثات" else "All conversations cleared", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text(if (isArabic) "حذف الكل" else "Confirm Wipe")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }
    }
}
