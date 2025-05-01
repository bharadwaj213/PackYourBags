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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckListViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.check_list_item, parent, false)
        return CheckListViewHolder(view)
    }

    override fun onBindViewHolder(holder: CheckListViewHolder, position: Int) {
        val item = itemsList[position]
        holder.checkBox.text = item.itemname
        holder.checkBox.isChecked = item.checked

        if (show == MyConstants.FALSE_STRING) {
            holder.btnDelete.visibility = View.GONE
            holder.layout.setBackgroundResource(R.drawable.border_1dp)
        } else {
            holder.layout.setBackgroundResource(
                if (item.checked) R.color.purple_700 else R.drawable.border_1dp
            )
        }

        holder.checkBox.setOnClickListener {
            val isChecked = holder.checkBox.isChecked
            item.checked = isChecked

            getItemDocumentId(item) { docId ->
                if (docId != null && uid != null) {
                    db.collection("users").document(uid)
                        .collection("items").document(docId)
                        .update("checked", isChecked)
                        .addOnSuccessListener {
                            notifyDataSetChanged()
                            Toast.makeText(
                                context,
                                "(${item.itemname}) ${if (isChecked) "Packed" else "Un-Packed"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
        }

        holder.btnDelete.setOnClickListener {
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
                                    itemsList.removeAt(position)
                                    notifyDataSetChanged()
                                    Toast.makeText(context, "Item deleted", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun getItemCount(): Int = itemsList.size

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
            .addOnFailureListener {
                callback(null)
            }
    }

    class CheckListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val layout: LinearLayout = itemView.findViewById(R.id.linearLayout)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }
}
