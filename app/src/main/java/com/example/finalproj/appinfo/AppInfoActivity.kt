package com.example.finalproj.appinfo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.finalproj.R

class AppInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_info)

        supportActionBar?.title = "App Info"
    }
}