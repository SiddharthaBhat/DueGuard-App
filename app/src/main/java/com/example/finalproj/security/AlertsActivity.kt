package com.example.finalproj.security

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutDisabled: LinearLayout
    private lateinit var btnGoToSettings: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alerts)

        recyclerView = findViewById(R.id.recyclerViewAlerts)
        layoutDisabled = findViewById(R.id.layoutDisabled)
        btnGoToSettings = findViewById(R.id.btnGoToSettings)

        recyclerView.layoutManager = LinearLayoutManager(this)

        btnGoToSettings.setOnClickListener {
            startActivity(Intent(this, SecurityActivity::class.java))
        }

        checkSecurityState()
    }

    private fun checkSecurityState() {
        val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("intruder_enabled", false)

        if (isEnabled) {
            recyclerView.visibility = View.VISIBLE
            layoutDisabled.visibility = View.GONE
            loadLogs()
        } else {
            recyclerView.visibility = View.GONE
            layoutDisabled.visibility = View.VISIBLE
        }
    }

    private fun loadLogs() {

        CoroutineScope(Dispatchers.IO).launch {

            val logs = AppDatabase.getDatabase(applicationContext)
                .intruderLogDao()
                .getAllLogs()

            withContext(Dispatchers.Main) {
                recyclerView.adapter = AlertsAdapter(logs.toMutableList())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check state in case user turned it ON in settings and came back
        checkSecurityState()
    }
}