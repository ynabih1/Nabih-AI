package com.example.core.utils

import android.content.Context
import android.net.Uri
import java.io.InputStream
import java.lang.StringBuilder
import java.util.zip.ZipInputStream
import java.util.zip.Inflater

object DocumentParser {

    fun parseUri(context: Context, uri: Uri): String {
        val fileName = getFileName(context, uri) ?: "Attached File"
        val extension = fileName.substringAfterLast('.', "").lowercase()
        
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return "Failed to open file stream."
            inputStream.use { stream ->
                when (extension) {
                    "txt", "md", "json", "xml", "kt", "java", "py", "js" -> stream.bufferedReader().use { it.readText() }
                    "csv" -> parseCsv(stream)
                    "docx" -> parseDocx(stream)
                    "pptx" -> parsePptx(stream)
                    "xlsx" -> parseXlsx(stream)
                    "pdf" -> parsePdf(stream)
                    "zip" -> parseZip(stream)
                    "doc", "xls", "ppt" -> parseBinaryDocument(stream)
                    else -> "Binary file attached (${extension.uppercase()}). The document metadata is available."
                }
            }
        } catch (e: Exception) {
            "Error parsing file: ${e.localizedMessage}"
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path?.substringAfterLast('/')
        }
        return result
    }

    private fun parseCsv(inputStream: InputStream): String {
        val reader = inputStream.bufferedReader()
        val builder = StringBuilder("CSV Table Data:\n")
        var lineCount = 0
        reader.forEachLine { line ->
            if (lineCount < 500) { // Limit to 500 lines to prevent token blowing
                val cells = line.split(",")
                builder.append("| ").append(cells.joinToString(" | ")).append(" |\n")
                if (lineCount == 0) {
                    builder.append("|").append(cells.joinToString("|") { "---" }).append("|\n")
                }
                lineCount++
            }
        }
        return builder.toString()
    }

    private fun parseDocx(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        var content = ""
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val xmlText = zipInputStream.bufferedReader().readText()
                val regex = Regex("<w:t.*?>(.*?)</w:t>")
                val matches = regex.findAll(xmlText)
                content = matches.map { it.groupValues[1] }.joinToString(" ")
                break
            }
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        return content.ifBlank { "Empty Word document." }
    }

    private fun parsePptx(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        val slidesText = mutableListOf<Pair<Int, String>>()
        while (entry != null) {
            if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                val slideNum = entry.name.filter { it.isDigit() }.toIntOrNull() ?: 0
                val xmlText = zipInputStream.bufferedReader().readText()
                val regex = Regex("<a:t.*?>(.*?)</a:t>")
                val matches = regex.findAll(xmlText)
                val slideContent = matches.map { it.groupValues[1] }.joinToString(" ")
                slidesText.add(Pair(slideNum, slideContent))
            }
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        return slidesText.sortedBy { it.first }
            .joinToString("\n\n") { "Slide ${it.first}: ${it.second}" }
            .ifBlank { "Empty PowerPoint presentation." }
    }

    private fun parseXlsx(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        var sharedStringsXml = ""
        val sheetsXml = mutableMapOf<String, String>()
        
        while (entry != null) {
            if (entry.name == "xl/sharedStrings.xml") {
                sharedStringsXml = zipInputStream.bufferedReader().readText()
            } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                sheetsXml[entry.name] = zipInputStream.bufferedReader().readText()
            }
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        
        val stringsList = mutableListOf<String>()
        if (sharedStringsXml.isNotEmpty()) {
            val regex = Regex("<t.*?>(.*?)</t>")
            regex.findAll(sharedStringsXml).forEach {
                stringsList.add(it.groupValues[1])
            }
        }
        
        val result = StringBuilder()
        sheetsXml.keys.sorted().forEach { sheetName ->
            val xml = sheetsXml[sheetName] ?: ""
            val displayName = sheetName.substringAfterLast('/').substringBefore('.')
            result.append("--- Excel Sheet: $displayName ---\n")
            
            val rowRegex = Regex("<row.*?>(.*?)</row>")
            val rows = rowRegex.findAll(xml)
            
            rows.forEach { rowMatch ->
                val rowXml = rowMatch.groupValues[1]
                val cellRegex = Regex("<c r=\"([A-Z]+)(\\d+)\"(?: t=\"([^\"]+)\")?.*?><v>(.*?)</v></c>")
                val cells = cellRegex.findAll(rowXml)
                
                val rowValues = mutableListOf<String>()
                cells.forEach { cellMatch ->
                    val col = cellMatch.groupValues[1]
                    val type = cellMatch.groupValues[3]
                    val value = cellMatch.groupValues[4]
                    
                    val displayValue = if (type == "s") {
                        val idx = value.toIntOrNull()
                        if (idx != null && idx >= 0 && idx < stringsList.size) {
                            stringsList[idx]
                        } else {
                            value
                        }
                    } else {
                        value
                    }
                    rowValues.add("$col: $displayValue")
                }
                if (rowValues.isNotEmpty()) {
                    result.append(rowValues.joinToString(" | ")).append("\n")
                }
            }
            result.append("\n")
        }
        return result.toString().ifBlank { "Empty Excel sheet." }
    }

    private fun parsePdf(inputStream: InputStream): String {
        try {
            val bytes = inputStream.readBytes()
            val pdfText = StringBuilder()
            
            var idx = 0
            while (idx < bytes.size - 6) {
                if (bytes[idx] == 's'.code.toByte() &&
                    bytes[idx+1] == 't'.code.toByte() &&
                    bytes[idx+2] == 'r'.code.toByte() &&
                    bytes[idx+3] == 'e'.code.toByte() &&
                    bytes[idx+4] == 'a'.code.toByte() &&
                    bytes[idx+5] == 'm'.code.toByte()) {
                    
                    idx += 6
                    while (idx < bytes.size && (bytes[idx] == '\r'.code.toByte() || bytes[idx] == '\n'.code.toByte())) {
                        idx++
                    }
                    
                    val streamStart = idx
                    var streamEnd = -1
                    while (idx < bytes.size - 9) {
                        if (bytes[idx] == 'e'.code.toByte() &&
                            bytes[idx+1] == 'n'.code.toByte() &&
                            bytes[idx+2] == 'd'.code.toByte() &&
                            bytes[idx+3] == 's'.code.toByte() &&
                            bytes[idx+4] == 't'.code.toByte() &&
                            bytes[idx+5] == 'r'.code.toByte() &&
                            bytes[idx+6] == 'e'.code.toByte() &&
                            bytes[idx+7] == 'a'.code.toByte() &&
                            bytes[idx+8] == 'm'.code.toByte()) {
                            streamEnd = idx
                            break
                        }
                        idx++
                    }
                    
                    if (streamEnd != -1) {
                        val streamBytes = bytes.copyOfRange(streamStart, streamEnd)
                        var decompressedBytes: ByteArray? = null
                        try {
                            val normalDecompressor = Inflater()
                            normalDecompressor.setInput(streamBytes)
                            val bos = java.io.ByteArrayOutputStream()
                            val buf = ByteArray(1024)
                            while (!normalDecompressor.finished()) {
                                val count = normalDecompressor.inflate(buf)
                                if (count == 0 && normalDecompressor.needsInput()) break
                                bos.write(buf, 0, count)
                            }
                            decompressedBytes = bos.toByteArray()
                            normalDecompressor.end()
                        } catch (e: Exception) {
                            try {
                                val rawDecompressor = Inflater(true)
                                rawDecompressor.setInput(streamBytes)
                                val bos = java.io.ByteArrayOutputStream()
                                val buf = ByteArray(1024)
                                while (!rawDecompressor.finished()) {
                                    val count = rawDecompressor.inflate(buf)
                                    if (count == 0 && rawDecompressor.needsInput()) break
                                    bos.write(buf, 0, count)
                                }
                                decompressedBytes = bos.toByteArray()
                                rawDecompressor.end()
                            } catch (e2: Exception) {}
                        }
                        
                        val textToParse = if (decompressedBytes != null) {
                            String(decompressedBytes, Charsets.UTF_8)
                        } else {
                            String(streamBytes, Charsets.US_ASCII)
                        }
                        
                        val matches = Regex("\\((.*?)\\)").findAll(textToParse)
                        for (m in matches) {
                            val matchedText = m.groupValues[1]
                            if (matchedText.length > 1 && matchedText.all { it.code in 32..126 || it.code in 160..255 || it == '\n' || it == '\r' || it == '\t' }) {
                                pdfText.append(matchedText).append(" ")
                            }
                        }
                    }
                }
                idx++
            }
            
            if (pdfText.isEmpty()) {
                val rawString = String(bytes, Charsets.ISO_8859_1)
                val matches = Regex("\\((.*?)\\)\\s*Tj|\\((.*?)\\)\\s*TJ").findAll(rawString)
                for (m in matches) {
                    val matchedText = m.groupValues[1].ifEmpty { m.groupValues[2] }
                    if (matchedText.length > 2 && matchedText.all { it.code in 32..126 || it == ' ' }) {
                        pdfText.append(matchedText).append(" ")
                    }
                }
            }
            
            return pdfText.toString().trim().ifBlank { "PDF contains no extractable text." }
        } catch (e: Exception) {
            return "Failed to parse PDF text: ${e.localizedMessage}"
        }
    }

    private fun parseZip(inputStream: InputStream): String {
        val zipInputStream = ZipInputStream(inputStream)
        var entry = zipInputStream.nextEntry
        val result = StringBuilder("ZIP Archive Contents:\n")
        var filesProcessed = 0
        while (entry != null && filesProcessed < 15) {
            val isDirectory = entry.isDirectory
            result.append(if (isDirectory) "[Dir] " else "[File] ").append(entry.name)
            if (!isDirectory) {
                val extension = entry.name.substringAfterLast('.', "").lowercase()
                if (extension in listOf("txt", "csv", "json", "md", "xml", "kt", "java", "py", "js", "html", "css")) {
                    val text = zipInputStream.bufferedReader().readText()
                    result.append(":\n\"\"\"\n").append(text.take(1500)).append("\n\"\"\"\n")
                    filesProcessed++
                } else {
                    result.append(" (${entry.size} bytes)\n")
                }
            } else {
                result.append("\n")
            }
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        return result.toString()
    }

    private fun parseBinaryDocument(inputStream: InputStream): String {
        try {
            val bytes = inputStream.readBytes()
            val text = StringBuilder()
            var currentWord = StringBuilder()
            for (b in bytes) {
                val char = b.toInt().toChar()
                if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in " .,?!;:()[]{}@_-+=*&%#$/\\\n\r\t") {
                    currentWord.append(char)
                } else {
                    if (currentWord.length > 4) {
                        text.append(currentWord).append(" ")
                    }
                    currentWord = StringBuilder()
                }
            }
            if (currentWord.length > 4) {
                text.append(currentWord)
            }
            val cleanedText = text.toString()
                .replace(Regex("\\s+"), " ")
                .split(" ")
                .filter { word -> 
                    word.length < 30 && !word.contains(Regex("[^a-zA-Z0-9.,?!]"))
                }
                .joinToString(" ")
            
            return cleanedText.take(5000)
        } catch (e: Exception) {
            return "Failed to parse binary document: ${e.localizedMessage}"
        }
    }
}
