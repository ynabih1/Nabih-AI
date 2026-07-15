import re

with open('app/src/main/java/com/example/feature/auth/LoginScreen.kt', 'r') as f:
    content = f.read()

# Replace imports
content = content.replace("import com.google.android.gms.auth.api.signin.GoogleSignIn\n", "")
content = content.replace("import com.google.android.gms.auth.api.signin.GoogleSignInOptions\n", "")
content = content.replace("import com.google.android.gms.common.api.ApiException\n", "")
content = content.replace("import androidx.activity.result.contract.ActivityResultContracts\n", 
"""import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID
""")

# Replace googleSignInLauncher completely
old_launcher = """    // Google Sign-In Error state
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Google Sign-In callback received. Result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val email = account?.email ?: ""
                val name = account?.displayName ?: ""
                android.util.Log.d("LoginScreen", "Google Sign-In successful. Email: $email, Name: $name")
                if (email.isNotEmpty()) {
                    scope.launch {
                        isLoading = true
                        var registeredUser = settingsViewModel.getUserByEmail(email)
                        if (registeredUser == null) {
                            android.util.Log.d("LoginScreen", "User not found locally. Registering implicitly for OAuth.")
                            val defaultName = if (name.isNotBlank()) name else email.substringBefore("@")
                            settingsViewModel.registerUser(email, defaultName, "oauth_google")
                            registeredUser = settingsViewModel.getUserByEmail(email)
                        } else {
                            android.util.Log.d("LoginScreen", "User found locally. Proceeding to login.")
                        }
                        
                        isLoading = false
                        if (registeredUser != null) {
                            settingsViewModel.updateLoginState(true, "GOOGLE", email, name.ifBlank { registeredUser.name })
                            Toast.makeText(context, if (isArabic) "مرحباً بك ${registeredUser.name}" else "Welcome, ${registeredUser.name}!", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        } else {
                            android.util.Log.e("LoginScreen", "Failed to retrieve or register user locally after Google Sign-In.")
                            val msg = if (isArabic) {
                                "الحساب غير موجود. يرجى إنشاء حساب أولاً."
                            } else {
                                "Account not found. Please create an account first."
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    android.util.Log.w("LoginScreen", "Google Sign-In succeeded but email was empty.")
                    Toast.makeText(context, if (isArabic) "فشل الحصول على معلومات حساب Google." else "Failed to retrieve Google account info.", Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginScreen", "Google Sign-In failed with ApiException. Code: ${e.statusCode}, Message: ${e.message}", e)
                val errorMsg = if (isArabic) {
                    "فشل تسجيل الدخول بـ Google (كود: ${e.statusCode})"
                } else {
                    "Google Sign-In failed (code: ${e.statusCode})"
                }
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.w("LoginScreen", "Google Sign-In failed or was cancelled. Result code: ${result.resultCode}")
            val errorMsg = if (isArabic) {
                "تم إلغاء تسجيل الدخول بـ Google."
            } else {
                "Google Sign-In was cancelled."
            }
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        }
    }"""
    
new_launcher = """    // Google Sign-In with CredentialManager
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
    
content = content.replace(old_launcher, new_launcher)

# Replace the onClick handler
old_onclick = """                                        val clientId = com.example.BuildConfig.GOOGLE_CLIENT_ID
                                        val isClientIdMissing = clientId.isEmpty() || clientId.contains("YOUR_GOOGLE") || clientId.contains("placeholder")
                                        if (isClientIdMissing) {
                                            val missingMsg = if (isArabic) {
                                                "معرّف عميل Google غير مضبوط في الإعدادات."
                                            } else {
                                                "Google Client ID is missing or not configured."
                                            }
                                            Toast.makeText(context, missingMsg, Toast.LENGTH_LONG).show()
                                        } else {
                                            if (activity != null) {
                                                isLoading = true
                                                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                                    .requestEmail()
                                                    .requestIdToken(clientId)
                                                    .build()
                                                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                                                
                                                googleSignInClient.signOut().addOnCompleteListener {
                                                    googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                                    isLoading = false
                                                }
                                            } else {
                                                Toast.makeText(context, "Activity is not available.", Toast.LENGTH_SHORT).show()
                                            }
                                        }"""
                                        
new_onclick = """                                        val clientId = com.example.BuildConfig.GOOGLE_CLIENT_ID
                                        val isClientIdMissing = clientId.isEmpty() || clientId.contains("YOUR_GOOGLE") || clientId.contains("placeholder")
                                        if (isClientIdMissing) {
                                            Toast.makeText(context, if (isArabic) "معرّف عميل Google غير مضبوط في الإعدادات." else "Google Client ID is missing or not configured.", Toast.LENGTH_LONG).show()
                                        } else {
                                            scope.launch {
                                                isLoading = true
                                                try {
                                                    val rawNonce = UUID.randomUUID().toString()
                                                    val bytes = rawNonce.toByteArray()
                                                    val md = MessageDigest.getInstance("SHA-256")
                                                    val digest = md.digest(bytes)
                                                    val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
                                                    
                                                    val googleIdOption = GetGoogleIdOption.Builder()
                                                        .setFilterByAuthorizedAccounts(false)
                                                        .setServerClientId(clientId)
                                                        .setNonce(hashedNonce)
                                                        .build()
                                                        
                                                    val request = GetCredentialRequest.Builder()
                                                        .addCredentialOption(googleIdOption)
                                                        .build()
                                                        
                                                    val result = credentialManager.getCredential(
                                                        request = request,
                                                        context = context,
                                                    )
                                                    
                                                    val credential = result.credential
                                                    if (credential is androidx.credentials.CustomCredential &&
                                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                                        val email = googleIdTokenCredential.id
                                                        val name = googleIdTokenCredential.displayName ?: ""
                                                        handleGoogleSignIn(email, name)
                                                    } else {
                                                        android.util.Log.w("LoginScreen", "Received unexpected credential type")
                                                        isLoading = false
                                                    }
                                                } catch (e: GetCredentialException) {
                                                    android.util.Log.e("LoginScreen", "GetCredentialException: ${e.message}")
                                                    Toast.makeText(context, if (isArabic) "تم الإلغاء أو فشل الاتصال." else "Google Sign-In failed or cancelled.", Toast.LENGTH_SHORT).show()
                                                    isLoading = false
                                                } catch (e: Exception) {
                                                    android.util.Log.e("LoginScreen", "Google Sign In Error", e)
                                                    isLoading = false
                                                }
                                            }
                                        }"""

content = content.replace(old_onclick, new_onclick)

with open('app/src/main/java/com/example/feature/auth/LoginScreen.kt', 'w') as f:
    f.write(content)
print("LoginScreen patched")
