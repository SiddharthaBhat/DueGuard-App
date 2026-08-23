package com.example.finalproj

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.finalproj.home.UploadActivity
import com.example.finalproj.navigation.FullSettingsFragment
import com.example.finalproj.navigation.MemberDocumentsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainNavigationActivity : AppCompatActivity() {

    private var memberId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_navigation)

        memberId = intent.getIntExtra("memberId", -1)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        if (savedInstanceState == null) {
            loadFragment(MemberDocumentsFragment.newInstance(memberId))
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    loadFragment(MemberDocumentsFragment.newInstance(memberId))
                    true
                }

                R.id.nav_add -> {
                    val intent = Intent(this, UploadActivity::class.java)
                    intent.putExtra("memberId", memberId)
                    startActivity(intent)
                    false 
                }

                R.id.nav_settings -> {
                    // Full Settings as a FRAGMENT to keep navigation menu visible
                    loadFragment(FullSettingsFragment())
                    true
                }

                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}