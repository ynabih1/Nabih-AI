with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'r') as f:
    text = f.read()

bad_block = """            var line: String?
            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                accumulated += l
                try {
                    val textToken = "\\\"text\\\":"
                    var startIndex = 0
                    while (true) {
                        val index = accumulated.indexOf(textToken, startIndex)
                        if (index == -1) break
                        val valStart = accumulated.indexOf('"', index + textToken.length)
                        if (valStart == -1) break
                        var valEnd = valStart + 1
                        var escaped = false
                        while (valEnd < accumulated.length) {
                            val c = accumulated[valEnd]
                            if (escaped) {
                                escaped = false
                            } else if (c == '\\\\') {
                                escaped = true
                            } else if (c == '"') {
                                break
                            }
                            valEnd++
                        }
                        if (valEnd < accumulated.length) {
                            val rawText = accumulated.substring(valStart + 1, valEnd)
                            val unescaped = unescapeJsonString(rawText)
                            if (unescaped.length > stringBuilder.length) {
                                val diff = unescaped.substring(stringBuilder.length)
                                stringBuilder.append(diff)
                                emit(diff)
                            }
                        }
                        startIndex = index + textToken.length
                    }"""

good_block = """            var line: String?
            val stringBuilder = java.lang.StringBuilder()
            var accumulated = ""
            var previouslyProcessedBlocks = 0
            while (reader.readLine().also { line = it } != null) {
                val l = line?.trim() ?: continue
                if (l.isEmpty()) continue
                accumulated += l
                try {
                    val textToken = "\\\"text\\\":"
                    var startIndex = 0
                    var parsedBlocks = 0
                    while (true) {
                        val index = accumulated.indexOf(textToken, startIndex)
                        if (index == -1) break
                        val valStart = accumulated.indexOf('"', index + textToken.length)
                        if (valStart == -1) break
                        var valEnd = valStart + 1
                        var escaped = false
                        while (valEnd < accumulated.length) {
                            val c = accumulated[valEnd]
                            if (escaped) {
                                escaped = false
                            } else if (c == '\\\\') {
                                escaped = true
                            } else if (c == '"') {
                                break
                            }
                            valEnd++
                        }
                        if (valEnd < accumulated.length) {
                            val rawText = accumulated.substring(valStart + 1, valEnd)
                            val unescaped = unescapeJsonString(rawText)
                            if (parsedBlocks >= previouslyProcessedBlocks) {
                                stringBuilder.append(unescaped)
                                emit(unescaped)
                                previouslyProcessedBlocks++
                            }
                            parsedBlocks++
                        }
                        startIndex = index + textToken.length
                    }"""

if bad_block in text:
    text = text.replace(bad_block, good_block)
    with open('app/src/main/java/com/example/data/remote/AiProvider.kt', 'w') as f:
        f.write(text)
    print("Fixed AiProvider.kt")
else:
    print("Block not found!")
