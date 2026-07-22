import java.util.regex.Pattern

fun main() {
    val prompt = "ddd\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]"
    
    val cleanPrompt = prompt
        .replace(Regex("\\[REASONING MODE:.*?\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[ARABIC POST-PROCESSING.*?\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[WEB SEARCH.*?\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[An image attachment.*?\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[Document Attached.*?\\]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\[.*?\\]", RegexOption.DOT_MATCHES_ALL), "")
        .trim()
        
    println("Original:")
    println(prompt)
    println("\nClean:")
    println(cleanPrompt)
}
main()
