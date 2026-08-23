package com.example.finalproj

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.finalproj.data.AppDatabase
import com.example.finalproj.data.FamilyMember
import com.example.finalproj.utils.ChannelSpinnerAdapter
import com.example.finalproj.utils.NotificationChannel
import com.google.android.material.appbar.MaterialToolbar
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class AddMemberActivity : AppCompatActivity() {

    private lateinit var etMemberName: EditText
    private lateinit var etMemberPhone: EditText
    private lateinit var etMemberEmail: EditText
    private lateinit var spinnerChannel: Spinner
    private lateinit var btnSaveMember: Button
    private lateinit var ivProfileImage: CircleImageView
    private lateinit var btnSelectImage: View

    private lateinit var channels: List<NotificationChannel>
    private var selectedImageUri: Uri? = null
    private var currentImagePath: String? = null
    private var memberId: Int = -1

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            ivProfileImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_member)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etMemberName = findViewById(R.id.etMemberName)
        etMemberPhone = findViewById(R.id.etMemberPhone)
        etMemberEmail = findViewById(R.id.etMemberEmail)
        spinnerChannel = findViewById(R.id.spinnerChannel)
        btnSaveMember = findViewById(R.id.btnSaveMember)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)

        channels = listOf(
            NotificationChannel("WhatsApp", R.drawable.ic_whatsapp),
            NotificationChannel("Email", R.drawable.ic_email),
            NotificationChannel("SMS", R.drawable.ic_sms)
        )

        val adapter = ChannelSpinnerAdapter(this, channels)
        spinnerChannel.adapter = adapter

        memberId = intent.getIntExtra("memberId", -1)
        if (memberId != -1) {
            toolbar.title = "Edit Member"
            loadMemberData()
        }

        btnSelectImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        btnSaveMember.setOnClickListener {
            saveMember()
        }
    }

    private fun loadMemberData() {
        this.lifecycleScope.launch {
            val member = withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(applicationContext)
                    .familyMemberDao().getMemberById(memberId)
            }
            
            member?.let {
                etMemberName.setText(it.name)
                etMemberPhone.setText(it.phone)
                etMemberEmail.setText(it.email)
                currentImagePath = it.profileImagePath
                
                val channelIndex = channels.indexOfFirst { c -> c.name == it.notifyChannel }
                if (channelIndex != -1) spinnerChannel.setSelection(channelIndex)

                if (!it.profileImagePath.isNullOrEmpty()) {
                    ivProfileImage.setImageURI(Uri.fromFile(File(it.profileImagePath)))
                }
            }
        }
    }

    private fun saveMember() {
        val name = etMemberName.text.toString().trim()
        val phone = etMemberPhone.text.toString().trim()
        val email = etMemberEmail.text.toString().trim()
        val selectedChannel = channels[spinnerChannel.selectedItemPosition].name

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter member name", Toast.LENGTH_SHORT).show()
            return
        }

        this.lifecycleScope.launch(Dispatchers.IO) {
            val imagePath = selectedImageUri?.let { uri ->
                saveImageToInternalStorage(uri)
            } ?: currentImagePath

            val familyMember = FamilyMember(
                id = if (memberId == -1) 0 else memberId,
                name = name,
                phone = phone,
                email = email,
                profileImagePath = imagePath,
                notifyChannel = selectedChannel,
                cloudId = null
            )

            AppDatabase.getDatabase(applicationContext).familyMemberDao().insert(familyMember)

            withContext(Dispatchers.Main) {
                val message = if (memberId == -1) "Member Added" else "Member Updated"
                Toast.makeText(this@AddMemberActivity, message, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
