import re
import os

path = 'app/src/main/java/com/example/feature/chat/MainScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# 1. Update Header
old_header = """            // Header: Nabih AI Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1967D2) // Match the logo blue color
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
            }"""

new_header = """            // Header: Nabih AI Logo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painter = painterResource(id = R.drawable.logo), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Nabih AI",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1967D2) // Match the logo blue color
                )
            }"""

if old_header in content:
    content = content.replace(old_header, new_header)
    print("Replaced Header")
else:
    print("Old Header not found")

# 2. Remove Search Button
search_button_pattern = r"""                item \{\s*NavigationDrawerItem\(\s*icon = \{ Icon\(Icons\.Rounded\.Search,.*?modifier = Modifier\.padding\(horizontal = 16\.dp, vertical = 2\.dp\)\s*\)\s*\}\s*"""
content = re.sub(search_button_pattern, "", content, flags=re.DOTALL)
print("Regex replace for search button run.")

# 3. Fix Account Button
old_account_click = """onClick = { /* Handle Account Click */ }"""
new_account_click = """onClick = { onNavigateTo("account"); onCloseDrawer() }"""
if old_account_click in content:
    content = content.replace(old_account_click, new_account_click)
    print("Replaced Account Click")
else:
    print("Account Click not found")

with open(path, 'w') as f:
    f.write(content)
