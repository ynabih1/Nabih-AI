import os
path = 'app/src/main/java/com/example/data/repository/ChatRepository.kt'
with open(path, 'r') as f:
    text = f.read()

old_prompt = """            val systemPrompt = "You are Nabih AI, a fast, efficient AI assistant. " +
                    "You speak Arabic and English natively. " +
                    "CRITICAL INSTRUCTIONS: You must provide concise and direct answers. Avoid unnecessary explanations, introductions, or repeated information. Answer exactly what the user asks using the shortest accurate answer possible. Do not use greetings or filler phrases. Do not repeat the user's question. Add details only when explicitly requested. If information is unknown, say so briefly. Provide accurate, structured answers. " +
                    "Current local time: ${System.currentTimeMillis()}\\n$memoriesStr\\n$searchContext\""""

new_prompt = """            val systemPrompt = "You are Nabih Ultra, the default AI assistant inside Nabih AI. " +
                    "Always provide accurate, natural, and professional responses. " +
                    "Understand user intent, preserve conversation context, and answer clearly. " +
                    "Never expose internal errors, debug messages, JSON, stack traces, or implementation details. " +
                    "If a response cannot be generated, apologize politely and ask the user to rephrase the question. " +
                    "Automatically improve spelling and grammar before generating the final answer. " +
                    "Current local time: ${System.currentTimeMillis()}\\n$memoriesStr\\n$searchContext\""""

text = text.replace(old_prompt, new_prompt)

# actually wait, let's just do a regex replace because of the ${} interpolations which might be tricky with newlines.

import re
text = re.sub(r'val systemPrompt = "You are Nabih AI.*?searchContext"', new_prompt, text, flags=re.DOTALL)

with open(path, 'w') as f:
    f.write(text)
