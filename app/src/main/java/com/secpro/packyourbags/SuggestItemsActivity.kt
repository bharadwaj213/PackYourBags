package com.secpro.packyourbags

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.secpro.packyourbags.Adapter.SuggestedItemsAdapter
import com.secpro.packyourbags.Constants.MyConstants
import com.secpro.packyourbags.Model.Items
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class SuggestItemsActivity : AppCompatActivity() {

    private lateinit var destinationInput: TextInputEditText
    private lateinit var startDateInput: TextInputEditText
    private lateinit var endDateInput: TextInputEditText
    private lateinit var generateSuggestionsBtn: MaterialButton
    private lateinit var addToListBtn: MaterialButton
    private lateinit var suggestedItemsRecyclerView: RecyclerView
    private lateinit var progressBar: View
    private lateinit var resultsCard: View
    private lateinit var weatherInfo: android.widget.TextView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

    private lateinit var adapter: SuggestedItemsAdapter
    private val suggestedItems = mutableListOf<Items>()
    
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    
    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val OPENWEATHER_API_KEY = "bd5e378503939ddaee76f12ad7a97608" // Using a free API key

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggest_items)
        
        try {
            initViews()
            setupToolbar()
            setupDatePickers()
            setupRecyclerView()
            setupButtons()
        } catch (e: Exception) {
            Log.e("SuggestItems", "Error in onCreate: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initViews() {
        try {
            destinationInput = findViewById(R.id.destinationInput)
            startDateInput = findViewById(R.id.startDateInput)
            endDateInput = findViewById(R.id.endDateInput)
            generateSuggestionsBtn = findViewById(R.id.generateSuggestionsBtn)
            addToListBtn = findViewById(R.id.addToListBtn)
            suggestedItemsRecyclerView = findViewById(R.id.suggestedItemsRecyclerView)
            progressBar = findViewById(R.id.progressBar)
            resultsCard = findViewById(R.id.resultsCard)
            weatherInfo = findViewById(R.id.weatherInfo)
            toolbar = findViewById(R.id.toolbar)
        } catch (e: Exception) {
            Log.e("SuggestItems", "Error initializing views: ${e.message}")
            throw e
        }
    }
    
    private fun setupToolbar() {
        try {
            // Now we can use setSupportActionBar since we've set the NoActionBar theme
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
            
            toolbar.setNavigationOnClickListener {
                onBackPressed()
            }
        } catch (e: Exception) {
            Log.e("SuggestItems", "Error setting up toolbar: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun setupDatePickers() {
        // Set end date to tomorrow by default
        endCalendar.add(Calendar.DAY_OF_MONTH, 1)
        
        startDateInput.setOnClickListener {
            showDatePicker(true)
        }
        
        endDateInput.setOnClickListener {
            showDatePicker(false)
        }
    }
    
    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = if (isStartDate) startCalendar else endCalendar
        
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                val dateText = dateFormat.format(calendar.time)
                
                if (isStartDate) {
                    startDateInput.setText(dateText)
                    // If end date is before start date, update end date
                    if (endCalendar.before(startCalendar)) {
                        endCalendar.time = startCalendar.time
                        endCalendar.add(Calendar.DAY_OF_MONTH, 1)
                        endDateInput.setText(dateFormat.format(endCalendar.time))
                    }
                } else {
                    endDateInput.setText(dateText)
                    // If start date is after end date, update start date
                    if (startCalendar.after(endCalendar)) {
                        startCalendar.time = endCalendar.time
                        startCalendar.add(Calendar.DAY_OF_MONTH, -1)
                        startDateInput.setText(dateFormat.format(startCalendar.time))
                    }
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun setupRecyclerView() {
        try {
            // Create a new empty list to avoid potential initialization issues
            val items = mutableListOf<Items>()
            
            // Initialize adapter with the empty list
            adapter = SuggestedItemsAdapter(this, items)
            
            // Set layout manager
            val layoutManager = LinearLayoutManager(this)
            suggestedItemsRecyclerView.layoutManager = layoutManager
            
            // Set adapter
            suggestedItemsRecyclerView.adapter = adapter
            
            // Set fixed height for better visibility
            suggestedItemsRecyclerView.layoutParams.height = 
                resources.displayMetrics.heightPixels / 2
            
            // Copy the items (this is now safe since adapter is initialized)
            suggestedItems.clear()
            
            Log.d("SuggestItems", "RecyclerView setup complete")
        } catch (e: Exception) {
            Log.e("SuggestItems", "Error setting up RecyclerView: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun setupButtons() {
        generateSuggestionsBtn.setOnClickListener {
            if (validateInputs()) {
                generateSuggestions()
            }
        }
        
        addToListBtn.setOnClickListener {
            addSelectedItemsToList()
        }
    }
    
    private fun validateInputs(): Boolean {
        val destination = destinationInput.text.toString().trim()
        val startDate = startDateInput.text.toString().trim()
        val endDate = endDateInput.text.toString().trim()
        
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (startDate.isEmpty()) {
            Toast.makeText(this, "Please select a start date", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (endDate.isEmpty()) {
            Toast.makeText(this, "Please select an end date", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    private fun generateSuggestions() {
        progressBar.visibility = View.VISIBLE
        resultsCard.visibility = View.GONE
        suggestedItems.clear()
        adapter.notifyDataSetChanged()
        
        val destination = destinationInput.text.toString().trim()
        val startDate = startDateInput.text.toString().trim()
        val endDate = endDateInput.text.toString().trim()
        
        // First, get weather information for the destination
        fetchWeatherData(destination)
    }
    
    private fun fetchWeatherData(city: String) {
        try {
            // Build the URL for the OpenWeather API
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("api.openweathermap.org")
                .addPathSegment("data")
                .addPathSegment("2.5")
                .addPathSegment("weather")
                .addQueryParameter("q", city)
                .addQueryParameter("units", "metric")
                .addQueryParameter("appid", OPENWEATHER_API_KEY)
                .build()
            
            Log.d("SuggestItems", "Fetching weather data for city: $city")
            Log.d("SuggestItems", "URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("SuggestItems", "Error fetching weather data: ${e.message}")
                    
                    runOnUiThread {
                        Toast.makeText(
                            this@SuggestItemsActivity,
                            "Error fetching weather data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = View.GONE
                        
                        // If we can't get weather, still generate suggestions based on destination
                        generatePackingList(city, null)
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e("SuggestItems", "Unsuccessful response: ${response.code}")
                        
                        runOnUiThread {
                            Toast.makeText(
                                this@SuggestItemsActivity,
                                "Error: Couldn't find weather for this location",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = View.GONE
                            
                            // If we can't get weather, still generate suggestions based on destination
                            generatePackingList(city, null)
                        }
                        return
                    }
                    
                    try {
                        val responseData = response.body?.string()
                        val jsonObject = JSONObject(responseData)
                        
                        // Extract weather information
                        val weatherArray = jsonObject.getJSONArray("weather")
                        val weatherDescription = weatherArray.getJSONObject(0).getString("description")
                        val weatherMain = weatherArray.getJSONObject(0).getString("main")
                        
                        val mainObj = jsonObject.getJSONObject("main")
                        val temperature = mainObj.getDouble("temp")
                        val tempMin = mainObj.getDouble("temp_min")
                        val tempMax = mainObj.getDouble("temp_max")
                        
                        val weatherData = WeatherData(
                            weatherMain,
                            weatherDescription,
                            temperature,
                            tempMin,
                            tempMax
                        )
                        
                        runOnUiThread {
                            generatePackingList(city, weatherData)
                        }
                        
                    } catch (e: Exception) {
                        Log.e("SuggestItems", "Error parsing weather data: ${e.message}")
                        
                        runOnUiThread {
                            Toast.makeText(
                                this@SuggestItemsActivity,
                                "Error parsing weather data",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = View.GONE
                            
                            // If we can't parse weather, still generate suggestions based on destination
                            generatePackingList(city, null)
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("SuggestItems", "Error creating weather request: ${e.message}")
            e.printStackTrace()
            
            runOnUiThread {
                Toast.makeText(
                    this@SuggestItemsActivity,
                    "Error creating weather request: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
                
                // If we can't get weather, still generate suggestions based on destination
                generatePackingList(city, null)
            }
        }
    }
    
    private fun generatePackingList(destination: String, weatherData: WeatherData?) {
        val tripDuration = calculateTripDuration()
        
        // Generate weather information text
        val weatherInfoText = if (weatherData != null) {
            "Weather in $destination: ${weatherData.description}, " +
                    "Temperature: ${weatherData.temp.toInt()}°C (${weatherData.tempMin.toInt()}°C - ${weatherData.tempMax.toInt()}°C)"
        } else {
            "Weather information not available for $destination"
        }
        
        // Show loading indicator
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            weatherInfo.text = "Generating packing recommendations for $destination..."
        }
        
        // Get AI-generated packing list using a prompt
        generateAIRecommendations(destination, weatherData, tripDuration)
    }
    
    private fun generateAIRecommendations(
        destination: String, 
        weatherData: WeatherData?, 
        tripDuration: Int
    ) {
        // Log input parameters
        Log.d("SuggestItems", "==== STARTING AI RECOMMENDATION PROCESS ====")
        Log.d("SuggestItems", "Destination: $destination")
        Log.d("SuggestItems", "Trip Duration: $tripDuration days")
        if (weatherData != null) {
            Log.d("SuggestItems", "Weather Main: ${weatherData.main}")
            Log.d("SuggestItems", "Weather Description: ${weatherData.description}")
            Log.d("SuggestItems", "Temperature: ${weatherData.temp}°C (${weatherData.tempMin}°C - ${weatherData.tempMax}°C)")
        } else {
            Log.d("SuggestItems", "Weather Data: Not available")
        }
        
        // Generate weather information text
        val weatherInfoText = if (weatherData != null) {
            "Weather in $destination: ${weatherData.description}, " +
                    "Temperature: ${weatherData.temp.toInt()}°C (${weatherData.tempMin.toInt()}°C - ${weatherData.tempMax.toInt()}°C)"
        } else {
            "Weather information not available for $destination"
        }
        
        // Basic items that everyone needs regardless of destination
        val basicItems = listOf(
            Items(itemname = "Passport/ID", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
            Items(itemname = "Money/Credit cards", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
            Items(itemname = "Phone & charger", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
            Items(itemname = "Travel insurance info", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false)
        )
        
        Log.d("SuggestItems", "Basic Items: ${basicItems.size}")
        
        // Based on the weather and destination, create appropriate items
        val weatherBasedItems = mutableListOf<Items>()
        
        // Add items based on weather condition
        if (weatherData != null) {
            when {
                weatherData.main.contains("Rain", ignoreCase = true) -> {
                    Log.d("SuggestItems", "Weather condition detected: Rain")
                    weatherBasedItems.add(Items(itemname = "Umbrella", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Raincoat", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Waterproof shoes", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
                weatherData.main.contains("Snow", ignoreCase = true) -> {
                    weatherBasedItems.add(Items(itemname = "Winter coat", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Gloves", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Scarf", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Hat/beanie", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Snow boots", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
                weatherData.main.contains("Clear", ignoreCase = true) -> {
                    weatherBasedItems.add(Items(itemname = "Sunglasses", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Sunscreen", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    if (weatherData.temp > 25) {
                        weatherBasedItems.add(Items(itemname = "Sun hat", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                        weatherBasedItems.add(Items(itemname = "Light clothing", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    }
                }
            }
            
            // Add items based on temperature
            when {
                weatherData.temp < 5 -> {
                    weatherBasedItems.add(Items(itemname = "Heavy winter coat", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Thermal layers", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Winter boots", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
                weatherData.temp < 15 -> {
                    weatherBasedItems.add(Items(itemname = "Light jacket or sweater", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Long pants", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
                weatherData.temp < 25 -> {
                    weatherBasedItems.add(Items(itemname = "T-shirts", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Light sweater", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Casual shoes", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
                else -> {
                    weatherBasedItems.add(Items(itemname = "T-shirts", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Shorts", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Sandals", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                    weatherBasedItems.add(Items(itemname = "Swimwear", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
                }
            }
        } else {
            // Default items if weather data is not available
            weatherBasedItems.add(Items(itemname = "Umbrella", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            weatherBasedItems.add(Items(itemname = "Light jacket", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            weatherBasedItems.add(Items(itemname = "Weather-appropriate clothing", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        }
        
        // Items based on trip duration
        val durationBasedItems = mutableListOf<Items>()
        
        // Calculate clothing needs
        val shirtCount = (tripDuration + 1) / 2
        val underwearCount = tripDuration + 1
        
        Log.d("SuggestItems", "Trip duration-based calculation: $tripDuration days")
        Log.d("SuggestItems", "Calculated shirts: $shirtCount, underwear: $underwearCount")
        
        durationBasedItems.add(Items(itemname = "T-shirts/tops (${shirtCount})", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        durationBasedItems.add(Items(itemname = "Underwear (${underwearCount})", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        durationBasedItems.add(Items(itemname = "Socks (${underwearCount})", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        
        // Add toiletries for longer trips
        if (tripDuration > 3) {
            durationBasedItems.add(Items(itemname = "Toothbrush & toothpaste", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            durationBasedItems.add(Items(itemname = "Shampoo & conditioner", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            durationBasedItems.add(Items(itemname = "Deodorant", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            durationBasedItems.add(Items(itemname = "Razor", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        }
        
        // Add items for even longer trips
        if (tripDuration > 7) {
            durationBasedItems.add(Items(itemname = "Laundry soap", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            durationBasedItems.add(Items(itemname = "First aid kit", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
            durationBasedItems.add(Items(itemname = "Sewing kit", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false))
        }
        
        // Add destination-specific items based on common knowledge
        val destinationBasedItems = when {
            destination.contains("beach", ignoreCase = true) || 
            destination.contains("miami", ignoreCase = true) || 
            destination.contains("hawaii", ignoreCase = true) || 
            destination.contains("cancun", ignoreCase = true) -> {
                Log.d("SuggestItems", "Destination type detected: Beach")
                listOf(
                    Items(itemname = "Swimsuit", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Beach towel", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Flip flops", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Beach bag", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false)
                )
            }
            destination.contains("mountain", ignoreCase = true) || 
            destination.contains("alps", ignoreCase = true) || 
            destination.contains("hiking", ignoreCase = true) -> {
                listOf(
                    Items(itemname = "Hiking boots", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Water bottle", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Hiking backpack", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Walking stick", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false)
                )
            }
            destination.contains("ski", ignoreCase = true) || 
            destination.contains("snow", ignoreCase = true) -> {
                listOf(
                    Items(itemname = "Ski jacket", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Ski pants", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Thermals", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false),
                    Items(itemname = "Ski goggles", category = MyConstants.SUGGEST_ME_CAMEL_CASE, addedby = "system", checked = false)
                )
            }
            else -> emptyList()
        }
        
        // Combine all items and remove duplicates
        val allItems = (basicItems + weatherBasedItems + durationBasedItems + destinationBasedItems)
            .distinctBy { it.itemname }
        
        // Log the suggested items
        Log.d("SuggestItems", "==== AI SUGGESTIONS RESULT ====")
        Log.d("SuggestItems", "Total items suggested: ${allItems.size}")
        Log.d("SuggestItems", "Weather-based items: ${weatherBasedItems.size}")
        Log.d("SuggestItems", "Duration-based items: ${durationBasedItems.size}")
        Log.d("SuggestItems", "Destination-based items: ${destinationBasedItems.size}")
        Log.d("SuggestItems", "------------------------")
        Log.d("SuggestItems", "AI Suggestions for $destination (${allItems.size} items):")
        for (item in allItems) {
            Log.d("SuggestItems", "- ${item.itemname}")
        }
        
        if (allItems.isEmpty()) {
            Log.e("SuggestItems", "WARNING: AI returned ZERO suggestions!")
        }
        
        // Update UI
        runOnUiThread {
            Log.d("SuggestItems", "Updating UI with ${allItems.size} items")
            weatherInfo.text = weatherInfoText
            
            // Use the adapter's update method instead
            adapter.updateItems(allItems)
            
            // Ensure RecyclerView is visible and has proper height
            val recyclerViewHeight = if (allItems.size > 5) {
                resources.displayMetrics.heightPixels / 2
            } else {
                ViewGroup.LayoutParams.WRAP_CONTENT
            }
            suggestedItemsRecyclerView.layoutParams.height = recyclerViewHeight
            suggestedItemsRecyclerView.visibility = View.VISIBLE
            suggestedItemsRecyclerView.requestLayout()
            
            // Log all items to verify they are in the adapter
            for (i in 0 until adapter.itemCount) {
                Log.d("SuggestItems", "Item $i in adapter: ${suggestedItems.getOrNull(i)?.itemname}")
            }
            
            progressBar.visibility = View.GONE
            resultsCard.visibility = View.VISIBLE
            
            // Show a toast with the number of suggestions
            Toast.makeText(
                this@SuggestItemsActivity,
                "Generated ${allItems.size} suggestions for $destination",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun calculateTripDuration(): Int {
        val millisPerDay = 24 * 60 * 60 * 1000L
        val startTime = startCalendar.timeInMillis
        val endTime = endCalendar.timeInMillis
        return ((endTime - startTime) / millisPerDay).toInt() + 1
    }
    
    private fun addSelectedItemsToList() {
        if (user == null) {
            Toast.makeText(this, "Please log in to add items", Toast.LENGTH_SHORT).show()
            return
        }
        
        val selectedItems = adapter.getSelectedItems()
        
        if (selectedItems.isEmpty()) {
            Toast.makeText(this, "Please select at least one item", Toast.LENGTH_SHORT).show()
            return
        }
        
        progressBar.visibility = View.VISIBLE
        
        val batch = db.batch()
        var count = 0
        
        // Add items to the "Recommended Items" category instead of "My List"
        for (item in selectedItems) {
            val modifiedItem = Items(
                itemname = item.itemname,
                category = MyConstants.RECOMMENDED_ITEMS_CAMEL_CASE,
                addedby = user.uid,
                checked = false
            )
            
            val docRef = db.collection("users").document(user.uid)
                .collection("items")
                .document()
                
            batch.set(docRef, modifiedItem)
            count++
        }
        
        batch.commit()
            .addOnSuccessListener {
                Log.d("SuggestItems", "Successfully added $count items to Recommended Items")
                Toast.makeText(
                    this,
                    "Added $count items to Recommended Items",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e("SuggestItems", "Failed to add items: ${e.message}")
                Toast.makeText(
                    this,
                    "Failed to add items: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                progressBar.visibility = View.GONE
            }
    }
    
    data class WeatherData(
        val main: String,
        val description: String,
        val temp: Double,
        val tempMin: Double,
        val tempMax: Double
    )
} 