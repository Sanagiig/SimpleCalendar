package com.example.simple_calendar.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.simple_calendar.R
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import com.example.simple_calendar.databinding.ActivityMainBinding
import com.simplemobiletools.commons.extensions.viewBinding

class MainActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator_layout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun refreshItems() {
        TODO("Not yet implemented")
    }
}