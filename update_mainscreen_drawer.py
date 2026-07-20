with open('app/src/main/java/com/example/chat/MainScreen.kt', 'r') as f:
    text = f.read()

drawer_header_old = """            // Header with statusBarsPadding and spacious branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }"""

drawer_header_new = """            // Header with statusBarsPadding and spacious branding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }"""

if drawer_header_old in text:
    text = text.replace(drawer_header_old, drawer_header_new)
    print("Drawer header updated")
else:
    print("Drawer header NOT FOUND")

with open('app/src/main/java/com/example/chat/MainScreen.kt', 'w') as f:
    f.write(text)
