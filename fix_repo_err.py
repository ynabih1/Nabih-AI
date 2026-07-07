import re

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    content = f.read()

old_err = """        if (!success) {
            if (model.provider == ApiProvider.NABIH) {
                val errorMsg = lastException?.localizedMessage ?: "Failed to connect to Nabih Ultra service."
                emit("Nabih Ultra error: $errorMsg")
                return@flow
            }
            throw Exception(lastException?.localizedMessage ?: "Failed to connect to AI provider after $maxRetries attempts.")
        }"""

new_err = """        if (!success) {
            val errorMsg = lastException?.localizedMessage ?: "Failed to connect to AI provider after $maxRetries attempts."
            val fullError = if (model.provider == ApiProvider.NABIH) "Nabih Ultra Service Error: $errorMsg" else errorMsg
            throw Exception(fullError)
        }"""

content = content.replace(old_err, new_err)

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(content)
