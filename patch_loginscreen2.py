import re

with open('app/src/main/java/com/example/feature/auth/LoginScreen.kt', 'r') as f:
    content = f.read()

# Let's replace googleSignInLauncher using a regex
# The regex will match from 'val googleSignInLauncher =' to '    // End of Google Sign-In Error state' if I had added it.
# Let's just find 'val googleSignInLauncher = rememberLauncherForActivityResult' and replace it up to its end.

start_idx = content.find("val googleSignInLauncher = rememberLauncherForActivityResult")
if start_idx != -1:
    # Find the matching closing brace. 
    # rememberLauncherForActivityResult takes a trailing lambda, ending at level 1 brace.
    open_braces = 0
    in_block = False
    end_idx = start_idx
    for i in range(start_idx, len(content)):
        if content[i] == '{':
            open_braces += 1
            in_block = True
        elif content[i] == '}':
            open_braces -= 1
        
        if in_block and open_braces == 0:
            end_idx = i + 1
            break
            
    print(f"Replacing googleSignInLauncher from {start_idx} to {end_idx}")
    
    new_launcher = """// Google Sign-In with CredentialManager
    val credentialManager = remember { CredentialManager.create(context) }
    
    fun handleGoogleSignIn(email: String, name: String) {
        scope.launch {
            isLoading = true
            var registeredUser = settingsViewModel.getUserByEmail(email)
            if (registeredUser == null) {
                android.util.Log.d("LoginScreen", "User not found locally. Registering implicitly for OAuth.")
                val defaultName = if (name.isNotBlank()) name else email.substringBefore("@")
                settingsViewModel.registerUser(email, defaultName, "oauth_google")
                registeredUser = settingsViewModel.getUserByEmail(email)
            }
            isLoading = false
            if (registeredUser != null) {
                settingsViewModel.updateLoginState(true, "GOOGLE", email, name.ifBlank { registeredUser.name })
                Toast.makeText(context, if (isArabic) "مرحباً بك ${registeredUser.name}" else "Welcome, ${registeredUser.name}!", Toast.LENGTH_SHORT).show()
                onLoginSuccess()
            } else {
                Toast.makeText(context, if (isArabic) "الحساب غير موجود." else "Account not found.", Toast.LENGTH_LONG).show()
            }
        }
    }"""
    
    content = content[:start_idx] + new_launcher + content[end_idx:]
else:
    print("googleSignInLauncher not found")
    
with open('app/src/main/java/com/example/feature/auth/LoginScreen.kt', 'w') as f:
    f.write(content)
print("LoginScreen patched again")
