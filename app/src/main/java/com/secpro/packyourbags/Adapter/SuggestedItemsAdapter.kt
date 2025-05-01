package com.secpro.packyourbags.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.secpro.packyourbags.Model.Items
import com.secpro.packyourbags.R

class SuggestedItemsAdapter(
    private val context: Context,
    private val itemsList: MutableList<Items>
) : RecyclerView.Adapter<SuggestedItemsAdapter.ViewHolder>() {

    private val selectedItems = mutableSetOf<Items>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val categoryText: TextView = itemView.findViewById(R.id.categoryText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.suggested_item_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemsList[position]
        
        holder.checkBox.text = item.itemname
        holder.categoryText.visibility = View.GONE // We don't need to show categories in suggest screen
        
        // Set checked state
        holder.checkBox.isChecked = selectedItems.contains(item)
        
        // Handle checkbox clicks
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(item)
            } else {
                selectedItems.remove(item)
            }
        }
        
        // Make the whole item clickable
        holder.itemView.setOnClickListener {
            holder.checkBox.isChecked = !holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = itemsList.size
    
    fun getSelectedItems(): List<Items> {
        return selectedItems.toList()
    }
    
    fun updateItems(newItems: List<Items>) {
        // Clear selections when updating list
        selectedItems.clear()
        
        // Update list
        itemsList.clear()
        itemsList.addAll(newItems)
        notifyDataSetChanged()
    }
} 