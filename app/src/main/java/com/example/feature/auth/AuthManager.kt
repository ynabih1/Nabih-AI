package com.example.feature.auth

import com.example.R

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.microsoft.identity.client.exception.MsalClientException

object AuthManager {
    private const val TAG = "AuthManager"
    
    private var googleSignInClient: GoogleSignInClient? = null
    private var msalSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var isMsalInitializing = false

    /**
     * Retrieves the configured Google Sign-In Client.
     */
    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        if (googleSignInClient == null) {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                // requestIdToken is optional but recommended. If configured in the environment, we can pass it here.
                .build()
            googleSignInClient = GoogleSignIn.getClient(context.applicationContext, gso)
        }
        return googleSignInClient!!
    }

    /**
     * Initializes the MSAL single account public client application.
     */
    fun initMsal(context: Context, onInitComplete: (ISingleAccountPublicClientApplication?) -> Unit = {}) {
        if (msalSingleAccountApp != null) {
            onInitComplete(msalSingleAccountApp)
            return
        }
        if (isMsalInitializing) return
        isMsalInitializing = true

        try {
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context.applicationContext,
                R.raw.msal_config,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        Log.i(TAG, "MSAL Single Account Application created successfully.")
                        msalSingleAccountApp = application
                        isMsalInitializing = false
                        onInitComplete(application)
                    }

                    override fun onError(exception: MsalException) {
                        Log.e(TAG, "MSAL Initialization failed: ${exception.message}", exception)
                        isMsalInitializing = false
                        onInitComplete(null)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Exception during MSAL Initialization", e)
            isMsalInitializing = false
            onInitComplete(null)
        }
    }

    /**
     * Performs interactive Microsoft Authentication using MSAL.
     */
    fun signInWithMicrosoft(
        activity: Activity,
        callback: AuthenticationCallback
    ) {
        val app = msalSingleAccountApp
        if (app == null) {
            initMsal(activity) { initializedApp ->
                if (initializedApp != null) {
                    initializedApp.signIn(activity, null, arrayOf("user.read"), callback)
                } else {
                    callback.onError(MsalClientException("MSAL_NOT_INITIALIZED", "Microsoft authentication library is not initialized."))
                }
            }
            return
        }
        app.signIn(activity, null, arrayOf("user.read"), callback)
    }

    /**
     * Signs out from Google Sign-In Client.
     */
    fun signOutGoogle(context: Context, onComplete: () -> Unit = {}) {
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
            Log.i(TAG, "Google Sign-Out completed.")
            onComplete()
        }
    }

    /**
     * Signs out from Microsoft Account (MSAL).
     */
    fun signOutMicrosoft(onComplete: () -> Unit = {}) {
        val app = msalSingleAccountApp
        if (app != null) {
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    Log.i(TAG, "Microsoft Sign-Out completed successfully.")
                    onComplete()
                }

                override fun onError(exception: MsalException) {
                    Log.e(TAG, "Microsoft Sign-Out failed: ${exception.message}", exception)
                    onComplete() // Complete anyway to clear local state
                }
            })
        } else {
            onComplete()
        }
    }

    /**
     * Structural Identity Token Validation (standard JWT three-part format checks).
     */
    fun isTokenStructurallyValid(token: String?): Boolean {
        if (token.isNullOrBlank()) return false
        val parts = token.split(".")
        return parts.size == 3 && parts.all { it.isNotBlank() }
    }
}
