package com.example.finalproj.security

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.finalproj.R
import com.example.finalproj.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var textDetailInfo: TextView
    private lateinit var imageFront: ImageView
    private lateinit var imageBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_detail)

        textDetailInfo = findViewById(R.id.textDetailInfo)
        imageFront = findViewById(R.id.imageFront)
        imageBack = findViewById(R.id.imageBack)

        val logId = intent.getIntExtra("log_id", 0)

        CoroutineScope(Dispatchers.IO).launch {

            val log = AppDatabase.getDatabase(applicationContext)
                .intruderLogDao()
                .getLogById(logId)

            withContext(Dispatchers.Main) {

                val formattedDate = SimpleDateFormat(
                    "dd MMM yyyy - hh:mm:ss a",
                    Locale.getDefault()
                ).format(Date(log.timestamp))

                textDetailInfo.text =
                    "Attempt: ${log.attemptNumber}\n" +
                            "Date: $formattedDate\n" +
                            "Device: ${log.deviceModel}"

                imageFront.setImageBitmap(BitmapFactory.decodeFile(log.frontImagePath))
                imageBack.setImageBitmap(BitmapFactory.decodeFile(log.backImagePath))
            }
        }
    }
}