package com.example.das

import AnalysisDatapointData
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class ResultAdapter(val adps_list: List<AnalysisDatapointData>): RecyclerView.Adapter<ResultAdapter.ResultHolder>() {

    inner class ResultHolder(val item_view: LinearLayout): RecyclerView.ViewHolder(item_view){
        val user_id = item_view.findViewById<TextView>(R.id.user_id)
        val ts = item_view.findViewById<TextView>(R.id.timestamp)
        val analysis_value = item_view.findViewById<TextView>(R.id.analysis_value)
        val ucl = item_view.findViewById<TextView>(R.id.ucl)
        val lcl = item_view.findViewById<TextView>(R.id.lcl)
        val outlier_flag = item_view.findViewById<TextView>(R.id.outlier_flag)
        val root_cause = item_view.findViewById<TextView>(R.id.root_cause)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultHolder {
        val view: LinearLayout = LayoutInflater.from(parent.context).inflate(R.layout.result_item, parent, false) as LinearLayout
        return ResultHolder(view)
    }

    override fun onBindViewHolder(holder: ResultHolder, position: Int) {
        val true_position = holder.adapterPosition
        holder.user_id.text = "User id: " + adps_list[true_position].user_id.toString()
        holder.ts.text = "Timestamp: " + adps_list[true_position].timestamp
        holder.analysis_value.text = "Analysis value: " + adps_list[true_position].analysis_value.toString()
        holder.ucl.text = "UCL: " + adps_list[true_position].ucl.toString()
        holder.lcl.text = "LCL: " + adps_list[true_position].lcl.toString()
        holder.outlier_flag.text = "Outlier flag: " + adps_list[true_position].outlier_flag.toString()
        holder.root_cause.text = "Root cause parameters: " + adps_list[true_position].root_cause
    }

    override fun getItemCount(): Int {
        return adps_list.size
    }
}