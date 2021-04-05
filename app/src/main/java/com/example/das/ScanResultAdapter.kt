package com.example.das

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class ScanResultAdapter(scanResults: MutableList<ScanResult>, function: () -> Unit) {
    fun notifyDataSetChanged() {
        TODO("Not yet implemented")
    }

    fun notifyItemChanged(indexQuery: Int) {

    }

    fun notifyItemInserted(i: Int) {

    }
}
