package com.firstword.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.firstword.app.R
import com.firstword.app.databinding.FragmentProfileBinding
import com.firstword.app.models.User
import com.firstword.app.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {
    
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var currentUser: User? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        setupClickListeners()
        loadUserProfile()
    }
    
    private fun setupClickListeners() {
        binding.buttonEditProfile.setOnClickListener {
            // TODO: Navigate to edit profile screen
            Toast.makeText(context, "Edit profile feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.buttonSettings.setOnClickListener {
            // TODO: Navigate to settings screen
            Toast.makeText(context, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.buttonSignOut.setOnClickListener {
            signOut()
        }
        
        binding.layoutFollowers.setOnClickListener {
            // TODO: Navigate to followers screen
            Toast.makeText(context, "Followers feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.layoutFollowing.setOnClickListener {
            // TODO: Navigate to following screen
            Toast.makeText(context, "Following feature coming soon", Toast.LENGTH_SHORT).show()
        }
        
        binding.layoutPosts.setOnClickListener {
            // TODO: Navigate to user posts screen
            Toast.makeText(context, "User posts feature coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadUserProfile() {
        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            showSignInPrompt()
            return
        }
        
        binding.progressBar.visibility = View.VISIBLE
        
        firestore.collection("users").document(firebaseUser.uid)
            .get()
            .addOnSuccessListener { document ->
                binding.progressBar.visibility = View.GONE
                
                if (document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    currentUser?.let { user ->
                        displayUserProfile(user)
                    }
                } else {
                    // Create user profile if it doesn't exist
                    createUserProfile(firebaseUser)
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    context,
                    "Failed to load profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun displayUserProfile(user: User) {
        binding.layoutSignedIn.visibility = View.VISIBLE
        binding.layoutSignInPrompt.visibility = View.GONE
        
        // User info
        binding.textDisplayName.text = user.displayName.ifEmpty { user.handle }
        binding.textHandle.text = "@${user.handle}"
        
        // Load avatar
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(binding.imageAvatar)
        }
        
        // Stats
        binding.textFollowersCount.text = user.followersCount.toString()
        binding.textFollowingCount.text = user.followingCount.toString()
        binding.textPostsCount.text = user.postsCount.toString()
        
        // Guest user indicator
        if (user.isGuest) {
            binding.textGuestIndicator.visibility = View.VISIBLE
            binding.buttonEditProfile.text = "Sign Up to Save Profile"
        } else {
            binding.textGuestIndicator.visibility = View.GONE
        }
        
        // Interests
        if (user.interests.isNotEmpty()) {
            binding.textInterests.text = user.interests.joinToString(", ")
            binding.layoutInterests.visibility = View.VISIBLE
        } else {
            binding.layoutInterests.visibility = View.GONE
        }
    }
    
    private fun createUserProfile(firebaseUser: com.google.firebase.auth.FirebaseUser) {
        val user = User(
            id = firebaseUser.uid,
            handle = generateHandle(firebaseUser.displayName ?: "user"),
            displayName = firebaseUser.displayName ?: "",
            email = firebaseUser.email ?: "",
            avatarUrl = firebaseUser.photoUrl?.toString() ?: "",
            isGuest = firebaseUser.isAnonymous
        )
        
        firestore.collection("users").document(firebaseUser.uid)
            .set(user)
            .addOnSuccessListener {
                currentUser = user
                displayUserProfile(user)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Failed to create profile: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
    
    private fun showSignInPrompt() {
        binding.layoutSignedIn.visibility = View.GONE
        binding.layoutSignInPrompt.visibility = View.VISIBLE
        
        binding.buttonSignIn.setOnClickListener {
            val intent = Intent(context, AuthActivity::class.java)
            startActivity(intent)
        }
    }
    
    private fun signOut() {
        auth.signOut()
        
        // Navigate to auth screen
        val intent = Intent(context, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }
    
    private fun generateHandle(displayName: String): String {
        val baseHandle = displayName.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
        return "${baseHandle}_${System.currentTimeMillis().toString().takeLast(4)}"
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
