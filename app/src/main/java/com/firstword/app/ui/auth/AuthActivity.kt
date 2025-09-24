package com.firstword.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firstword.app.MainActivity
import com.firstword.app.R
import com.firstword.app.databinding.ActivityAuthBinding
import com.firstword.app.models.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firestore: FirebaseFirestore

    private var isPasswordSignIn = false
    private var isEmailAuthVisible = false

    companion object {
        private const val RC_SIGN_IN = 9001
        private const val TAG = "AuthActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeFirebase()
        setupClickListeners()
        checkEmailLinkIntent(intent)
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        // Main email button - shows email auth card
        binding.btnSignInEmail.setOnClickListener {
            showEmailAuthCard()
        }

        // Email authentication card buttons
        binding.btnEmailAction.setOnClickListener {
            handleEmailAction()
        }

        binding.btnToggleAuthMode.setOnClickListener {
            toggleAuthMode()
        }

        binding.btnCreateAccount.setOnClickListener {
            handleCreateAccount()
        }

        binding.btnBack.setOnClickListener {
            hideEmailAuthCard()
        }

        // Other auth methods
        binding.btnSignInGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnSignInPhone.setOnClickListener {
            Toast.makeText(this, "Phone authentication coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnContinueGuest.setOnClickListener {
            continueAsGuest()
        }
    }

    private fun showEmailAuthCard() {
        isEmailAuthVisible = true
        binding.cardEmailAuth.visibility = android.view.View.VISIBLE
        binding.authButtonsContainer.visibility = android.view.View.GONE
        binding.title.visibility = android.view.View.GONE
        binding.subtitle.visibility = android.view.View.GONE
        binding.logo.visibility = android.view.View.GONE
    }

    private fun hideEmailAuthCard() {
        isEmailAuthVisible = false
        binding.cardEmailAuth.visibility = android.view.View.GONE
        binding.authButtonsContainer.visibility = android.view.View.VISIBLE
        binding.title.visibility = android.view.View.VISIBLE
        binding.subtitle.visibility = android.view.View.VISIBLE
        binding.logo.visibility = android.view.View.VISIBLE

        // Clear inputs
        binding.etEmail.text?.clear()
        binding.etPassword.text?.clear()
        binding.emailLayout.error = null
        binding.passwordLayout.error = null
    }

    private fun toggleAuthMode() {
        isPasswordSignIn = !isPasswordSignIn

        if (isPasswordSignIn) {
            binding.passwordLayout.visibility = android.view.View.VISIBLE
            binding.btnEmailAction.text = "Sign In"
            binding.btnToggleAuthMode.text = "Use Magic Link Instead"
            binding.btnCreateAccount.text = "Create Account"
        } else {
            binding.passwordLayout.visibility = android.view.View.GONE
            binding.btnEmailAction.text = "Send Magic Link"
            binding.btnToggleAuthMode.text = "Sign in with Password"
            binding.btnCreateAccount.text = "Create Account with Magic Link"
        }
    }

    private fun handleEmailAction() {
        val email = binding.etEmail.text.toString().trim()
        if (!validateEmail(email)) return

        if (isPasswordSignIn) {
            val password = binding.etPassword.text.toString()
            if (!validatePassword(password)) return
            signInWithEmailPassword(email, password)
        } else {
            sendMagicLink(email)
        }
    }

    private fun handleCreateAccount() {
        val email = binding.etEmail.text.toString().trim()
        if (!validateEmail(email)) return

        if (isPasswordSignIn) {
            val password = binding.etPassword.text.toString()
            if (!validatePassword(password)) return
            createAccountWithEmailPassword(email, password)
        } else {
            createAccountWithMagicLink(email)
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.emailLayout.error = "Email is required"
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.emailLayout.error = "Please enter a valid email"
                false
            }
            else -> {
                binding.emailLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                binding.passwordLayout.error = "Password is required"
                false
            }
            password.length < 6 -> {
                binding.passwordLayout.error = "Password must be at least 6 characters"
                false
            }
            else -> {
                binding.passwordLayout.error = null
                true
            }
        }
    }

    // Magic Link Authentication
    private fun sendMagicLink(email: String) {
        binding.btnEmailAction.isEnabled = false

        val actionCodeSettings = ActionCodeSettings.newBuilder()
            .setUrl("https://firstword.page.link/emailSignIn")
            .setHandleCodeInApp(true)
            .setAndroidPackageName(
                "com.firstword.app",
                true,
                null
            )
            .build()

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
            .addOnCompleteListener { task ->
                binding.btnEmailAction.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "Magic link sent successfully")
                    val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
                    prefs.edit().putString("email_for_sign_in", email).apply()

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Check Your Email")
                        .setMessage("We've sent a magic link to $email. Click the link to sign in.")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                            hideEmailAuthCard()
                        }
                        .show()
                } else {
                    Log.w(TAG, "Failed to send magic link", task.exception)
                    Toast.makeText(this, "Failed to send magic link: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun createAccountWithMagicLink(email: String) {
        sendMagicLink(email)
    }

    // Email/Password Authentication
    private fun createAccountWithEmailPassword(email: String, password: String) {
        binding.btnCreateAccount.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnCreateAccount.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.let { createUserProfile(it, "email") }
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this, "Account creation failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun signInWithEmailPassword(email: String, password: String) {
        binding.btnEmailAction.isEnabled = false

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.btnEmailAction.isEnabled = true

                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    user?.let { createUserProfile(it, "email") }
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Handle magic link when app is opened from email
    private fun checkEmailLinkIntent(intent: Intent) {
        val emailLink = intent.data?.toString()

        if (emailLink != null) {
            val prefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
            val email = prefs.getString("email_for_sign_in", null)

            if (email != null) {
                auth.signInWithEmailLink(email, emailLink)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(TAG, "signInWithEmailLink:success")
                            val user = auth.currentUser
                            user?.let { createUserProfile(it, "email") }
                            prefs.edit().remove("email_for_sign_in").apply()
                        } else {
                            Log.w(TAG, "signInWithEmailLink:failure", task.exception)
                            Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { checkEmailLinkIntent(it) }
    }

    // Keep your existing Google and Guest authentication methods...
    private fun signInWithGoogle() {
        binding.btnSignInGoogle.isEnabled = false
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { firebaseAuthWithGoogle(it) }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google sign in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                binding.btnSignInGoogle.isEnabled = true
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let { createUserProfile(it, "google") }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                    binding.btnSignInGoogle.isEnabled = true
                }
            }
    }

    private fun continueAsGuest() {
        binding.btnContinueGuest.isEnabled = false
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInAnonymously:success")
                    val user = auth.currentUser
                    user?.let { createGuestProfile(it) }
                } else {
                    Log.w(TAG, "signInAnonymously:failure", task.exception)
                    Toast.makeText(this, "Guest sign in failed", Toast.LENGTH_SHORT).show()
                    binding.btnContinueGuest.isEnabled = true
                }
            }
    }

    private fun createUserProfile(firebaseUser: com.google.firebase.auth.FirebaseUser, authMethod: String) {
        val user = User(
            id = firebaseUser.uid,
            handle = generateHandle(firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "user"),
            displayName = firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "User",
            email = firebaseUser.email ?: "",
            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
            authMethods = listOf(authMethod),
            isGuest = false
        )

        saveUserToFirestore(user)
    }

    private fun createGuestProfile(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val user = User(
            id = firebaseUser.uid,
            handle = "guest_${firebaseUser.uid.take(8)}",
            displayName = "Guest User",
            isGuest = true
        )

        saveUserToFirestore(user)
    }

    private fun saveUserToFirestore(user: User) {
        firestore.collection("users").document(user.id)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User profile created successfully")
                navigateToMain()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error creating user profile", e)
                navigateToMain()
            }
    }

    private fun generateHandle(baseName: String): String {
        val baseHandle = baseName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
        return "${baseHandle}_${System.currentTimeMillis().toString().takeLast(4)}"
    }

    // In AuthActivity.kt, add this to your navigateToMain() method:
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)

        // Check if we should return to a specific action
        val returnToFeed = intent.getBooleanExtra("return_to_feed", false)
        val action = intent.getStringExtra("action")
        val postId = intent.getStringExtra("post_id")

        if (returnToFeed && action != null && postId != null) {
            // You can pass this data back to the feed
            intent.putExtra("pending_action", action)
            intent.putExtra("pending_post_id", postId)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}