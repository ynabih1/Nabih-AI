with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    content = f.read()

old_search = """                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text(if (isArabic) "البحث" else "Search") },
                        selected = false,
                        onClick = {
                            // Can be mapped to a search route if available or just do nothing for now
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }"""

new_search = """                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                        label = { Text(if (isArabic) "البحث" else "Search") },
                        selected = false,
                        onClick = {
                            onNavigateTo("search")
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }"""

content = content.replace(old_search, new_search)
with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(content)
