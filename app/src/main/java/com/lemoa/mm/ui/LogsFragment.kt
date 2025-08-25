package com.lemoa.mm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lemoa.mm.R
import com.lemoa.mm.data.AppDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsFragment : Fragment() {
    private lateinit var listView: ListView
    private lateinit var empty: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_logs, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listView = view.findViewById(R.id.listLogs)
        empty = view.findViewById(R.id.tvEmptyLogs)
        listView.emptyView = empty
        view.findViewById<View>(R.id.btnClearLogs).setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) { AppDb.get(requireContext()).logDao().clearAll() }
            Toast.makeText(requireContext(), "All logs cleared.", Toast.LENGTH_SHORT).show()
            loadList()
        }
        loadList()
    }

    private fun loadList() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { AppDb.get(requireContext()).logDao().getAllDesc() }
            val data = items.map { "${it.ts}  •  ${it.number}  •  UGX ${it.amount}  •  ${it.code}" }
            listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, data)
        }
    }
}
