package com.lemoa.mm.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.lemoa.mm.R
import com.lemoa.mm.data.AppDb
import com.lemoa.mm.data.SettingsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etTemplate = view.findViewById<EditText>(R.id.etTemplate)
        val etRegexMtn = view.findViewById<EditText>(R.id.etRegexMtn)
        val etRegexAirtel = view.findViewById<EditText>(R.id.etRegexAirtel)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)

        lifecycleScope.launch {
            val db = AppDb.get(requireContext())
            val s = withContext(Dispatchers.IO) { db.settingsDao().get() } ?: SettingsEntity.default()
            etTemplate.setText(s.template)
            etRegexMtn.setText(s.regexMtn)
            etRegexAirtel.setText(s.regexAirtel)
        }

        btnSave.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDb.get(requireContext())
                db.settingsDao().upsert(SettingsEntity(
                    id = 1,
                    template = etTemplate.text.toString().ifBlank { "Your code is: {code}" },
                    regexMtn = etRegexMtn.text.toString().ifBlank { SettingsEntity.default().regexMtn },
                    regexAirtel = etRegexAirtel.text.toString().ifBlank { SettingsEntity.default().regexAirtel }
                ))
            }
            Toast.makeText(requireContext(), "Saved.", Toast.LENGTH_SHORT).show()
        }
    }
}
