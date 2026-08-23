package com.example.finalproj.security

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.finalproj.R
import com.example.finalproj.home.HomeActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executor

class LockActivity : AppCompatActivity() {

    private val DEFAULT_PIN = "1947"
    private val MAX_ATTEMPTS = 4

    private lateinit var etPin: TextInputEditText
    private lateinit var btnUnlock: MaterialButton
    private lateinit var btnBiometricUnlock: MaterialButton
    private lateinit var prefs: SharedPreferences

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        etPin = findViewById(R.id.etPin)
        btnUnlock = findViewById(R.id.btnUnlock)
        btnBiometricUnlock = findViewById(R.id.btnBiometricUnlock)

        prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)

        btnUnlock.setOnClickListener {
            handleUnlock()
        }

        setupBiometric()
    }

    private fun setupBiometric() {
        val biometricEnabled = prefs.getBoolean("biometric_enabled", false)
        if (!biometricEnabled) {
            btnBiometricUnlock.visibility = View.GONE
            return
        }

        btnBiometricUnlock.visibility = View.VISIBLE
        
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(applicationContext, "Authentication succeeded!", Toast.LENGTH_SHORT).show()
                    proceed()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for DueGuard")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use PIN")
            .build()

        btnBiometricUnlock.setOnClickListener {
            biometricPrompt.authenticate(promptInfo)
        }

        // Auto-show biometric prompt
        biometricPrompt.authenticate(promptInfo)
    }

    private fun handleUnlock() {
        val isLockEnabled = prefs.getBoolean("lock_enabled", false)
        if (!isLockEnabled) {
            proceed()
            return
        }

        val enteredPin = etPin.text?.toString()?.trim()
        val savedPin = prefs.getString("user_pin", DEFAULT_PIN)
        var wrongAttempts = prefs.getInt("wrong_attempts", 0)

        if (enteredPin == savedPin) {
            prefs.edit().putInt("wrong_attempts", 0).apply()
            proceed()
        } else {
            wrongAttempts++
            prefs.edit().putInt("wrong_attempts", wrongAttempts).apply()
            Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()

            val intruderEnabled = prefs.getBoolean("intruder_enabled", false)
            if (wrongAttempts >= MAX_ATTEMPTS && intruderEnabled) {
                val intent = Intent(this, IntruderCaptureActivity::class.java)
                intent.putExtra("attempt_number", wrongAttempts)
                startActivity(intent)
                prefs.edit().putInt("wrong_attempts", 0).apply()
            }
        }
    }

    private fun proceed() {
        val targetClassName = intent.getStringExtra("target_activity")
        val nextIntent = if (targetClassName != null) {
            Intent().setClassName(this, targetClassName).apply {
                putExtras(intent)
                putExtra("unlocked", true)
                action = intent.action
                type = intent.type
            }
        } else {
            Intent(this, HomeActivity::class.java)
        }
        startActivity(nextIntent)
        finish()
    }
}