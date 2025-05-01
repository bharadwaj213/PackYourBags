package com.secpro.packyourbags.Adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.secpro.packyourbags.Constants.MyConstants
import com.secpro.packyourbags.Model.Items
import com.secpro.packyourbags.R

class CheckListAdapter(
    private val context: Context,
    private var itemsList: MutableList<Items>,
    private val show: String
) : RecyclerView.Adapter<CheckListAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser
    private var recyclerView: RecyclerView? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        val itemLayout: CardView = itemView.findViewById(R.id.itemLayout)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        
        // Set up swipe actions
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                try {
                    val position = viewHolder.adapterPosition
                    if (position !in itemsList.indices) {
                        android.util.Log.e("CheckListAdapter", "Invalid position in swipe: $position")
                        return
                    }
                    val item = itemsList[position]
                    
                    when (direction) {
                        ItemTouchHelper.LEFT -> {
                            // Delete item
                            deleteItem(item, position)
                        }
                        ItemTouchHelper.RIGHT -> {
                            // Toggle checked status
                            toggleItemChecked(item, position)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CheckListAdapter", "Error in swipe action: ${e.message}")
                }
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        android.util.Log.d("CheckListAdapter", "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.check_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            if (position !in itemsList.indices) {
                android.util.Log.e("CheckListAdapter", "Invalid position: $position")
                return
            }

            val item = itemsList[position]
            android.util.Log.d("CheckListAdapter", "Binding item at position $position: ${item.itemname}")
            
            holder.checkBox.text = item.itemname
            holder.checkBox.isChecked = item.checked

            // Log view dimensions
            android.util.Log.d("CheckListAdapter", "View dimensions - Width: ${holder.itemView.width}, Height: ${holder.itemView.height}")
            android.util.Log.d("CheckListAdapter", "CheckBox dimensions - Width: ${holder.checkBox.width}, Height: ${holder.checkBox.height}")

            // Set up click listeners
            holder.checkBox.setOnCheckedChangeListener(null) // Clear any existing listener
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                try {
                    toggleItemChecked(item, position)
                } catch (e: Exception) {
                    android.util.Log.e("CheckListAdapter", "Error in checkbox change: ${e.message}")
                }
            }

            holder.btnDelete.setOnClickListener {
                try {
                    deleteItem(item, position)
                } catch (e: Exception) {
                    android.util.Log.e("CheckListAdapter", "Error in delete click: ${e.message}")
                }
            }

            // Ensure views are visible
            holder.itemLayout.visibility = View.VISIBLE
            holder.checkBox.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE

            // Add animation
            holder.itemLayout.alpha = 0f
            holder.itemLayout.animate()
                .alpha(1f)
                .setDuration(300)
                .setListener(null)
        } catch (e: Exception) {
            android.util.Log.e("CheckListAdapter", "Error in onBindViewHolder: ${e.message}")
        }
    }

    private fun toggleItemChecked(item: Items, position: Int) {
        try {
            if (user == null) return

            val updatedItem = item.copy(checked = !item.checked)
            db.collection("users").document(user.uid)
                .collection("items")
                .whereEqualTo("itemname", item.itemname)
                .whereEqualTo("category", item.category)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        for (doc in documents) {
                            db.collection("users").document(user.uid)
                                .collection("items")
                                .document(doc.id)
                                .update("checked", updatedItem.checked)
                                .addOnSuccessListener {
                                    if (position in itemsList.indices) {
                                        itemsList[position] = updatedItem
                                        notifyItemChanged(position)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CheckListAdapter", "Error updating item: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("CheckListAdapter", "Error in toggleItemChecked: ${e.message}")
        }
    }

    private fun deleteItem(item: Items, position: Int) {
        try {
            if (user == null) return

            db.collection("users").document(user.uid)
                .collection("items")
                .whereEqualTo("itemname", item.itemname)
                .whereEqualTo("category", item.category)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        for (doc in documents) {
                            db.collection("users").document(user.uid)
                                .collection("items")
                                .document(doc.id)
                                .delete()
                                .addOnSuccessListener {
                                    if (position in itemsList.indices) {
                                        itemsList.removeAt(position)
                                        notifyItemRemoved(position)
                                        notifyItemRangeChanged(position, itemsList.size)
                                    }
                                }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CheckListAdapter", "Error deleting item: ${e.message}")
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("CheckListAdapter", "Error in deleteItem: ${e.message}")
        }
    }

    fun updateItems(newItems: MutableList<Items>) {
        try {
            android.util.Log.d("CheckListAdapter", "Updating items, old size: ${itemsList.size}, new size: ${newItems.size}")
            
            // Log old and new items for debugging
            itemsList.forEachIndexed { index, item ->
                android.util.Log.d("CheckListAdapter", "Old item $index: ${item.itemname}, checked: ${item.checked}")
            }
            
            newItems.forEachIndexed { index, item ->
                android.util.Log.d("CheckListAdapter", "New item $index: ${item.itemname}, checked: ${item.checked}")
            }
            
            // Calculate the differences and update only changed items
            val oldList = itemsList.toList()
            val diffResult = calculateDiff(oldList, newItems)
            
            // Update the items list - make a new copy to avoid reference issues
            itemsList.clear()
            itemsList.addAll(ArrayList(newItems))
            
            android.util.Log.d("CheckListAdapter", "Items list updated, new size: ${itemsList.size}")
            
            // Apply the calculated differences
            diffResult.dispatchUpdatesTo(this)
            
            // Also notify that the whole dataset changed as a fallback
            notifyDataSetChanged()
            
            android.util.Log.d("CheckListAdapter", "Notified adapter of changes")
            
            // Force layout update on the next UI cycle
            recyclerView?.post {
                android.util.Log.d("CheckListAdapter", "Forcing layout update")
                recyclerView?.requestLayout()
                recyclerView?.invalidate()
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckListAdapter", "Error updating items: ${e.message}")
            android.util.Log.e("CheckListAdapter", "Stack trace: ${e.stackTraceToString()}")
            
            // Try a simpler approach as fallback
            try {
                itemsList.clear()
                itemsList.addAll(newItems)
                notifyDataSetChanged()
                android.util.Log.d("CheckListAdapter", "Fallback update completed")
            } catch (e2: Exception) {
                android.util.Log.e("CheckListAdapter", "Even fallback update failed: ${e2.message}")
            }
        }
    }

    private fun calculateDiff(oldList: List<Items>, newList: List<Items>): androidx.recyclerview.widget.DiffUtil.DiffResult {
        return androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition].itemname == newList[newItemPosition].itemname &&
                       oldList[oldItemPosition].category == newList[newItemPosition].category
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldList[oldItemPosition] == newList[newItemPosition]
            }
        })
    }

    override fun getItemCount(): Int = itemsList.size
}
