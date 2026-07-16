@Composable
fun AiConfigurationSection(settings: AppSettings, viewModel: SettingsViewModel, isArabic: Boolean) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var nabihKey by remember { mutableStateOf(settings.nabihApiKey) }
    var googleKey by remember { mutableStateOf(settings.googleApiKey) }
    var openaiKey by remember { mutableStateOf(settings.openaiApiKey) }
    var anthropicKey by remember { mutableStateOf(settings.anthropicApiKey) }

    val activeModelId = settings.defaultModel.id

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isArabic) "نماذج الذكاء الاصطناعي" else "AI Models",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val providersList = listOf(
            Triple("nabih", "Nabih Ultra", nabihKey),
            Triple("openai", "ChatGPT", openaiKey),
            Triple("claude", "Claude", anthropicKey),
            Triple("google", "Gemini", googleKey)
        )

        providersList.forEach { (id, name, savedKey) ->
            val isActive = (id == "nabih" && activeModelId == com.example.core.model.AiModel.NABIH_ULTRA.id) ||
                    (id == "google" && activeModelId == com.example.core.model.AiModel.GEMINI.id) ||
                    (id == "openai" && activeModelId == com.example.core.model.AiModel.CHATGPT.id) ||
                    (id == "claude" && activeModelId == com.example.core.model.AiModel.CLAUDE.id)

            var tempKey by remember(savedKey) { mutableStateOf(savedKey) }
            var isTesting by remember { mutableStateOf(false) }
            var testStatus by remember { mutableStateOf<Boolean?>(null) } 

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    if (isActive) {
                        Text(
                            text = if (isArabic) "النموذج الافتراضي" else "Default Model",
                            color = Color(0xFF10A37F),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (id == "nabih" || savedKey.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                val targetModel = when (id) {
                                    "nabih" -> com.example.core.model.AiModel.NABIH_ULTRA
                                    "google" -> com.example.core.model.AiModel.GEMINI
                                    "openai" -> com.example.core.model.AiModel.CHATGPT
                                    "claude" -> com.example.core.model.AiModel.CLAUDE
                                    else -> com.example.core.model.AiModel.NABIH_ULTRA
                                }
                                viewModel.updateDefaultModel(targetModel)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (isArabic) "تعيين كافتراضي" else "Set as Default", color = Color(0xFF10A37F))
                        }
                    }
                }

                if (id != "nabih") {
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it; testStatus = null },
                        placeholder = { Text(if (isArabic) "أدخل مفتاح الـ API" else "Enter API Key", color = Color.Gray) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Black,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (testStatus != null) {
                        Text(
                            text = if (testStatus == true) {
                                if (isArabic) "API Key يعمل بشكل صحيح" else "API Key works correctly"
                            } else {
                                if (isArabic) "API Key غير صالح، يرجى التحقق من المفتاح" else "Invalid API Key, please check your key"
                            },
                            color = if (testStatus == true) Color(0xFF10A37F) else Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (savedKey.isNotEmpty()) {
                        Text(
                            text = if (isArabic) "آخر حالة: متصل ومحفوظ" else "Last status: Connected and saved",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (tempKey.isBlank()) return@Button
                                coroutineScope.launch {
                                    isTesting = true
                                    testStatus = testApiKeyConnection(id, tempKey)
                                    isTesting = false
                                }
                            },
                            enabled = !isTesting && tempKey.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (isArabic) "اختبار الاتصال" else "Test Connection")
                            }
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    when (id) {
                                        "google" -> { googleKey = tempKey; viewModel.saveApiKeys(nabihKey, tempKey, openaiKey, anthropicKey) }
                                        "openai" -> { openaiKey = tempKey; viewModel.saveApiKeys(nabihKey, googleKey, tempKey, anthropicKey) }
                                        "claude" -> { anthropicKey = tempKey; viewModel.saveApiKeys(nabihKey, googleKey, openaiKey, tempKey) }
                                    }
                                    com.example.core.model.ModelRegistry.syncAndRefresh(context)
                                    Toast.makeText(context, if (isArabic) "تم الحفظ" else "Saved", Toast.LENGTH_SHORT).show()
                                    testStatus = null
                                }
                            },
                            enabled = testStatus == true,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (testStatus == true) Color.Black else Color.LightGray, 
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isArabic) "حفظ" else "Save")
                        }
                    }
                }
            }
        }
    }
}

suspend fun testApiKeyConnection(provider: String, key: String): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    if (key.isBlank()) return@withContext false
    try {
        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val request = when (provider) {
            "google" -> okhttp3.Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models?key=$key")
                .get()
                .build()
            "openai" -> okhttp3.Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $key")
                .get()
                .build()
            "claude" -> okhttp3.Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .header("x-api-key", key)
                .header("anthropic-version", "2023-06-01")
                .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                .build()
            else -> return@withContext false
        }
        
        val response = client.newCall(request).execute()
        
        if (provider == "claude") {
            response.code != 401 && response.code != 403
        } else {
            response.isSuccessful
        }
    } catch (e: Exception) {
        false
    }
}
