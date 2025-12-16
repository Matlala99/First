package com.firstword.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.firstword.app.R
import com.firstword.app.databinding.ActivityEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var currentUser: com.firstword.app.models.User? = null
    private var selectedAvatarUri: Uri? = null
    private var selectedBannerUri: Uri? = null

    // Available interests for suggestions
    private val availableInterests = listOf(
        "Technology", "Sports", "Music", "Movies", "Gaming",
        "Science", "Art", "Travel", "Food", "Fashion",
        "Business", "Education", "Health", "Politics", "Environment"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                when (selectedImageType) {
                    "avatar" -> {
                        selectedAvatarUri = uri
                        Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .into(binding.imageAvatar)
                    }
                    "banner" -> {
                        selectedBannerUri = uri
                        Glide.with(this)
                            .load(uri)
                            .into(binding.imageBanner)
                    }
                    else -> {} // Add an else branch to make when exhaustive
                }
            }
        }
    }

    private var selectedImageType = "avatar"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Edit Profile"

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupUI()
        loadUserData()
    }

    private fun setupUI() {
        // Setup interests autocomplete
        val interestsAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            availableInterests
        )
        binding.editInterests.setAdapter(interestsAdapter)

        // Profile image pickers
        binding.buttonChangeAvatar.setOnClickListener {
            selectedImageType = "avatar"
            openImagePicker()
        }

        binding.imageAvatar.setOnClickListener {
            selectedImageType = "avatar"
            openImagePicker()
        }

        binding.buttonChangeBanner.setOnClickListener {
            selectedImageType = "banner"
            openImagePicker()
        }

        binding.imageBanner.setOnClickListener {
            selectedImageType = "banner"
            openImagePicker()
        }

        // Theme selection
        binding.radioGroupTheme.setOnCheckedChangeListener { group, checkedId ->
            val theme = when (checkedId) {
                R.id.radio_light -> "light"
                R.id.radio_dark -> "dark"
                R.id.radio_system -> "system"
                else -> "system" // Default to system
            }
            currentUser = currentUser?.copy(theme = theme)
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    currentUser = document.toObject(com.firstword.app.models.User::class.java)
                    currentUser?.let { displayUserData(it) }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayUserData(user: com.firstword.app.models.User) {
        binding.editDisplayName.setText(user.displayName)
        binding.editBio.setText(user.bio)
        binding.editLocation.setText(user.location)
        binding.editWebsite.setText(user.website)

        // Display interests
        binding.editInterests.setText(user.interests.joinToString(", "))

        // Display theme
        when (user.theme) {
            "light" -> binding.radioLight.isChecked = true
            "dark" -> binding.radioDark.isChecked = true
            "system" -> binding.radioSystem.isChecked = true
        }

        // Load images
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .circleCrop()
                .into(binding.imageAvatar)
        }

        if (user.bannerUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.bannerUrl)
                .into(binding.imageBanner)
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_profile, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_save -> {
                saveProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return
        currentUser ?: return

        binding.progressBar.visibility = View.VISIBLE

        // Update user object with form data
        val updatedUser = currentUser!!.copy(
            displayName = binding.editDisplayName.text.toString().trim(),
            bio = binding.editBio.text.toString().trim(),
            location = binding.editLocation.text.toString().trim(),
            website = binding.editWebsite.text.toString().trim(),
            interests = binding.editInterests.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        )

        // Upload images if selected
        val uploadTasks = mutableListOf<com.google.android.gms.tasks.Task<Uri>>()

        selectedAvatarUri?.let { uri ->
            uploadTasks.add(uploadImage(uri, "avatars/$userId.jpg"))
        }

        selectedBannerUri?.let { uri ->
            uploadTasks.add(uploadImage(uri, "banners/$userId.jpg"))
        }

        if (uploadTasks.isNotEmpty()) {
            com.google.android.gms.tasks.Tasks.whenAll(uploadTasks)
                .addOnSuccessListener {
                    // Get download URLs
                    var finalUser = updatedUser
                    uploadTasks.forEachIndexed { index, task ->
                        if (task.isSuccessful) {
                            val downloadUri = task.result
                            finalUser = if (index == 0 && selectedAvatarUri != null) {
                                finalUser.copy(avatarUrl = downloadUri.toString())
                            } else if (selectedBannerUri != null) {
                                finalUser.copy(bannerUrl = downloadUri.toString())
                            } else {
                                finalUser
                            }
                        }
                    }
                    saveUserToFirestore(finalUser)
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveUserToFirestore(updatedUser)
        }
    }

    private fun uploadImage(uri: Uri, path: String): com.google.android.gms.tasks.Task<Uri> {
        val storageRef = storage.reference.child(path)
        return storageRef.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }
    }

    private fun saveUserToFirestore(user: com.firstword.app.models.User) {
        val userId = auth.currentUser?.uid ?: return

        val updates = mapOf(
            "displayName" to user.displayName,
            "bio" to user.bio,
            "location" to user.location,
            "website" to user.website,
            "interests" to user.interests,
            "theme" to user.theme,
            "avatarUrl" to user.avatarUrl,
            "bannerUrl" to user.bannerUrl,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Set result and finish
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
            }
    }
}