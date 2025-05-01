package com.secpro.packyourbags.Adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.secpro.packyourbags.CheckList
import com.secpro.packyourbags.Constants.MyConstants
import com.secpro.packyourbags.R
import com.secpro.packyourbags.SuggestItemsActivity

class MyAdapter(
    private val context: Context,
    private val titles: List<String>,
    private val images: List<Int>,
    private val activity: Activity
) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.main_item, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.title.text = titles[position]
        holder.img.setImageResource(images[position])
        holder.linearLayout.alpha = 0.8f

        holder.linearLayout.setOnClickListener {
            if (titles[position] == MyConstants.SUGGEST_ME_CAMEL_CASE) {
                // Launch the SuggestItemsActivity for the "Suggest Me" category
                val intent = Intent(context, SuggestItemsActivity::class.java)
                context.startActivity(intent)
            } else {
                // Handle other categories as before
                val intent = Intent(context, CheckList::class.java).apply {
                    val category = when (titles[position]) {
                        MyConstants.BASIC_NEEDS_CAMEL_CASE -> MyConstants.BASIC_NEEDS_CAMEL_CASE
                        MyConstants.CLOTHING_CAMEL_CASE -> MyConstants.CLOTHING_CAMEL_CASE
                        MyConstants.PERSONAL_CARE_CAMEL_CASE -> MyConstants.PERSONAL_CARE_CAMEL_CASE
                        MyConstants.BABY_NEEDS_CAMEL_CASE -> MyConstants.BABY_NEEDS_CAMEL_CASE
                        MyConstants.HEALTH_CAMEL_CASE -> MyConstants.HEALTH_CAMEL_CASE
                        MyConstants.TECHNOLOGY_CAMEL_CASE -> MyConstants.TECHNOLOGY_CAMEL_CASE
                        MyConstants.FOOD_CAMEL_CASE -> MyConstants.FOOD_CAMEL_CASE
                        MyConstants.BEACH_SUPPLIES_CAMEL_CASE -> MyConstants.BEACH_SUPPLIES_CAMEL_CASE
                        MyConstants.CAR_SUPPLIES_CAMEL_CASE -> MyConstants.CAR_SUPPLIES_CAMEL_CASE
                        MyConstants.NEEDS_CAMEL_CASE -> MyConstants.NEEDS_CAMEL_CASE
                        MyConstants.MY_LIST_CAMEL_CASE -> MyConstants.MY_LIST_CAMEL_CASE
                        MyConstants.MY_SELECTIONS_CAMEL_CASE -> MyConstants.MY_SELECTIONS_CAMEL_CASE
                        else -> titles[position]
                    }
                    putExtra(MyConstants.HEADER_SMALL, category)
                    putExtra(MyConstants.SHOW_SMALL, if (category == MyConstants.MY_SELECTIONS_CAMEL_CASE) MyConstants.FALSE_STRING else MyConstants.TRUE_STRING)
                }
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int = titles.size

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val img: ImageView = itemView.findViewById(R.id.img)
        val linearLayout: LinearLayout = itemView.findViewById(R.id.linearLayout)
    }
}