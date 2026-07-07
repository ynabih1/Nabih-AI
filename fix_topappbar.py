import re

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'r') as f:
    code = f.read()

header_pattern = r"TopAppBar\([\s\S]*?colors = TopAppBarDefaults\.topAppBarColors\("

new_header = """TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.Unspecified,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Nabih AI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        var modelMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(
                                onClick = { modelMenuExpanded = true },
                                modifier = Modifier.padding(end = 4.dp),
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(text = selectedModel.displayName.split(" ").firstOrNull() ?: selectedModel.displayName, style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = modelMenuExpanded,
                                onDismissRequest = { modelMenuExpanded = false }
                            ) {
                                com.example.data.model.AiModel.values().forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.displayName) },
                                        onClick = {
                                            chatViewModel.selectModel(model)
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors("""

if re.search(header_pattern, code):
    code = re.sub(header_pattern, new_header, code)
    print("Replaced successfully")
else:
    print("Pattern not found")

with open('app/src/main/java/com/example/ui/screen/MainScreen.kt', 'w') as f:
    f.write(code)

