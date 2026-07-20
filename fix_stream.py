with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r') as f:
    text = f.read()

bad_block = """            var line: String?
            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            var previouslyProcessedBlocks = 0
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                accumulated += l"""

good_block = """            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            var previouslyProcessedBlocks = 0
            val buffer = CharArray(4096)
            var readChars: Int
            while (reader.read(buffer).also { readChars = it } != -1) {
                val chunk = String(buffer, 0, readChars)
                accumulated += chunk"""

if bad_block in text:
    text = text.replace(bad_block, good_block)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w') as f:
        f.write(text)
    print("Fixed Gemini stream reading")
else:
    print("Block not found!")
