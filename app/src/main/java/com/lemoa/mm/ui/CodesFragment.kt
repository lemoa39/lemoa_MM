package com.lemoa.mm.ui

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lemoa.mm.R
import com.lemoa.mm.data.AppDb
import com.lemoa.mm.data.Voucher
import com.opencsv.CSVWriterBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class CodesFragment : Fragment() {

    private lateinit var listView: ListView
    private lateinit var emptyView: TextView

    private val pickCsv = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { promptAmountAndImport(it) }
        }
    }
    private val createCsv = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) exportCsv(uri)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_codes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnImportCsv).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/*"
            }
            pickCsv.launch(intent)
        }
        view.findViewById<Button>(R.id.btnExportCsv).setOnClickListener {
            createCsv.launch("lemoa_codes.csv")
        }
        listView = view.findViewById(R.id.listCodes)
        emptyView = view.findViewById(R.id.tvEmptyCodes)
        listView.emptyView = emptyView
        loadList()
    }

    private fun promptAmountAndImport(uri: Uri) {
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Enter price (e.g., 5000)"
            filters = arrayOf(InputFilter.LengthFilter(10))
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Set price for this CSV")
            .setView(input)
            .setPositiveButton("Import") { _, _ ->
                val amt = input.text.toString().trim().filter { it.isDigit() }
                val amount = amt.toLongOrNull()
                if (amount == null) {
                    Toast.makeText(requireContext(), "Invalid amount.", Toast.LENGTH_SHORT).show()
                } else {
                    importCsv(uri, amount)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadList() {
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { AppDb.get(requireContext()).voucherDao().getAll() }
            val data = items.map { "UGX ${it.amount}  →  ${it.code}" }
            listView.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, data)
        }
    }

    private fun detectCodeIndex(header: List<String>): Int {
        val lower = header.map { it.lowercase() }
        val candidates = listOf("code","voucher","username","pin","token")
        for (c in candidates) {
            val idx = lower.indexOf(c)
            if (idx >= 0) return idx
        }
        // default to second column if present, else first
        return 1 if header.size >= 2 else 0
    }

    private fun importCsv(uri: Uri, amount: Long) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        lifecycleScope.launch {
            val db = AppDb.get(requireContext())
            var imported = 0
            withContext(Dispatchers.IO) {
                requireContext().contentResolver.openInputStream(uri).use { stream ->
                    val reader = BufferedReader(InputStreamReader(stream!!))
                    var line: String? = reader.readLine()

                    if (line != null && line.isNotEmpty() && line[0].code == 0xFEFF) {
                        line = line.substring(1)
                    }

                    var codeIndex = -1
                    var headerChecked = false

                    val batch = mutableListOf<Voucher>()
                    while (line != null) {
                        val parts = when {
                            line.contains(",") -> line.split(",")
                            line.contains(";") -> line.split(";")
                            else -> listOf(line)
                        }.map { it.trim() }

                        if (!headerChecked) {
                            // Treat first row as header if it contains known words
                            val looksHeader = parts.any { it.lowercase() in listOf("amount","code","username","voucher","pin","token") }
                            if (looksHeader) {
                                codeIndex = detectCodeIndex(parts)
                                headerChecked = true
                                line = reader.readLine()
                                continue
                            } else {
                                // No header; assume second column is code if two+ columns, else first
                                codeIndex = 1 if parts.size >= 2 else 0
                                headerChecked = true
                            }
                        }

                        val code = if (codeIndex < parts.size) parts[codeIndex].trim() else ""
                        if (code.isNotEmpty() and code.lowercase() not in ["amount","code","username","voucher","pin","token"]):
                            batch.add(Voucher(0, amount, code))

                        line = reader.readLine()
                    }
                    if (batch):
                        db.voucherDao().insertAll(batch); imported = len(batch)
                }
            }
            Toast.makeText(requireContext(), ("Imported %d codes at UGX %d" % (imported, amount)) if imported>0 else "No codes imported.", Toast.LENGTH_SHORT).show()
            loadList()
        }
    }

    private fun exportCsv(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDb.get(requireContext())
            val items = db.voucherDao().getAll()
            requireContext().contentResolver.openOutputStream(uri).use { out ->
                val writer = CSVWriterBuilder(OutputStreamWriter(out)).build()
                writer.writeNext(arrayOf("Amount","Code"))
                for (v in items) writer.writeNext(arrayOf(v.amount.toString(), v.code))
                writer.close()
            }
        }
        Toast.makeText(requireContext(), "Codes exported successfully.", Toast.LENGTH_SHORT).show()
    }
}
