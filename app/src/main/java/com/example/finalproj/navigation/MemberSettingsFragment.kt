package com.example.finalproj.navigation

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.finalproj.R
import com.example.finalproj.profile.ProfileActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class MemberSettingsFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var switchCloudSync: SwitchMaterial

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_member_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        prefs = requireContext().getSharedPreferences("app_settings", 0)

        // Profile navigation
        val layoutProfile = view.findViewById<View>(R.id.layoutProfile)
        layoutProfile.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        // Cloud Sync Toggle
        switchCloudSync = view.findViewById(R.id.switchCloudSync)

        // Load saved value
        val enabled = prefs.getBoolean("cloud_sync", false)
        switchCloudSync.isChecked = enabled

        // Save when changed
        switchCloudSync.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("cloud_sync", isChecked).apply()
        }
    }

    companion object {
        fun newInstance(memberId: Int): MemberSettingsFragment {
            val fragment = MemberSettingsFragment()
            val bundle = Bundle()
            bundle.putInt("memberId", memberId)
            fragment.arguments = bundle
            return fragment
        }
    }
}