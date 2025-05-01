package com.secpro.packyourbags.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

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
    }
}
