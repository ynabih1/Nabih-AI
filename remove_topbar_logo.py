filepath = 'app/src/main/java/com/example/chat/MainScreen.kt'
with open(filepath, 'r') as f:
    text = f.read()

old_title = """                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },"""
new_title = """                    title = {
                        Text(
                            text = "Nabih AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },"""

if old_title in text:
    text = text.replace(old_title, new_title)
    print("Replaced in TopAppBar")
else:
    print("TopAppBar pattern not found")

with open(filepath, 'w') as f:
    f.write(text)
