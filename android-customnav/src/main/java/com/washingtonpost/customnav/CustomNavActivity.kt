package com.washingtonpost.customnav

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.wapo.android.commons.util.applySystemBarPadding
import com.washingtonpost.customnav.databinding.ActivityCustomNavBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CustomNavActivity: AppCompatActivity() {

    private lateinit var binding: ActivityCustomNavBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.enableEdgeToEdge(window)
        super.onCreate(savedInstanceState)
        binding = ActivityCustomNavBinding.inflate(layoutInflater)
        binding.root.applySystemBarPadding()
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}