import re

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "r") as f:
    content = f.read()

# Add KeyboardActions import
import_stmt = "import androidx.compose.foundation.text.KeyboardOptions\nimport androidx.compose.foundation.text.KeyboardActions\n"
content = content.replace("import androidx.compose.foundation.text.KeyboardOptions\n", import_stmt)

# Fix webClientId
# let's add it right before it's used
old_gso = "val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)\n                                            .requestIdToken(webClientId)"
new_gso = "val webClientId = context.getString(R.string.default_web_client_id)\n                                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)\n                                            .requestIdToken(webClientId)"
content = content.replace(old_gso, new_gso)

# Fix registerUser calls
content = content.replace('settingsViewModel.registerUser(emailInput, nameInput.trim(), "email_password", passwordInput)', 'settingsViewModel.registerUser(emailInput, nameInput.trim(), passwordInput)')
content = content.replace('settingsViewModel.registerUser(emailInput, displayName, "email_password", passwordInput)', 'settingsViewModel.registerUser(emailInput, displayName, passwordInput)')

with open("app/src/main/java/com/example/feature/auth/LoginScreen.kt", "w") as f:
    f.write(content)
