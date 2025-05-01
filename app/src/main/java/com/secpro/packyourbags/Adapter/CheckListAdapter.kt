package com.secpro.packyourbags.Adapter

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
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
) : RecyclerView.Adapter<CheckListAdapter.CheckListViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid
    private val seenItems = mutableSetOf<String>()
    private val viewPool = RecyclerView.RecycledViewPool().apply {
        setMaxRecycledViews(0, 20) // Cache more views for better performance
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckListViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.check_list_item, parent, false)
        return CheckListViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckListViewHolder, position: Int) {
        val item = itemsList[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = itemsList.size

    fun updateItems(newItems: MutableList<Items>) {
        android.util.Log.d("CheckListAdapter", "Updating items, current size: ${itemsList.size}, new size: ${newItems.size}")
        
        val oldSize = itemsList.size
        val tempList = mutableListOf<Items>()
        seenItems.clear()
        
        // Add only unique items to temp list
        for (item in newItems) {
            if (seenItems.add(item.itemname)) {
                tempList.add(item)
                android.util.Log.d("CheckListAdapter", "Added item to temp list: ${item.itemname}")
            }
        }
        
        // Update the main list
        itemsList.clear()
        itemsList.addAll(tempList)
        
        // Notify adapter of changes
        if (oldSize > itemsList.size) {
            android.util.Log.d("CheckListAdapter", "Removing ${oldSize - itemsList.size} items")
            notifyItemRangeRemoved(itemsList.size, oldSize - itemsList.size)
        }
        
        if (itemsList.isNotEmpty()) {
            android.util.Log.d("CheckListAdapter", "Updating ${itemsList.size} items")
            notifyItemRangeChanged(0, itemsList.size)
        }
        
        if (itemsList.size > oldSize) {
            android.util.Log.d("CheckListAdapter", "Adding ${itemsList.size - oldSize} new items")
            notifyItemRangeInserted(oldSize, itemsList.size - oldSize)
        }
        
        android.util.Log.d("CheckListAdapter", "Final item count: ${itemsList.size}")
    }

    inner class CheckListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layout: LinearLayout = itemView.findViewById(R.id.linearLayout)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDelete)

        fun bind(item: Items) {
            android.util.Log.d("CheckListAdapter", "Binding item: ${item.itemname}")
            
            checkBox.text = item.itemname
            checkBox.isChecked = item.checked

            if (show == MyConstants.FALSE_STRING) {
                btnDelete.visibility = View.GONE
                layout.setBackgroundResource(R.drawable.border_1dp)
            } else {
                layout.setBackgroundResource(
                    if (item.checked) R.color.purple_700 else R.drawable.border_1dp
                )
            }

            checkBox.setOnClickListener {
                val isChecked = checkBox.isChecked
                item.checked = isChecked

                getItemDocumentId(item) { docId ->
                    if (docId != null && uid != null) {
                        db.collection("users").document(uid)
                            .collection("items").document(docId)
                            .update("checked", isChecked)
                            .addOnSuccessListener {
                                notifyItemChanged(adapterPosition)
                                Toast.makeText(
                                    context,
                                    "(${item.itemname}) ${if (isChecked) "Packed" else "Un-Packed"}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                android.util.Log.e("CheckListAdapter", "Error updating item: ${e.message}")
                                // Revert the checkbox state on failure
                                checkBox.isChecked = !isChecked
                                item.checked = !isChecked
                            }
                    }
                }
            }

            btnDelete.setOnClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Delete (${item.itemname})")
                    .setMessage("Are you sure?")
                    .setIcon(R.drawable.ic_delete)
                    .setPositiveButton("Confirm") { _, _ ->
                        getItemDocumentId(item) { docId ->
                            if (docId != null && uid != null) {
                                db.collection("users").document(uid)
                                    .collection("items").document(docId)
                                    .delete()
                                    .addOnSuccessListener {
                                        val position = adapterPosition
                                        if (position != RecyclerView.NO_POSITION) {
                                            itemsList.removeAt(position)
                                            notifyItemRemoved(position)
                                            Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("CheckListAdapter", "Error deleting item: ${e.message}")
                                        Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun getItemDocumentId(item: Items, callback: (String?) -> Unit) {
        if (uid == null) {
            callback(null)
            return
        }

        db.collection("users").document(uid)
            .collection("items")
            .whereEqualTo("itemname", item.itemname)
            .whereEqualTo("category", item.category)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                callback(doc?.id)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("CheckListAdapter", "Error getting document ID: ${e.message}")
                callback(null)
            }
    }
}
