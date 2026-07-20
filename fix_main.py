with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

import re

new_imports = """
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
"""

text = text.replace('import androidx.lifecycle.viewModelScope', new_imports + '\nimport androidx.lifecycle.viewModelScope')

permission_launcher = """
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Handle response
    }
"""

text = text.replace('override fun onCreate(savedInstanceState: Bundle?) {', permission_launcher + '\n    override fun onCreate(savedInstanceState: Bundle?) {')

check_permission = """
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
"""

text = text.replace('enableEdgeToEdge()', 'enableEdgeToEdge()\n' + check_permission)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
