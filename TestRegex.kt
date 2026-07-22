fun main() {
    val prompt = "ddd\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]"
    val res1 = prompt.replace(Regex("\\[REASONING MODE:.*?\\]", RegexOption.IGNORE_CASE), "")
    val res2 = prompt.replace(Regex("\\[.*?\\]", RegexOption.DOT_MATCHES_ALL), "")
    println("RES1: " + res1)
    println("RES2: " + res2)
}
