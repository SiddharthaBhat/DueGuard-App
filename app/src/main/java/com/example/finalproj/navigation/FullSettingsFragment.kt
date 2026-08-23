package com.example.finalproj.navigation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finalproj.R
import com.example.finalproj.appinfo.AppInfoActivity
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.profile.ProfileActivity
import com.example.finalproj.security.SecurityActivity
import com.example.finalproj.settings.NotificationSettingsActivity

class FullSettingsFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        db = AppDatabase.getDatabase(requireContext())

        val layoutProfile = view.findViewById<View>(R.id.layoutProfile)
        val layoutNotifications = view.findViewById<View>(R.id.layoutNotifications)
        val layoutAppInfo = view.findViewById<View>(R.id.layoutAppInfo)
        val layoutSecurity = view.findViewById<View>(R.id.layoutSecurity)
        val layoutLogout = view.findViewById<View>(R.id.layoutLogout)
        val layoutSync = view.findViewById<View>(R.id.layoutSync)

        // 🚀 FULL LOCAL: Hide Cloud Sync and Logout permanently
        layoutLogout.visibility = View.GONE
        layoutSync.visibility = View.GONE

        layoutProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        layoutNotifications.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationSettingsActivity::class.java))
        }

        layoutAppInfo.setOnClickListener {
            startActivity(Intent(requireContext(), AppInfoActivity::class.java))
        }

        layoutSecurity.setOnClickListener {
            startActivity(Intent(requireContext(), SecurityActivity::class.java))
        }
    }
}
