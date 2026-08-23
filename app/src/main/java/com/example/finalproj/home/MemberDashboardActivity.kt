package com.example.finalproj.home

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.finalproj.R
import com.example.finalproj.navigation.FullSettingsFragment
import com.example.finalproj.navigation.MemberDocumentsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MemberDashboardActivity : AppCompatActivity() {

    private var memberId: Int = -1
    private var memberName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_member_dashboard)

        memberId = intent.getIntExtra("memberId", -1)
        memberName = intent.getStringExtra("memberName")

        if (memberId == -1) {
            finish()
            return
        }

        // Setup Header
        findViewById<TextView>(R.id.tvMemberName).text = memberName ?: "Member"
        
        // Setup Back Button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.memberBottomNav)

        if (savedInstanceState == null) {
            bottomNav.selectedItemId = R.id.nav_documents
            loadFragment(MemberDocumentsFragment.newInstance(memberId))
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_documents -> {
                    loadFragment(MemberDocumentsFragment.newInstance(memberId))
                    true
                }
                R.id.nav_add -> {
                    val intent = Intent(this, UploadActivity::class.java)
                    intent.putExtra("memberId", memberId)
                    startActivity(intent)
                    false // Don't select the tab, just perform action
                }
                R.id.nav_settings -> {
                    // ✅ FIXED: Load as Fragment to keep Navigation Menu visible
                    loadFragment(FullSettingsFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.memberFragmentContainer, fragment)
            .commit()
    }
}