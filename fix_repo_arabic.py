with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    text = f.read()

old_system_prompt = """            val systemPrompt = "You are Nabih Ultra, the default AI assistant inside Nabih AI. " +
                    "Always provide accurate, natural, and professional responses. " +
                    "Understand user intent, preserve conversation context, and answer clearly. " +
                    "Never expose internal errors, debug messages, JSON, stack traces, or implementation details. " +
                    "If a response cannot be generated, apologize politely and ask the user to rephrase the question. " +
                    "Automatically improve spelling and grammar before generating the final answer. " +
                    "Current local time: ${System.currentTimeMillis()}\\n$memoriesStr\\n$searchContext\""""

new_system_prompt = """            var systemPrompt = "You are Nabih Ultra, the default AI assistant inside Nabih AI. " +
                    "Always provide accurate, natural, and professional responses. " +
                    "Understand user intent, preserve conversation context, and answer clearly. " +
                    "Never expose internal errors, debug messages, JSON, stack traces, or implementation details. " +
                    "If a response cannot be generated, apologize politely and ask the user to rephrase the question. " +
                    "Automatically improve spelling and grammar before generating the final answer. " +
                    "Current local time: ${System.currentTimeMillis()}\\n$memoriesStr\\n$searchContext"

            if (isArabic) {
                systemPrompt += "\\n\\n[ARABIC POST-PROCESSING LAYER: You MUST enforce proper Arabic grammar and utilize essential diacritics (Tashkeel) to resolve ambiguity. Ensure zero spelling errors, especially regarding Hamza (أ, إ, ء), Taa Marbuta (ة vs ه), and Alef Maksura (ى vs ي). Your output must be high-quality, eloquent, and perfectly formatted Arabic.]"
            }"""

if old_system_prompt in text:
    text = text.replace(old_system_prompt, new_system_prompt)
    print("Replaced system prompt!")
else:
    print("System prompt not found!")

# Also applying the post-processor to the streamed chunks
old_collect = """            ).collect { currentFullText ->
                responseText = currentFullText
                emit(currentFullText)
            }"""

new_collect = """            ).collect { currentFullText ->
                val processedText = if (isArabic) com.example.utils.ArabicPostProcessor.process(currentFullText) else currentFullText
                responseText = processedText
                emit(processedText)
            }"""

if old_collect in text:
    text = text.replace(old_collect, new_collect)
    print("Replaced collect!")
else:
    print("Collect not found!")

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(text)
