package com.secpro.packyourbags

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.secpro.packyourbags.api.ShuttleAIService

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize views
        apiKeyEditText = findViewById(R.id.apiKeyEditText)
        saveButton = findViewById(R.id.saveButton)
        toolbar = findViewById(R.id.toolbar)

        // Setup toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Settings"
        
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Load saved API key
        val savedApiKey = ShuttleAIService.getApiKey(this)
        apiKeyEditText.setText(savedApiKey)

        // Setup save button
        saveButton.setOnClickListener {
            val apiKey = apiKeyEditText.text.toString().trim()
            
            if (apiKey.isBlank()) {
                Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Save API key
            ShuttleAIService.saveApiKey(this, apiKey)
            Toast.makeText(this, "API key saved successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
} 