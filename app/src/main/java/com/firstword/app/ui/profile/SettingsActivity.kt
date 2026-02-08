package com.firstword.app.ui.profile

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firstword.app.databinding.ActivitySettingsBinding
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.btnAccountDetails.setOnClickListener {
            Toast.makeText(this, "Account Details feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnNotifications.setOnClickListener {
            Toast.makeText(this, "Notification settings coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnTheme.setOnClickListener {
            Toast.makeText(this, "Neo-Modern Dark is the default experience!", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            finishAffinity() 
            // In a real app, you'd navigate back to AuthActivity
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
