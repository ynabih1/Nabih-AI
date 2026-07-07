import re

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    content = f.read()

# 1. Handle missing / invalid key for NABIH
old_nabih = """        if (actualModel.provider == ApiProvider.NABIH) {
            finalApiKey = BuildConfig.GEMINI_API_KEY
            actualModel = AiModel.GEMINI_FLASH // map under the hood
            // Bypass user key requirement for Nabih Ultra"""
            
new_nabih = """        if (actualModel.provider == ApiProvider.NABIH) {
            finalApiKey = BuildConfig.GEMINI_API_KEY
            if (finalApiKey.isEmpty() || finalApiKey == "MY_GEMINI_API_KEY" || finalApiKey.startsWith("YOUR_")) {
                emit("Nabih Ultra is currently offline. Please configure the backend API key or try again later.")
                return@flow
            }
            actualModel = AiModel.GEMINI_FLASH // map under the hood
            // Bypass user key requirement for Nabih Ultra"""
            
content = content.replace(old_nabih, new_nabih)

# 2. Handle errors cleanly for NABIH
old_catch = """            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("ChatRepository", "API Request failed on try $currentTry", e)
                currentTry++
                if (currentTry < maxRetries) {
                    kotlinx.coroutines.delay(1000L * currentTry) // Exponential backoff
                }
            }"""

new_catch = """            } catch (e: Exception) {
                lastException = e
                android.util.Log.e("ChatRepository", "API Request failed on try $currentTry", e)
                
                val errorMessage = e.message ?: e.localizedMessage ?: ""
                if (model.provider == ApiProvider.NABIH) {
                    if (errorMessage.contains("429") || errorMessage.contains("Too Many Requests") || errorMessage.contains("Quota") || errorMessage.contains("exhausted")) {
                        success = true
                        responseText = "Nabih Ultra is currently at capacity. Please try again later."
                        break
                    } else if (errorMessage.contains("400") || errorMessage.contains("403")) {
                        success = true
                        responseText = "Nabih Ultra service is currently unavailable. Please try again later."
                        break
                    }
                }
                
                currentTry++
                if (currentTry < maxRetries) {
                    kotlinx.coroutines.delay(1000L * currentTry) // Exponential backoff
                }
            }"""
            
content = content.replace(old_catch, new_catch)

# 3. Clean up the final throw message for Nabih
old_throw = """        if (!success) {
            throw Exception(lastException?.localizedMessage ?: "Failed to connect to AI provider after $maxRetries attempts.")
        }"""
        
new_throw = """        if (!success) {
            if (model.provider == ApiProvider.NABIH) {
                emit("Nabih Ultra service is currently unavailable. Please try again later.")
                return@flow
            }
            throw Exception(lastException?.localizedMessage ?: "Failed to connect to AI provider after $maxRetries attempts.")
        }"""
        
content = content.replace(old_throw, new_throw)

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(content)
