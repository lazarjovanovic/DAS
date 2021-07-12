package com.example.das

import AnalysisDatapointData
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson

class ResultingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resulting)
        val bundle = intent.extras
        val json = bundle?.getString("datapoint")
        val gson = Gson()
        val adpList: List<AnalysisDatapointData> = gson.fromJson(json , Array<AnalysisDatapointData>::class.java).toList()
        val resAdapter = ResultAdapter(adpList)
        val linearLayout = LinearLayoutManager(this)
        val rv = findViewById<RecyclerView>(R.id.recycler_view)
        rv.adapter = resAdapter
        rv.layoutManager = linearLayout
    }
}