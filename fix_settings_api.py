import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

api_section = """
            // API Keys Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                ) {
                    var showKeys by remember { mutableStateOf(false) }
                    var googleKey by remember { mutableStateOf(settings.googleApiKey) }
                    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
                    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Outlined.Key, null, tint = MaterialTheme.colorScheme.primary)
                                Text(if (isArabic) "مفاتيح API الخاصة بك" else "Your API Keys", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            IconButton(onClick = { showKeys = !showKeys }) {
                                Icon(if (showKeys) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isArabic) "تم تخزين هذه المفاتيح بشكل آمن على جهازك فقط. لن يتم مشاركتها." else "These keys are securely stored locally on your device and never shared.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )

                        AnimatedVisibility(visible = showKeys) {
                            Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = googleKey,
                                    onValueChange = { googleKey = it },
                                    label = { Text("Google Gemini API Key") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = openaiKey,
                                    onValueChange = { openaiKey = it },
                                    label = { Text("OpenAI API Key") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = anthropicKey,
                                    onValueChange = { anthropicKey = it },
                                    label = { Text("Anthropic Claude API Key") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Button(
                                    onClick = {
                                        settingsViewModel.saveApiKeys(googleKey, openaiKey, anthropicKey, "")
                                        Toast.makeText(context, if (isArabic) "تم حفظ المفاتيح" else "Keys saved", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(if (isArabic) "حفظ التغييرات" else "Save Keys")
                                }
                            }
                        }
                    }
                }
            }
"""

code = code.replace("            // About Screen Card", api_section + "\n            // About Screen Card")

# fix settingsViewModel.saveApiKeys call since we changed it to 3 arguments
code = code.replace("settingsViewModel.saveApiKeys(googleKey, openaiKey, anthropicKey, \"\")", "settingsViewModel.saveApiKeys(googleKey, openaiKey, anthropicKey)")

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

