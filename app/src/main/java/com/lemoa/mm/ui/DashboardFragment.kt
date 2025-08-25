package com.lemoa.mm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lemoa.mm.R
import com.lemoa.mm.data.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val tvCodes = view.findViewById<TextView>(R.id.tvCodesCount)
        val tvReplies = view.findViewById<TextView>(R.id.tvRepliesCount)
        val tvLast = view.findViewById<TextView>(R.id.tvLastReply)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)
        val btnRefresh = view.findViewById<Button>(R.id.btnRefresh)

        fun load() {
            lifecycleScope.launch {
                val db = AppDb.get(requireContext())
                val codes = withContext(Dispatchers.IO) { db.voucherDao().count() }
                val replies = withContext(Dispatchers.IO) { db.logDao().count() }
                val last = withContext(Dispatchers.IO) { db.logDao().latest() }
                tvCodes.text = "Total Codes: $codes"
                tvReplies.text = "Replies Sent: $replies"
                if (last != null) {
                    tvLast.text = "Last Reply: ${last.number} • UGX ${last.amount} • ${last.code} • ${last.ts}"
                    tvEmpty.visibility = View.GONE
                } else {
                    tvLast.text = "Last Reply: —"
                    tvEmpty.visibility = View.VISIBLE
                }
            }
        }
        btnRefresh.setOnClickListener { load() }
        load()
    }
}
