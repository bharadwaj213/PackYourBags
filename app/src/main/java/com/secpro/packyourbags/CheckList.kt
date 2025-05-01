package com.secpro.packyourbags

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.secpro.packyourbags.Adapter.CheckListAdapter
import com.secpro.packyourbags.Constants.MyConstants
import com.secpro.packyourbags.Model.Items
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView as MaterialSearchView
import androidx.recyclerview.widget.DefaultItemAnimator

class CheckList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var checkListAdapter: CheckListAdapter
    private lateinit var btnAdd: FloatingActionButton
    private lateinit var txtEmptyList: TextView
    private lateinit var progressIndicator: LinearProgressIndicator
    private lateinit var txtTitle: TextView
    private lateinit var searchView: SearchView
    private var itemsList = mutableListOf<Items>()
    private var show: String? = null
    private var isFirstLoad = true
    private var isFetching = false

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_list)

        try {
            // Initialize Firebase
            FirebaseApp.initializeApp(this)

            // Initialize views with null checks
            recyclerView = findViewById(R.id.recyclerView) ?: throw IllegalStateException("RecyclerView not found")
            btnAdd = findViewById(R.id.btnAdd) ?: throw IllegalStateException("FloatingActionButton not found")
            txtEmptyList = findViewById(R.id.txtEmptyList) ?: throw IllegalStateException("txtEmptyList not found")
            progressIndicator = findViewById(R.id.progressIndicator) ?: throw IllegalStateException("progressIndicator not found")
            txtTitle = findViewById(R.id.txtTitle) ?: throw IllegalStateException("Title TextView not found")
            searchView = findViewById(R.id.searchView) ?: throw IllegalStateException("SearchView not found")

            // Get category from intent
            show = intent.getStringExtra(MyConstants.HEADER_SMALL)
            if (show == null) {
                android.util.Log.e("CheckList", "No category provided in intent")
                finish()
                return
            }

            // Set title
            txtTitle.text = show

            // Set up RecyclerView
            recyclerView.setHasFixedSize(true)
            recyclerView.layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)
            recyclerView.itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
            recyclerView.visibility = View.VISIBLE // Ensure RecyclerView is visible

            // Initialize adapter with an empty list
            itemsList = mutableListOf()
            checkListAdapter = CheckListAdapter(this, itemsList, show!!)
            recyclerView.adapter = checkListAdapter
            
            // Log the initialization
            android.util.Log.d("CheckList", "Adapter initialized with empty list")
            android.util.Log.d("CheckList", "RecyclerView visibility: ${if (recyclerView.visibility == View.VISIBLE) "VISIBLE" else "NOT VISIBLE"}")

            // Set up search functionality
            searchView.queryHint = "Search items..."
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    filterItems(query)
                    searchView.clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    filterItems(newText)
                    return true
                }
            })

            // Set up add button
            btnAdd.setOnClickListener {
                showAddItemDialog()
            }

            // Fetch items only if not already fetched
            if (itemsList.isEmpty()) {
                fetchItems()
            }

        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error in onCreate: ${e.message}")
            Toast.makeText(this, "Error initializing checklist: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun filterItems(query: String?) {
        try {
            val filteredList = itemsList.filter {
                it.itemname.lowercase().contains(query?.lowercase() ?: "")
            }
            checkListAdapter.updateItems(filteredList.toMutableList())
        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error filtering items: ${e.message}")
            Toast.makeText(this, "Error filtering items", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddItemDialog() {
        try {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
            val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextItemName)
            
            val dialog = AlertDialog.Builder(this)
                .setTitle("Add New Item")
                .setView(dialogView)
                .setPositiveButton("Add") { _, _ ->
                    val itemName = editText.text.toString().trim()
                    if (itemName.isNotEmpty()) {
                        addItem(itemName)
                    } else {
                        Toast.makeText(this, "Please enter an item name", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            // Show keyboard when dialog opens
            dialog.setOnShowListener {
                editText.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }

            dialog.show()
        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error showing add item dialog: ${e.message}")
            Toast.makeText(this, "Error showing add item dialog", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addItem(itemName: String) {
        try {
            android.util.Log.d("CheckList", "Adding new item: $itemName for category: $show")
            
            // Create new item
            val newItem = Items(
                itemname = itemName,
                category = show!!,
                checked = false,
                addedby = user?.uid ?: ""
            )

            // Add to Firestore
            db.collection("users").document(user?.uid ?: "")
                .collection("items")
                .add(newItem)
                .addOnSuccessListener { documentReference ->
                    android.util.Log.d("CheckList", "Item added successfully with ID: ${documentReference.id}")
                    
                    // Update local list
                    itemsList.add(newItem)
                    android.util.Log.d("CheckList", "Current items list size: ${itemsList.size}")
                    
                    // Update adapter
                    checkListAdapter.updateItems(itemsList)
                    android.util.Log.d("CheckList", "Adapter updated with new item")
                    
                    // Show success message
                    Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CheckList", "Error adding item: ${e.message}")
                    Toast.makeText(this, "Error adding item: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error in addItem: ${e.message}")
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_one, menu)

        if (show == MyConstants.MY_SELECTIONS) {
            menu.getItem(0).isVisible = false
            menu.getItem(2).isVisible = false
            menu.getItem(3).isVisible = false
        } else if (show == MyConstants.MY_LIST_CAMEL_CASE) {
            menu.getItem(2).isVisible = false
        }

        val searchItem = menu.findItem(R.id.btnSearch)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = itemsList.filter {
                    it.itemname.lowercase().startsWith(newText ?: "")
                }
                updateRecycler(filteredList.toMutableList())
                return false
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btnMySelection -> {
                val intent = Intent(this, CheckList::class.java)
                intent.putExtra(MyConstants.HEADER_SMALL, MyConstants.MY_SELECTIONS)
                intent.putExtra(MyConstants.SHOW_SMALL, MyConstants.FALSE_STRING)
                startActivity(intent)
                true
            }
            R.id.btnCustomList -> {
                val intent = Intent(this, CheckList::class.java)
                intent.putExtra(MyConstants.HEADER_SMALL, MyConstants.MY_LIST_CAMEL_CASE)
                intent.putExtra(MyConstants.SHOW_SMALL, MyConstants.TRUE_STRING)
                startActivity(intent)
                true
            }
            R.id.btnAboutUs -> {
                startActivity(Intent(this, AboutUs::class.java))
                true
            }
            R.id.btnExit -> {
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getDefaultItemsForCategory(category: String): List<Items> {
        android.util.Log.d("CheckList", "Getting default items for category: $category")
        return when (category) {
            MyConstants.BASIC_NEEDS_CAMEL_CASE -> {
                android.util.Log.d("CheckList", "Found Basic Needs category")
                listOf(
                    Items(itemname = "Wallet", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "ID/Passport", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Keys", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Cash", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Credit/debit cards", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Travel documents", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Pen and notebook", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Travel itinerary", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Travel insurance information", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Snacks", category = MyConstants.BASIC_NEEDS_CAMEL_CASE, addedby = "system", checked = false)
                )
            }
            MyConstants.CLOTHING_CAMEL_CASE -> listOf(
                Items(itemname = "T-shirts", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Jeans/pants", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Shorts", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Underwear", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Socks", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Sweater/jacket", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Pajamas", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Hat/cap", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Belt", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Scarf", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Gloves", category = MyConstants.CLOTHING_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.PERSONAL_CARE_CAMEL_CASE -> listOf(
                Items(itemname = "Toothbrush", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Toothpaste", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Soap/body wash", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Shampoo/conditioner", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Deodorant", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Razor and shaving cream", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Hairbrush/comb", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Hair accessories", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Makeup", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Perfume/cologne", category = MyConstants.PERSONAL_CARE_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.BABY_NEEDS_CAMEL_CASE -> listOf(
                Items(itemname = "Diapers", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby wipes", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby clothing", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby food/formula", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Bottles and sippy cups", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Pacifiers", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby lotion", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby sunscreen", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Baby toys", category = MyConstants.BABY_NEEDS_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.HEALTH_CAMEL_CASE -> listOf(
                Items(itemname = "Prescribed medications", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Pain relievers", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Band-aids", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Insect repellent", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Sunscreen", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Hand sanitizer", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "First aid kit", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Prescription glasses/contact lenses", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Allergy medication", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Multivitamins", category = MyConstants.HEALTH_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.TECHNOLOGY_CAMEL_CASE -> listOf(
                Items(itemname = "Laptop", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Phone", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Charger", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Headphones", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Power Bank", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Camera", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Tablet", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "E-reader", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Portable speaker", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Travel adapter", category = MyConstants.TECHNOLOGY_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.FOOD_CAMEL_CASE -> listOf(
                Items(itemname = "Snacks", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Granola bars", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Instant noodles", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Canned goods", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Utensils (spoon, fork, knife)", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Reusable water bottle", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Non-perishable items", category = MyConstants.FOOD_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.BEACH_SUPPLIES_CAMEL_CASE -> listOf(
                Items(itemname = "Swimsuit", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Beach towel", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Sun hat", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Sunglasses", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Beach bag", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Sunscreen", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Beach toys", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Cooler with drinks and snacks", category = MyConstants.BEACH_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.CAR_SUPPLIES_CAMEL_CASE -> listOf(
                Items(itemname = "Driver's license", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Vehicle registration and insurance", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Car keys", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "GPS or maps", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Emergency car kit", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Travel pillows/blankets", category = MyConstants.CAR_SUPPLIES_CAMEL_CASE, addedby = "system", checked = false)
            )
            MyConstants.NEEDS_CAMEL_CASE -> listOf(
                Items(itemname = "Wallet", category = MyConstants.NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Tooth-paste", category = MyConstants.NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Car keys", category = MyConstants.NEEDS_CAMEL_CASE, addedby = "system", checked = false),
                Items(itemname = "Band-aids", category = MyConstants.NEEDS_CAMEL_CASE, addedby = "system", checked = false)
            )
            else -> {
                android.util.Log.d("CheckList", "No matching category found for: $category")
                emptyList()
            }
        }
    }

    private fun addDefaultItems() {
        val defaultItems = getDefaultItemsForCategory(show!!)
        android.util.Log.d("CheckList", "Default items for $show: ${defaultItems.size}")
        
        if (defaultItems.isEmpty()) {
            android.util.Log.d("CheckList", "No default items found for category: $show")
            updateRecycler(itemsList)
            return
        }

        // Log default items for debugging
        defaultItems.forEachIndexed { index, item ->
            android.util.Log.d("CheckList", "Default item $index: ${item.itemname} for category ${item.category}")
        }

        val batch = db.batch()
        user?.uid?.let { uid ->
            android.util.Log.d("CheckList", "Checking for existing items in category: $show")
            // First, check if any of these items already exist
            db.collection("users").document(uid)
                .collection("items")
                .whereEqualTo("category", show!!)
                .get()
                .addOnSuccessListener { existingDocs ->
                    android.util.Log.d("CheckList", "Found ${existingDocs.size()} existing items")
                    val existingItems = existingDocs.map { 
                        val item = it.toObject(Items::class.java)
                        android.util.Log.d("CheckList", "Existing item: ${item.itemname}")
                        item.itemname 
                    }.toSet()
                    
                    val itemsToAdd = defaultItems.filter { it.itemname !in existingItems }
                    
                    android.util.Log.d("CheckList", "Items to add: ${itemsToAdd.size}, Existing items: ${existingItems.size}")
                    
                    if (itemsToAdd.isEmpty()) {
                        android.util.Log.d("CheckList", "All default items already exist")
                        fetchItems() // Refresh the list
                        return@addOnSuccessListener
                    }

                    android.util.Log.d("CheckList", "Adding ${itemsToAdd.size} new default items")
                    val newBatch = db.batch() // Create a new batch to ensure it's empty
                    
                    itemsToAdd.forEach { item ->
                        android.util.Log.d("CheckList", "Adding item: ${item.itemname}")
                        val docRef = db.collection("users").document(uid)
                            .collection("items")
                            .document()
                        newBatch.set(docRef, item)
                    }
                    
                    newBatch.commit()
                        .addOnSuccessListener {
                            android.util.Log.d("CheckList", "Successfully added ${itemsToAdd.size} default items")
                            // Add items to our local list
                            itemsList.addAll(itemsToAdd)
                            android.util.Log.d("CheckList", "Updated local list with ${itemsToAdd.size} items, total now: ${itemsList.size}")
                            
                            // Update the recycler view with the combined list
                            updateRecycler(itemsList)
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CheckList", "Failed to add default items: ${e.message}")
                            android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                            Toast.makeText(this, "Failed to add default items", Toast.LENGTH_SHORT).show()
                            updateRecycler(itemsList)
                        }
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CheckList", "Error checking existing items: ${e.message}")
                    android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                    Toast.makeText(this, "Error checking existing items", Toast.LENGTH_SHORT).show()
                    progressIndicator.visibility = View.GONE
                    isFetching = false
                }
        } ?: run {
            // Handle case where user is null
            android.util.Log.e("CheckList", "User is null, cannot add default items")
            Toast.makeText(this, "Please log in to view items", Toast.LENGTH_SHORT).show()
            progressIndicator.visibility = View.GONE
            isFetching = false
        }
    }

    private fun initializeDefaultItemsForNewUser() {
        if (user == null) {
            android.util.Log.e("CheckList", "User is null, cannot initialize default items")
            return
        }

        // Check if user already has items
        db.collection("users").document(user.uid)
            .collection("items")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    android.util.Log.d("CheckList", "Initializing default items for new user")
                    
                    // Get all default items for each category
                    val allDefaultItems = mutableListOf<Items>()
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.BASIC_NEEDS_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.CLOTHING_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.PERSONAL_CARE_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.BABY_NEEDS_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.HEALTH_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.TECHNOLOGY_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.FOOD_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.BEACH_SUPPLIES_CAMEL_CASE))
                    allDefaultItems.addAll(getDefaultItemsForCategory(MyConstants.CAR_SUPPLIES_CAMEL_CASE))

                    android.util.Log.d("CheckList", "Total default items to add: ${allDefaultItems.size}")

                    // Add all default items to Firestore
                    val batch = db.batch()
                    allDefaultItems.forEach { item ->
                        val docRef = db.collection("users").document(user.uid)
                            .collection("items")
                            .document()
                        batch.set(docRef, item)
                    }

                    batch.commit()
                        .addOnSuccessListener {
                            android.util.Log.d("CheckList", "Successfully initialized default items for new user")
                            // After initialization, fetch items for current category
                            fetchItems()
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CheckList", "Failed to initialize default items: ${e.message}")
                            Toast.makeText(this, "Failed to initialize default items", Toast.LENGTH_SHORT).show()
                            fetchItems() // Still try to fetch items even if initialization failed
                        }
                } else {
                    android.util.Log.d("CheckList", "User already has items, skipping initialization")
                    fetchItems()
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CheckList", "Error checking user items: ${e.message}")
                Toast.makeText(this, "Error checking user items", Toast.LENGTH_SHORT).show()
                fetchItems() // Try to fetch items even if check failed
            }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("CheckList", "onResume called, isFirstLoad: $isFirstLoad")
        // Only fetch items if the list is empty and not already fetching
        if (itemsList.isEmpty() && !isFetching) {
            fetchItems()
        }
        isFirstLoad = false
    }

    override fun onPause() {
        super.onPause()
        // Clear any pending animations
        recyclerView.clearAnimation()
        // Save current state
        android.util.Log.d("CheckList", "onPause called, saving current state")
    }

    private fun fetchItems() {
        try {
            if (isFetching) {
                android.util.Log.d("CheckList", "Already fetching items, skipping")
                return
            }

            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) {
                android.util.Log.e("CheckList", "No user logged in")
                Toast.makeText(this, "Please log in to view items", Toast.LENGTH_SHORT).show()
                return
            }

            isFetching = true
            android.util.Log.d("CheckList", "User ID: ${user.uid}")
            progressIndicator.visibility = View.VISIBLE
            android.util.Log.d("CheckList", "Fetching items for category: $show")

            // Clear the current list before fetching new items
            itemsList.clear()
            checkListAdapter.updateItems(itemsList) // Clear the adapter immediately

            // ENHANCED LOGGING: Check if the show variable is null or empty
            if (show == null) {
                android.util.Log.e("CheckList", "Category is null, cannot fetch items")
                Toast.makeText(this, "Error: Category is null", Toast.LENGTH_SHORT).show()
                progressIndicator.visibility = View.GONE
                isFetching = false
                return
            }
            
            android.util.Log.d("CheckList", "Category value: '$show', Category type: ${show?.javaClass?.name}")

            if (show == MyConstants.MY_SELECTIONS) {
                // Fetch only checked items for My Selections
                db.collection("users").document(user.uid)
                    .collection("items")
                    .whereEqualTo("checked", true)
                    .get()
                    .addOnSuccessListener { documents ->
                        try {
                            android.util.Log.d("CheckList", "Successfully fetched ${documents.size()} checked items")
                            val seenItems = mutableSetOf<String>()
                            
                            for (doc in documents) {
                                val docData = doc.data
                                android.util.Log.d("CheckList", "Document data: $docData")
                                
                                val item = doc.toObject(Items::class.java)
                                android.util.Log.d("CheckList", "Found item: ${item.itemname} in category: ${item.category}, checked: ${item.checked}")
                                if (seenItems.add(item.itemname)) {
                                    itemsList.add(item)
                                    android.util.Log.d("CheckList", "Added checked item: ${item.itemname}")
                                }
                            }
                            android.util.Log.d("CheckList", "Total items after fetching: ${itemsList.size}")
                            updateRecycler(itemsList)
                        } catch (e: Exception) {
                            android.util.Log.e("CheckList", "Error processing checked items: ${e.message}")
                            android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                            Toast.makeText(this, "Error processing items", Toast.LENGTH_SHORT).show()
                        } finally {
                            progressIndicator.visibility = View.GONE
                            isFetching = false
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("CheckList", "Failed to fetch checked items: ${e.message}")
                        android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                        Toast.makeText(this, "Failed to fetch checked items: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateRecycler(mutableListOf())
                        progressIndicator.visibility = View.GONE
                        isFetching = false
                    }
            } else {
                // Fetch items for the current category
                android.util.Log.d("CheckList", "Querying Firestore for category: $show")
                db.collection("users").document(user.uid)
                    .collection("items")
                    .whereEqualTo("category", show)
                    .get()
                    .addOnSuccessListener { documents ->
                        try {
                            android.util.Log.d("CheckList", "Successfully fetched ${documents.size()} items for category: $show")
                            val seenItems = mutableSetOf<String>()
                            
                            // ENHANCED LOGGING: Log all documents received
                            for (doc in documents) {
                                val docData = doc.data
                                android.util.Log.d("CheckList", "Document data: $docData")
                                
                                val item = doc.toObject(Items::class.java)
                                android.util.Log.d("CheckList", "Found item: ${item.itemname} in category: ${item.category}")
                                if (seenItems.add(item.itemname)) {
                                    itemsList.add(item)
                                    android.util.Log.d("CheckList", "Added item: ${item.itemname}")
                                }
                            }
                            
                            android.util.Log.d("CheckList", "Total items after fetching: ${itemsList.size}")
                            
                            // If no items found and not in My List or My Selections, add default items
                            if (itemsList.isEmpty() && 
                                show != MyConstants.MY_LIST_CAMEL_CASE && 
                                show != MyConstants.MY_SELECTIONS) {
                                android.util.Log.d("CheckList", "No items found, adding default items for category: $show")
                                addDefaultItems()
                            } else {
                                updateRecycler(itemsList)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("CheckList", "Error processing items: ${e.message}")
                            android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                            Toast.makeText(this, "Error processing items", Toast.LENGTH_SHORT).show()
                            progressIndicator.visibility = View.GONE
                            isFetching = false
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("CheckList", "Failed to fetch items: ${e.message}")
                        android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                        Toast.makeText(this, "Failed to fetch items: ${e.message}", Toast.LENGTH_SHORT).show()
                        updateRecycler(mutableListOf())
                        progressIndicator.visibility = View.GONE
                        isFetching = false
                    }
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error in fetchItems: ${e.message}")
            android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
            Toast.makeText(this, "Error fetching items", Toast.LENGTH_SHORT).show()
            progressIndicator.visibility = View.GONE
            isFetching = false
        }
    }

    private fun updateProgress() {
        if (itemsList.isEmpty()) {
            progressIndicator.visibility = View.GONE
            return
        }

        val checkedCount = itemsList.count { it.checked }
        val progress = (checkedCount.toFloat() / itemsList.size.toFloat() * 100).toInt()
        
        progressIndicator.visibility = View.VISIBLE
        progressIndicator.progress = progress
    }

    private fun updateRecycler(itemsList: MutableList<Items>) {
        android.util.Log.d("CheckList", "Starting updateRecycler with ${itemsList.size} items")
        
        // Make a defensive copy to avoid concurrent modification issues
        val itemsCopy = ArrayList(itemsList)
        
        runOnUiThread {
            try {
                android.util.Log.d("CheckList", "Updating recycler with ${itemsCopy.size} items")
                
                if (itemsCopy.isEmpty()) {
                    android.util.Log.d("CheckList", "List is empty, showing empty message")
                    recyclerView.visibility = View.GONE
                    txtEmptyList.visibility = View.VISIBLE
                    txtEmptyList.text = when (show) {
                        MyConstants.MY_LIST_CAMEL_CASE -> "No items in your list.\nAdd some items to get started!"
                        MyConstants.MY_SELECTIONS -> "No items selected yet.\nCheck items from other categories to add them here!"
                        else -> "No items in this category.\nAdd some items to get started!"
                    }
                } else {
                    android.util.Log.d("CheckList", "List has items, showing RecyclerView")
                    txtEmptyList.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    
                    // Log each item being displayed
                    itemsCopy.forEachIndexed { index, item ->
                        android.util.Log.d("CheckList", "Item $index to display: ${item.itemname}, category: ${item.category}, checked: ${item.checked}")
                    }
                    
                    // Update the adapter with the items
                    checkListAdapter.updateItems(itemsCopy)
                    android.util.Log.d("CheckList", "Adapter updated with ${itemsCopy.size} items")
                    android.util.Log.d("CheckList", "Adapter items count: ${checkListAdapter.itemCount}")
                    
                    // Schedule layout animation
                    recyclerView.scheduleLayoutAnimation()
                    android.util.Log.d("CheckList", "Layout animation scheduled")
                }
                updateProgress()
                progressIndicator.visibility = View.GONE
                isFetching = false
            } catch (e: Exception) {
                android.util.Log.e("CheckList", "Error in updateRecycler: ${e.message}")
                android.util.Log.e("CheckList", "Stack trace: ${e.stackTraceToString()}")
                progressIndicator.visibility = View.GONE
                isFetching = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Proper cleanup
        try {
            recyclerView.adapter = null
            itemsList.clear()
            // Clear any remaining references
            recyclerView.clearDisappearingChildren()
            recyclerView.removeAllViews()
        } catch (e: Exception) {
            android.util.Log.e("CheckList", "Error during cleanup: ${e.message}")
        }
    }
}
