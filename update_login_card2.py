with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'r') as f:
    text = f.read()

old_box = """            // Main Interactive Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 24.dp),
                contentAlignment = Alignment.TopCenter
            ) {"""

new_box = """            // Main Interactive Content Area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.TopCenter
                ) {"""

if old_box in text:
    text = text.replace(old_box, new_box)

    # Now add the closing brace for the Card
    # We find:
    #             // Footer / Bottom Brand Details
    # and insert the closing brace before it.

    old_footer = """            // Footer / Bottom Brand Details"""
    new_footer = """            }
            
            // Footer / Bottom Brand Details"""
    text = text.replace(old_footer, new_footer)

    with open('app/src/main/java/com/example/auth/LoginScreen.kt', 'w') as f:
        f.write(text)
    print("Updated LoginScreen with Card")
else:
    print("Box not found")
