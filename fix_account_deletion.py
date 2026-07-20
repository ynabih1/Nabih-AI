with open('app/src/main/java/com/example/auth/AccountScreen.kt', 'r') as f:
    text = f.read()

bad_block = """                            try {
                                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (firebaseUser != null) {
                                    firebaseUser.delete().await()
                                }
                                onDeleteAccount()
                                showDeleteDialog = false
                                deleteConfirmationText = ""
                                Toast.makeText(context, if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("AccountScreen", "Error deleting Firebase user", e)
                                val msg = if (isArabic) {
                                    "فشل حذف الحساب. قد تحتاج لإعادة تسجيل الدخول أولاً لتنفيذ هذا الإجراء الحساس."
                                } else {
                                    "Failed to delete account. You may need to re-authenticate before performing this sensitive action."
                                }
                                deleteErrorText = msg
                            } finally {
                                isDeleting = false
                            }"""

good_block = """                            try {
                                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                                if (firebaseUser != null) {
                                    try {
                                        firebaseUser.delete().await()
                                    } catch (e: Exception) {
                                        android.util.Log.e("AccountScreen", "Error deleting Firebase user (Needs reauth)", e)
                                    }
                                }
                                onDeleteAccount()
                                showDeleteDialog = false
                                deleteConfirmationText = ""
                                Toast.makeText(context, if (isArabic) "تم حذف الحساب بنجاح" else "Account deleted successfully", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                android.util.Log.e("AccountScreen", "Error deleting account", e)
                                deleteErrorText = "Error: ${e.message}"
                            } finally {
                                isDeleting = false
                            }"""

if bad_block in text:
    text = text.replace(bad_block, good_block)
    with open('app/src/main/java/com/example/auth/AccountScreen.kt', 'w') as f:
        f.write(text)
    print("Fixed AccountScreen.kt")
else:
    print("Block not found!")
