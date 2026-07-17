import re
import os

path = 'app/src/main/java/com/example/feature/chat/MainScreen.kt'
with open(path, 'r') as f:
    content = f.read()

# 1. Drawer shape
content = content.replace("drawerShape = RoundedCornerShape(0.dp)", "drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp)")

# 2. Section Titles Typography
content = content.replace(
    'style = MaterialTheme.typography.labelMedium,\n                        color = MaterialTheme.colorScheme.onSurfaceVariant,\n                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)',
    'style = MaterialTheme.typography.titleSmall,\n                        fontWeight = FontWeight.Bold,\n                        color = MaterialTheme.colorScheme.primary,\n                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)'
)

# 3. Model item icons
model_row_pattern = r"""Row\(\s*modifier = Modifier\s*\.fillMaxWidth\(\)\s*\.padding\(horizontal = 16\.dp, vertical = 4\.dp\)\s*\.clip\(RoundedCornerShape\(12\.dp\)\)\s*\.background\(if \(isSelected\) Color\(0xFFE9D5FF\)\.copy\(alpha = 0\.5f\) else Color\.Transparent\)\s*\.clickable\(enabled = isUnlocked\) \{\s*onSelectModel\(model\)\s*onCloseDrawer\(\)\s*\}\s*\.padding\(horizontal = 16\.dp, vertical = 12\.dp\),\s*verticalAlignment = Alignment\.CenterVertically\s*\)\s*\{"""
model_row_replacement = """Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(100.dp)) // Pill shape like NavigationDrawerItem
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable(enabled = isUnlocked) {
                                    onSelectModel(model)
                                    onCloseDrawer()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.core.ui.icon.ProviderIcon(provider = model.provider, modifier = Modifier.size(24.dp).padding(end = 4.dp))
                            Spacer(modifier = Modifier.width(12.dp))"""
content = re.sub(model_row_pattern, model_row_replacement, content)

content = content.replace('color = if (isSelected) Color.Black else if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),', 
                          'color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),')

content = content.replace('tint = Color(0xFF1967D2)', 'tint = MaterialTheme.colorScheme.primary')

with open(path, 'w') as f:
    f.write(content)

print("Updated drawer design")
