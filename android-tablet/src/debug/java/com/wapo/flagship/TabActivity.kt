package com.wapo.flagship

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.washingtonpost.android.R

class TabActivity : AppCompatActivity() {
    lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_tabs)
        recyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = MyAdapter()
    }
}

class MyAdapter : RecyclerView.Adapter<MyVH>() {
    override fun onBindViewHolder(
        holder: MyVH,
        position: Int,
    ) {
        with(holder.itemView as TextView) {
            text = "Text at $position"
            textSize = 20f
        }
    }

    override fun getItemCount(): Int = 100

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MyVH = MyVH(TextView(parent.context))
}

class MyVH(
    itemView: View,
) : RecyclerView.ViewHolder(itemView)
