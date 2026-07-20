import re
with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'r') as f:
    text = f.read()

bad_prompt = """            val systemPrompt = "You are Nabih Ultra, the default AI assistant inside Nabih AI. " +
                    "Always provide accurate, natural, and professional responses. " +
                    "Understand user intent, preserve conversation context, and answer clearly. " +
                    "Never expose internal errors, debug messages, JSON, stack traces, or implementation details. " +
                    "If a response cannot be generated, apologize politely and ask the user to rephrase the question. " +
                    "Automatically improve spelling and grammar before generating the final answer. " +
                    "Current local time: ${System.currentTimeMillis()}
$memoriesStr
$searchContext\""""

good_prompt = """            val systemPrompt = "You are Nabih Ultra, the default AI assistant inside Nabih AI. " +
                    "Always provide accurate, natural, and professional responses. " +
                    "Understand user intent, preserve conversation context, and answer clearly. " +
                    "Never expose internal errors, debug messages, JSON, stack traces, or implementation details. " +
                    "If a response cannot be generated, apologize politely and ask the user to rephrase the question. " +
                    "Automatically improve spelling and grammar before generating the final answer. " +
                    "Current local time: ${System.currentTimeMillis()}\\n$memoriesStr\\n$searchContext\""""

text = text.replace(bad_prompt, good_prompt)

with open('app/src/main/java/com/example/data/repository/ChatRepository.kt', 'w') as f:
    f.write(text)
