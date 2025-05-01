package com.secpro.packyourbags

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
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

class CheckList : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var checkListAdapter: CheckListAdapter
    private lateinit var txtAdd: EditText
    private lateinit var btnAdd: ImageButton
    private lateinit var linearLayout: LinearLayout
    private lateinit var txtEmptyList: TextView

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private var itemsList: MutableList<Items> = mutableListOf()
    private lateinit var header: String
    private lateinit var show: String

    private var isFetching = false
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_list)

        // Initialize Firebase with proper error handling
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: IllegalStateException) {
            android.util.Log.w("CheckList", "Firebase already initialized")
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        header = intent.getStringExtra(MyConstants.HEADER_SMALL) ?: ""
        show = intent.getStringExtra(MyConstants.SHOW_SMALL) ?: ""
        supportActionBar?.title = header

        txtAdd = findViewById(R.id.txtAdd)
        btnAdd = findViewById(R.id.btnAdd)
        recyclerView = findViewById(R.id.recyclerView)
        linearLayout = findViewById(R.id.linearLayout)
        txtEmptyList = findViewById(R.id.txtEmptyList)

        // Initialize RecyclerView and adapter
        recyclerView.setHasFixedSize(true)
        recyclerView.setItemViewCacheSize(20)
        recyclerView.layoutManager = StaggeredGridLayoutManager(1, LinearLayout.VERTICAL)
        checkListAdapter = CheckListAdapter(this, itemsList, show)
        recyclerView.adapter = checkListAdapter

        // Enable hardware acceleration
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        window.decorView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        if (MyConstants.FALSE_STRING == show) {
            linearLayout.visibility = View.GONE
        }

        // Initialize default items for new users
        initializeDefaultItemsForNewUser()

        // Handle button click
        btnAdd.setOnClickListener {
            addItem()
        }

        // Handle enter key press
        txtAdd.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addItem()
                true
            } else {
                false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_one, menu)

        if (header == MyConstants.MY_SELECTIONS) {
            menu.getItem(0).isVisible = false
            menu.getItem(2).isVisible = false
            menu.getItem(3).isVisible = false
        } else if (header == MyConstants.MY_LIST_CAMEL_CASE) {
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

    private fun addItem() {
        val itemName = txtAdd.text.toString().trim()
        if (itemName.isEmpty()) {
            Toast.makeText(this, "Please enter an item name", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if item already exists in the list
        if (itemsList.any { it.itemname.equals(itemName, ignoreCase = true) }) {
            Toast.makeText(this, "This item already exists in the list", Toast.LENGTH_SHORT).show()
            return
        }

        addNewItem(itemName)
        txtAdd.text.clear()
    }

    private fun addNewItem(itemName: String) {
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val item = Items(
            itemname = itemName,
            category = header,
            addedby = MyConstants.USER_SMALL,
            checked = false
        )

        db.collection("users").document(user.uid)
            .collection("items")
            .add(item)
            .addOnSuccessListener { documentReference ->
                // Add the new item at the bottom of the list
                itemsList.add(item)
                // Update the UI immediately
                updateRecycler(itemsList)
                Toast.makeText(this, "Item added successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add item: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val defaultItems = getDefaultItemsForCategory(header)
        android.util.Log.d("CheckList", "Default items for $header: ${defaultItems.size}")
        
        if (defaultItems.isEmpty()) {
            android.util.Log.d("CheckList", "No default items found for category: $header")
            updateRecycler(itemsList)
            return
        }

        val batch = db.batch()
        user?.uid?.let { uid ->
            // First, check if any of these items already exist
            db.collection("users").document(uid)
                .collection("items")
                .whereEqualTo("category", header)
                .get()
                .addOnSuccessListener { existingDocs ->
                    val existingItems = existingDocs.map { it.toObject(Items::class.java).itemname }.toSet()
                    val itemsToAdd = defaultItems.filter { it.itemname !in existingItems }
                    
                    if (itemsToAdd.isEmpty()) {
                        android.util.Log.d("CheckList", "All default items already exist")
                        fetchItems() // Refresh the list
                        return@addOnSuccessListener
                    }

                    android.util.Log.d("CheckList", "Adding ${itemsToAdd.size} new default items")
                    itemsToAdd.forEach { item ->
                        val docRef = db.collection("users").document(uid)
                            .collection("items")
                            .document()
                        batch.set(docRef, item)
                    }
                    
                    batch.commit()
                        .addOnSuccessListener {
                            android.util.Log.d("CheckList", "Successfully added ${itemsToAdd.size} default items")
                            // Add items to the list in the order they were defined
                            itemsList.addAll(itemsToAdd)
                            updateRecycler(itemsList)
                        }
                        .addOnFailureListener { e ->
                            android.util.Log.e("CheckList", "Failed to add default items: ${e.message}")
                            Toast.makeText(this, "Failed to add default items", Toast.LENGTH_SHORT).show()
                            updateRecycler(itemsList)
                        }
                }
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
        // Only fetch items if not already fetching and not first load
        if (!isFetching && !isFirstLoad) {
            fetchItems()
        }
    }

    override fun onPause() {
        super.onPause()
        // Clear any pending animations
        recyclerView.clearAnimation()
        // Save current state
        android.util.Log.d("CheckList", "onPause called, saving current state")
    }

    private fun fetchItems() {
        if (user == null) {
            android.util.Log.e("CheckList", "User is null, cannot fetch items")
            return
        }

        if (isFetching) {
            android.util.Log.d("CheckList", "Already fetching items, skipping")
            return
        }

        isFetching = true
        android.util.Log.d("CheckList", "Starting fetch for category: $header")

        // Clear the current list before fetching new items
        itemsList.clear()
        checkListAdapter.updateItems(itemsList) // Clear the adapter immediately

        if (header == MyConstants.MY_SELECTIONS) {
            // Fetch only checked items for My Selections
            db.collection("users").document(user.uid)
                .collection("items")
                .whereEqualTo("checked", true)
                .get()
                .addOnSuccessListener { documents ->
                    android.util.Log.d("CheckList", "Successfully fetched ${documents.size()} checked items")
                    val seenItems = mutableSetOf<String>()
                    
                    for (doc in documents) {
                        val item = doc.toObject(Items::class.java)
                        if (seenItems.add(item.itemname)) {
                            itemsList.add(item)
                            android.util.Log.d("CheckList", "Added checked item: ${item.itemname}")
                        }
                    }
                    updateRecycler(itemsList)
                    isFetching = false
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CheckList", "Failed to fetch checked items: ${e.message}")
                    Toast.makeText(this, "Failed to fetch checked items: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateRecycler(mutableListOf())
                    isFetching = false
                }
        } else {
            // Fetch items for the current category
            db.collection("users").document(user.uid)
                .collection("items")
                .whereEqualTo("category", header)
                .get()
                .addOnSuccessListener { documents ->
                    android.util.Log.d("CheckList", "Successfully fetched ${documents.size()} items")
                    val seenItems = mutableSetOf<String>()
                    
                    for (doc in documents) {
                        val item = doc.toObject(Items::class.java)
                        if (seenItems.add(item.itemname)) {
                            itemsList.add(item)
                            android.util.Log.d("CheckList", "Added item: ${item.itemname}")
                        }
                    }
                    
                    // If no items found and not in My List or My Selections, add default items
                    if (itemsList.isEmpty() && 
                        header != MyConstants.MY_LIST_CAMEL_CASE && 
                        header != MyConstants.MY_SELECTIONS) {
                        android.util.Log.d("CheckList", "No items found, adding default items for category: $header")
                        addDefaultItems()
                    } else {
                        updateRecycler(itemsList)
                    }
                    isFetching = false
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("CheckList", "Failed to fetch items: ${e.message}")
                    Toast.makeText(this, "Failed to fetch items: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateRecycler(mutableListOf())
                    isFetching = false
                }
        }
    }

    private fun updateRecycler(itemsList: MutableList<Items>) {
        runOnUiThread {
            android.util.Log.d("CheckList", "Updating recycler with ${itemsList.size} items")
            
            if (itemsList.isEmpty()) {
                android.util.Log.d("CheckList", "List is empty, showing empty message")
                recyclerView.visibility = View.GONE
                txtEmptyList.visibility = View.VISIBLE
                txtEmptyList.text = when (header) {
                    MyConstants.MY_LIST_CAMEL_CASE -> "No items in your list. Add some items to get started!"
                    MyConstants.MY_SELECTIONS -> "No items selected yet. Check items from other categories to add them here!"
                    else -> "No items in this category. Add some items to get started!"
                }
            } else {
                android.util.Log.d("CheckList", "List has items, showing RecyclerView")
                recyclerView.visibility = View.VISIBLE
                txtEmptyList.visibility = View.GONE
                checkListAdapter.updateItems(itemsList)
                recyclerView.post {
                    recyclerView.scheduleLayoutAnimation()
                }
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
