package com.firstword.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.firstword.app.databinding.ActivityMainBinding
import com.firstword.app.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar as the action bar
        setSupportActionBar(binding.toolbar)

        // Check authentication first
        checkAuthState()

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Set up the bottom navigation with nav controller
        binding.bottomNavigation.setupWithNavController(navController)

        // Set up the ActionBar with nav controller
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.feedFragment,
                R.id.mapFragment,
                R.id.profileFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Redirect to AuthActivity if not authenticated
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish() // Finish MainActivity so user can't go back
        }
    }

    override fun onStart() {
        super.onStart()
        // Optional: Check auth state again when activity resumes
        val currentUser = auth.currentUser
        if (currentUser == null) {
            checkAuthState()
        }
    }
    // In MainActivity.kt, add this to onCreate():
    private fun handlePendingActions() {
        val pendingAction = intent.getStringExtra("pending_action")
        val pendingPostId = intent.getStringExtra("pending_post_id")

        if (pendingAction != null && pendingPostId != null) {
            // Show a message or automatically perform the action
            Toast.makeText(this, "You can now interact with posts!", Toast.LENGTH_SHORT).show()

            // Clear the extras so they're not processed again
            intent.removeExtra("pending_action")
            intent.removeExtra("pending_post_id")
        }
    }
}