package com.secpro.packyourbags

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.secpro.packyourbags.Adapter.MyAdapter
import com.secpro.packyourbags.Constants.MyConstants
import com.secpro.packyourbags.databinding.ActivityMainBinding
import com.secpro.packyourbags.ui.auth.LoginActivity

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val TIME_INTERVAL = 2000L
    private var mBackPressed: Long = 0
    private lateinit var recyclerView: RecyclerView
    private lateinit var titles: MutableList<String>
    private lateinit var images: MutableList<Int>
    private lateinit var adapter: MyAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        drawerLayout = binding.drawerLayout
        navView = binding.navView

        // Setup navigation drawer toggle
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.open, R.string.close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Setup navigation view
        navView.setNavigationItemSelectedListener(this)

        // Update navigation header with user info
        updateNavigationHeader()

        // Setup recycler view
        recyclerView = binding.recyclerView
        addAllTitles()
        addAllImages()

        adapter = MyAdapter(this, titles, images, this)
        val gridLayoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = gridLayoutManager
        recyclerView.adapter = adapter
    }

    private fun updateNavigationHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val textViewName = headerView.findViewById<android.widget.TextView>(R.id.textViewName)
        val textViewEmail = headerView.findViewById<android.widget.TextView>(R.id.textViewEmail)

        val user = auth.currentUser
        user?.let {
            textViewName.text = it.displayName ?: "User"
            textViewEmail.text = it.email
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                Toast.makeText(this, "Profile Selected", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                auth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed()
        } else {
            Toast.makeText(this, "Tap back button in order to exit", Toast.LENGTH_SHORT).show()
            mBackPressed = System.currentTimeMillis()
            }
        }
    }

    private fun addAllImages() {
        images = mutableListOf(
            R.drawable.basic_needs, R.drawable.cloths, R.drawable.person_care,
            R.drawable.baby_needs, R.drawable.health, R.drawable.technology,
            R.drawable.food, R.drawable.beach, R.drawable.car,
            R.drawable.need, R.drawable.mylist, R.drawable.selection
        )
    }

    private fun addAllTitles() {
        titles = mutableListOf(
            MyConstants.BASIC_NEEDS_CAMEL_CASE, MyConstants.CLOTHING_CAMEL_CASE,
            MyConstants.PERSONAL_CARE_CAMEL_CASE, MyConstants.BABY_NEEDS_CAMEL_CASE,
            MyConstants.HEALTH_CAMEL_CASE, MyConstants.TECHNOLOGY_CAMEL_CASE,
            MyConstants.FOOD_CAMEL_CASE, MyConstants.BEACH_SUPPLIES_CAMEL_CASE,
            MyConstants.CAR_SUPPLIES_CAMEL_CASE, MyConstants.NEEDS_CAMEL_CASE,
            MyConstants.MY_LIST_CAMEL_CASE, MyConstants.MY_SELECTIONS_CAMEL_CASE
        )
    }
}
