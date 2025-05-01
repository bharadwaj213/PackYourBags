package com.secpro.packyourbags

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutUs : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Retrieve views
        val developerNameTextView: TextView = findViewById(R.id.developerNameTextView)
        val developerBioTextView: TextView = findViewById(R.id.developerBioTextView)
        val skillsLayout: LinearLayout = findViewById(R.id.skillsLayout)
        val socialMediaLayout: LinearLayout = findViewById(R.id.socialMediaLayout)

        developerNameTextView.text = "Sai" // ✅ Correct
        developerBioTextView.text = "Sai Bharadwaj" // ✅ Correct


        val skills = listOf("C++", "Data Structure & Algorithm", "Core Java", "Android Development", "XML")
        skills.forEach { skill ->
            val skillTextView = TextView(this)
            skillTextView.text = skill
            skillsLayout.addView(skillTextView)
        }

        val socialMediaLinks = listOf(
            "https://www.linkedin.com/in/bharadwaj213(LinkedIn)",
            "https://github.com/bharadwaj213/packyourbags (GitHub)"
        )
        socialMediaLinks.forEach { link ->
            val linkTextView = TextView(this)
            linkTextView.text = link
            socialMediaLayout.addView(linkTextView)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
