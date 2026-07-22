const prompt = "ddd\n[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]";
console.log("RES1: " + prompt.replace(/\[REASONING MODE:.*?\]/i, ""));
console.log("RES2: " + prompt.replace(/\[.*?\]/s, ""));
