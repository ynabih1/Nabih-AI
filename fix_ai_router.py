import re

with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r') as f:
    text = f.read()

old_code = """                    val chunkCollector = mutableListOf<String>()
                    
                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText
                    ).collect { chunk ->
                        chunkCollector.add(chunk)
                        emit(chunkCollector.joinToString(""))
                    }"""

new_code = """                    val chunkCollector = StringBuilder()
                    var validated = false
                    
                    provider.generateResponseStream(
                        modelId = modelId,
                        apiKey = currentApiKey,
                        systemPrompt = systemPrompt,
                        prompt = prompt,
                        history = history,
                        attachedBase64Image = attachedBase64Image,
                        attachedDocText = attachedDocText
                    ).collect { chunk ->
                        chunkCollector.append(chunk)
                        val accumulated = chunkCollector.toString()
                        
                        if (!validated) {
                            if (accumulated.length > 25) {
                                val testStrLower = accumulated.trimStart().lowercase()
                                val isBad = testStrLower.startsWith("{") || testStrLower.startsWith("[") || 
                                            testStrLower.startsWith("exception:") || testStrLower.startsWith("error:") ||
                                            testStrLower.startsWith("api_error") ||
                                            testStrLower.contains("match result value") || testStrLower.contains("stack trace") || 
                                            testStrLower.contains("debug output") || testStrLower.contains("raw json parsing error")
                                if (isBad) {
                                    throw Exception("VALIDATION_FAILED: Bad response")
                                }
                                validated = true
                                emit(accumulated)
                            }
                        } else {
                            emit(accumulated)
                        }
                    }
                    
                    if (!validated) {
                        val finalStr = chunkCollector.toString().trim()
                        val lower = finalStr.lowercase()
                        if (finalStr.isEmpty() || lower == "null" || lower == "undefined" || lower.startsWith("{") || lower.startsWith("[") || lower.contains("match result value")) {
                            throw Exception("VALIDATION_FAILED: Empty or invalid response")
                        }
                        emit(finalStr)
                    }"""

text = text.replace(old_code, new_code)

with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w') as f:
    f.write(text)
