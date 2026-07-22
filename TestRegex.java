public class TestRegex {
    public static void main(String[] args) {
        String prompt = "ddd\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]";
        String clean = prompt.replaceAll("(?i)\\[REASONING MODE:.*?\\]", "");
        String clean2 = prompt.replaceAll("(?s)\\[.*?\\]", "");
        System.out.println("Clean1: " + clean);
        System.out.println("Clean2: " + clean2);
    }
}
