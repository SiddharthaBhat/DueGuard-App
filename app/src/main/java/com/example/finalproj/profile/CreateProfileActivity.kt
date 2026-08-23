package com.example.finalproj.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.UserProfile
import com.example.finalproj.home.HomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CreateProfileActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_CREATE = "create"
        const val MODE_EDIT = "edit"
    }

    private lateinit var profileImage: CircleImageView
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSaveProfile: MaterialButton
    private lateinit var btnSelectImage: MaterialButton

    private var selectedImagePath: String? = null
    private var currentMode = MODE_CREATE

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImagePath = saveImageToInternalStorage(uri)
                    profileImage.setImageURI(Uri.fromFile(File(selectedImagePath!!)))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_profile)

        profileImage = findViewById(R.id.profileImageView)
        etFullName = findViewById(R.id.etFullName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        btnSelectImage = findViewById(R.id.btnSelectImage)

        currentMode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CREATE

        if (currentMode == MODE_EDIT) {
            loadExistingProfile()
        }

        btnSelectImage.setOnClickListener {
            openGallery()
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun saveProfile() {

        val name = etFullName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        if (name.isEmpty()) {
            etFullName.setError("Enter your full name")
            etFullName.requestFocus()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email address")
            etEmail.requestFocus()
            return
        }

        if (phone.length != 10 || !phone.all { it.isDigit() }) {
            etPhone.setError("Enter valid 10 digit phone number")
            etPhone.requestFocus()
            return
        }

        lifecycleScope.launch {

            try {
                val dao = AppDatabase.getDatabase(applicationContext)
                    .userProfileDao()

                val existingProfile = withContext(Dispatchers.IO) {
                    dao.getProfile()
                }

                val finalImagePath =
                    selectedImagePath ?: existingProfile?.profileImagePath

                val profile = UserProfile(
                    id = 1,
                    fullName = name,
                    email = email,
                    phone = phone,
                    profileImagePath = finalImagePath
                )

                withContext(Dispatchers.IO) {
                    dao.insertOrUpdate(profile)
                }

                Toast.makeText(
                    this@CreateProfileActivity,
                    if (currentMode == MODE_EDIT)
                        "Profile Updated"
                    else
                        "Profile Created",
                    Toast.LENGTH_SHORT
                ).show()

                if (currentMode == MODE_CREATE) {
                    // Redirect to Home after creation
                    val intent = Intent(this@CreateProfileActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    finish()
                }

            } catch (e: Exception) {

                Toast.makeText(
                    this@CreateProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun loadExistingProfile() {

        lifecycleScope.launch {

            val profile = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext)
                    .userProfileDao()
                    .getProfile()
            }

            profile?.let {

                etFullName.setText(it.fullName)
                etEmail.setText(it.email)
                etPhone.setText(it.phone)

                selectedImagePath = it.profileImagePath

                it.profileImagePath?.let { path ->
                    val file = File(path)
                    if (file.exists()) {
                        profileImage.setImageURI(Uri.fromFile(file))
                    }
                }
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String {

        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "profile_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, fileName)

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }
}