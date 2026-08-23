package com.example.finalproj.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: CircleImageView
    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textPhone: TextView
    private lateinit var btnEditProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImage = findViewById(R.id.profileImageView)
        textName = findViewById(R.id.textName)
        textEmail = findViewById(R.id.textEmail)
        textPhone = findViewById(R.id.textPhone)
        btnEditProfile = findViewById(R.id.btnEditProfile)

        btnEditProfile.setOnClickListener {
            startActivity(
                Intent(this, CreateProfileActivity::class.java).apply {
                    putExtra(
                        CreateProfileActivity.EXTRA_MODE,
                        CreateProfileActivity.MODE_EDIT
                    )
                }
            )
        }

        loadProfile()
    }

    private fun loadProfile() {

        lifecycleScope.launch {

            val profile = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext)
                    .userProfileDao()
                    .getProfile()
            }

            if (profile != null) {

                textName.text = profile.fullName
                textEmail.text = profile.email
                textPhone.text = profile.phone

                profile.profileImagePath?.let { path ->
                    try {
                        val uri = Uri.parse(path)
                        profileImage.setImageURI(uri)
                    } catch (e: Exception) {
                        profileImage.setImageResource(
                            android.R.drawable.sym_def_app_icon
                        )
                    }
                }

            } else {
                // No profile created yet
                textName.text = "No Profile Found"
                textEmail.text = ""
                textPhone.text = ""
                profileImage.setImageResource(
                    android.R.drawable.sym_def_app_icon
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }
}