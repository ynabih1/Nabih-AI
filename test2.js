const prompt = `ddd
[REASONING MODE: AUTO. Analyze the user's request, select the optimal reasoning strategy (Fast, Deep Thinking, Creative, or Coding) under the hood, and tailor your formatting precisely to match.]`;
console.log("Stripped: " + prompt.replace(/\[REASONING MODE:.*?\]/ig, "").replace(/\[.*?\]/sg, ""));
