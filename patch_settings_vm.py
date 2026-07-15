import re

with open('app/src/main/java/com/example/feature/settings/SettingsViewModel.kt', 'r') as f:
    content = f.read()

# Replace the Nabih Ultra Key Validation block
nabih_block = """        // 2. Nabih Ultra Key Validation
        if (trimmedNabih.isNotEmpty() && trimmedNabih != settings.value.nabihApiKey) {
            val res = validateKeyGeneric("Nabih Ultra", trimmedNabih, isArabic) {
                com.example.core.network.NetworkClient.geminiService.generateContent(
                    model = "gemini-1.5-flash",
                    apiKey = trimmedNabih,
                    request = com.example.core.network.GeminiRequest(
                        contents = listOf(
                            com.example.core.network.GeminiContent(
                                parts = listOf(com.example.core.network.GeminiPart(text = "Ping"))
                            )
                        ),
                        generationConfig = com.example.core.network.GeminiGenerationConfig(maxOutputTokens = 1)
                    )
                )
            }
            results.add(res)
        }"""

if nabih_block in content:
    print("Found nabih block in SettingsViewModel")
else:
    print("Nabih block not found")
