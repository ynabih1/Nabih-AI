import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    content = f.read()

model_section = """@Composable
fun DefaultModelSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    SettingsSectionCard(if (isArabic) "النموذج الافتراضي" else "Default AI Model", Icons.Outlined.AutoAwesome) {
        val availableModels = listOf(
            AiModel.NABIH_ULTRA,
            AiModel.GPT_4O,
            AiModel.GEMINI_PRO,
            AiModel.CLAUDE_SONNET
        )
        
        Column(modifier = Modifier.padding(16.dp)) {
            availableModels.forEach { model ->
                val isLocked = when (model) {
                    AiModel.NABIH_ULTRA -> false
                    AiModel.GPT_4O -> settings.openaiApiKey.isEmpty()
                    AiModel.GEMINI_PRO -> settings.googleApiKey.isEmpty()
                    AiModel.CLAUDE_SONNET -> settings.anthropicApiKey.isEmpty()
                    else -> false
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isLocked) {
                            viewModel.updateDefaultModel(model)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    RadioButton(
                        selected = settings.defaultModel == model,
                        onClick = { viewModel.updateDefaultModel(model) },
                        enabled = !isLocked
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = model.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                        if (isLocked) {
                            Text(
                                text = if (isArabic) "يتطلب مفتاح API" else "Requires API Key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            )
                        } else if (model == AiModel.NABIH_ULTRA) {
                            Text(
                                text = if (isArabic) "متاح دائماً مجاناً" else "Always available for free",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = if (isArabic) "متصل وجاهز" else "Connected and Ready",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
"""

if "fun DefaultModelSection" not in content:
    content = content + "\n\n" + model_section

# Insert it in the lazy column
lazy_col_pattern = r'(item \{\s*ApiKeysSection\(settings, settingsViewModel, isArabic\)\s*\})'
replacement = r'item {\n                DefaultModelSection(settings, settingsViewModel, isArabic)\n            }\n            \1'
content = re.sub(lazy_col_pattern, replacement, content)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(content)
