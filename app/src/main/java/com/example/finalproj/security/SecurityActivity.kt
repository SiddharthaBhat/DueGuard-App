package com.example.finalproj.security

import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.finalproj.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.Executor

class SecurityActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var switchLock: SwitchMaterial
    private lateinit var switchBiometric: SwitchMaterial
    private lateinit var switchIntruder: SwitchMaterial
    private lateinit var btnSetPin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security)

        supportActionBar?.title = "Security"

        prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)

        switchLock = findViewById(R.id.switchLock)
        switchBiometric = findViewById(R.id.switchBiometric)
        switchIntruder = findViewById(R.id.switchIntruder)
        btnSetPin = findViewById(R.id.btnSetPin)

        // Load saved states
        switchLock.isChecked = prefs.getBoolean("lock_enabled", false)
        switchBiometric.isChecked = prefs.getBoolean("biometric_enabled", false)
        switchIntruder.isChecked = prefs.getBoolean("intruder_enabled", false)

        // Initialize UI state based on dependency
        updateSecurityUI()

        // Lock toggle - Ask for PIN before changing
        switchLock.setOnClickListener {
            val targetState = switchLock.isChecked
            // Revert visually until verified
            switchLock.isChecked = !targetState
            
            verifyPin {
                switchLock.isChecked = targetState
                prefs.edit().putBoolean("lock_enabled", targetState).apply()
                updateSecurityUI()
                
                // If turning ON and no PIN set, force set PIN
                if (targetState && prefs.getString("user_pin", null) == null) {
                    showSetPinDialog()
                }
            }
        }

        // Biometric toggle - Ask for PIN before changing
        switchBiometric.setOnClickListener {
            val targetState = switchBiometric.isChecked
            switchBiometric.isChecked = !targetState

            if (targetState && !isBiometricAvailable()) {
                Toast.makeText(this, "Biometric not available on this device", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyPin {
                switchBiometric.isChecked = targetState
                prefs.edit().putBoolean("biometric_enabled", targetState).apply()
            }
        }

        // Intruder toggle
        switchIntruder.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("intruder_enabled", isChecked).apply()
        }

        // Set / Change PIN
        btnSetPin.setOnClickListener {
            verifyPin {
                showSetPinDialog()
            }
        }
    }

    private fun updateSecurityUI() {
        val isLockEnabled = switchLock.isChecked
        
        // PIN button visibility
        btnSetPin.visibility = if (isLockEnabled) View.VISIBLE else View.GONE
        
        // Biometric switch dependency
        switchBiometric.isEnabled = isLockEnabled
        
        // Intruder switch dependency: only enabled if PIN lock is on
        switchIntruder.isEnabled = isLockEnabled
        
        if (!isLockEnabled) {
            // If lock is disabled, biometric and intruder detection MUST be disabled
            switchBiometric.isChecked = false
            switchIntruder.isChecked = false
            prefs.edit().putBoolean("biometric_enabled", false).apply()
            prefs.edit().putBoolean("intruder_enabled", false).apply()
            
            switchBiometric.alpha = 0.5f
            switchIntruder.alpha = 0.5f
        } else {
            switchBiometric.alpha = 1.0f
            switchIntruder.alpha = 1.0f
        }
    }

    private fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // ===================== PIN VERIFICATION =====================
    private fun verifyPin(onSuccess: () -> Unit) {
        val savedPin = prefs.getString("user_pin", null)
        if (savedPin == null) {
            // No PIN set yet, allow access (first time setup)
            onSuccess()
            return
        }

        val view = layoutInflater.inflate(R.layout.dialog_verify_pin, null)
        val etPin = view.findViewById<TextInputEditText>(R.id.etVerifyPin)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancelVerify)
        val btnVerify = view.findViewById<MaterialButton>(R.id.btnVerifyPin)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnVerify.setOnClickListener {
            val enteredPin = etPin.text.toString().trim()
            if (enteredPin == savedPin) {
                dialog.dismiss()
                onSuccess()
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    // ===================== PIN DIALOG =====================
    private fun showSetPinDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_set_pin, null)
        val etPin = view.findViewById<TextInputEditText>(R.id.etPin)
        val etConfirm = view.findViewById<TextInputEditText>(R.id.etConfirmPin)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val pin = etPin.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (pin.length < 4) {
                Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pin != confirm) {
                Toast.makeText(this, "PINs do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit().putString("user_pin", pin).apply()
            Toast.makeText(this, "PIN saved successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}