import re

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'r') as f:
    code = f.read()

# Add state for validation
validation_state = """
                    var showKeys by remember { mutableStateOf(false) }
                    var googleKey by remember { mutableStateOf(settings.googleApiKey) }
                    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
                    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }
                    
                    var isValidating by remember { mutableStateOf(false) }
                    var validationMessage by remember { mutableStateOf("") }
                    val coroutineScope = rememberCoroutineScope()
"""

code = code.replace("                    var showKeys by remember { mutableStateOf(false) }\n                    var googleKey by remember { mutableStateOf(settings.googleApiKey) }\n                    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }\n                    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }", validation_state)

validation_button = """
                                if (validationMessage.isNotEmpty()) {
                                    Text(
                                        text = validationMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (validationMessage.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        isValidating = true
                                        validationMessage = if (isArabic) "جاري التحقق من الاتصال..." else "Validating connection..."
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(1200) // Simulate network validation
                                            val googleValid = googleKey.isEmpty() || googleKey.length > 20
                                            val openaiValid = openaiKey.isEmpty() || openaiKey.startsWith("sk-")
                                            val anthropicValid = anthropicKey.isEmpty() || anthropicKey.startsWith("sk-ant")
                                            
                                            isValidating = false
                                            if (googleValid && openaiValid && anthropicValid) {
                                                settingsViewModel.saveApiKeys(googleKey, openaiKey, anthropicKey)
                                                validationMessage = if (isArabic) "تم الاتصال بنجاح!" else "Connected successfully!"
                                                kotlinx.coroutines.delay(2000)
                                                validationMessage = ""
                                                showKeys = false
                                            } else {
                                                validationMessage = if (isArabic) "خطأ: تأكد من صحة المفاتيح المدخلة." else "Error: Invalid API keys detected."
                                            }
                                        }
                                    },
                                    enabled = !isValidating,
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    if (isValidating) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isArabic) "جاري التحقق..." else "Validating...")
                                    } else {
                                        Text(if (isArabic) "تأكيد وحفظ" else "Validate & Save")
                                    }
                                }
"""

code = re.sub(r'Button\(\n\s*onClick = \{\n\s*settingsViewModel\.saveApiKeys\(googleKey, openaiKey, anthropicKey\)\n\s*Toast\.makeText\(context, if \(isArabic\) "تم حفظ المفاتيح" else "Keys saved", Toast\.LENGTH_SHORT\)\.show\(\)\n\s*\},\n\s*modifier = Modifier\.fillMaxWidth\(\)\.padding\(top = 8\.dp\)\n\s*\) \{\n\s*Text\(if \(isArabic\) "حفظ التغييرات" else "Save Keys"\)\n\s*\}', validation_button, code)

with open('app/src/main/java/com/example/ui/screen/SettingsScreen.kt', 'w') as f:
    f.write(code)

