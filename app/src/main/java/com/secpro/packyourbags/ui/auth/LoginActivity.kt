package com.secpro.packyourbags.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.secpro.packyourbags.MainActivity
import com.secpro.packyourbags.R
import com.secpro.packyourbags.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityLoginBinding
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var emailET: TextInputEditText
    private lateinit var passwordET: TextInputEditText
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>
    
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        
        // Configure Google Sign In
        configureGoogleSignIn()

        // Initialize views
        emailLayout = binding.textInputLayoutEmail
        passwordLayout = binding.textInputLayoutPassword
        emailET = binding.editTextEmail
        passwordET = binding.editTextPassword

        // Check if user is already logged in
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        binding.buttonLogin.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }

        binding.buttonSignup.setOnClickListener {
            if (validateInputs()) {
                signupUser()
            }
        }
        
        binding.buttonGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }
        
        // Register for Google Sign-In result
        googleSignInLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    // Google Sign In failed
                    Log.w(TAG, "Google sign in failed", e)
                    Snackbar.make(
                        binding.root,
                        "Google sign in failed: ${e.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            } else {
                showLoading(false)
            }
        }
    }
    
    private fun configureGoogleSignIn() {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }
    
    private fun signInWithGoogle() {
        showLoading(true)
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }
    
    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle: ${account.id}")
        
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithCredential:success")
                    startMainActivity()
                } else {
                    // Sign in failed
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Snackbar.make(
                        binding.root,
                        "Authentication failed: ${task.exception?.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                }
            }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val email = emailET.text.toString().trim()
        val password = passwordET.text.toString().trim()

        if (email.isEmpty()) {
            emailLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Please enter a valid email"
            isValid = false
        } else {
            emailLayout.error = null
        }

        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            passwordLayout.error = null
        }

        return isValid
    }

    private fun loginUser() {
        showLoading(true)
        val email = emailET.text.toString().trim()
        val password = passwordET.text.toString().trim()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    binding.textViewError.text = task.exception?.message ?: "Login failed"
                    binding.textViewError.visibility = View.VISIBLE
                }
            }
    }

    private fun signupUser() {
        showLoading(true)
        val email = emailET.text.toString().trim()
        val password = passwordET.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    binding.textViewError.text = task.exception?.message ?: "Signup failed"
                    binding.textViewError.visibility = View.VISIBLE
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.buttonLogin.isEnabled = !show
        binding.buttonSignup.isEnabled = !show
        binding.buttonGoogleSignIn.isEnabled = !show
    }
}
