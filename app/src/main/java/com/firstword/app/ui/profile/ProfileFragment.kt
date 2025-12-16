package com.firstword.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.firstword.app.R
import com.firstword.app.databinding.FragmentProfileBinding
import com.firstword.app.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private val viewModel: ProfileViewModel by viewModels()

    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.loadUserProfile()
        }
    }

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

        setupClickListeners()
        setupObservers()
        viewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        binding.buttonEditProfile.setOnClickListener {
            if (auth.currentUser != null) {
                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                editProfileLauncher.launch(intent)
            } else {
                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonSettings.setOnClickListener {
            Toast.makeText(context, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSignOut.setOnClickListener {
            signOut()
        }

        binding.layoutFollowers.setOnClickListener {
            if (auth.currentUser != null) {
                val intent = Intent(requireContext(), FollowersFollowingActivity::class.java)
                intent.putExtra("USER_ID", auth.currentUser?.uid)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutFollowing.setOnClickListener {
            if (auth.currentUser != null) {
                val intent = Intent(requireContext(), FollowersFollowingActivity::class.java)
                intent.putExtra("USER_ID", auth.currentUser?.uid)
                intent.putExtra("SHOW_FOLLOWING", true)
                startActivity(intent)
            } else {
                Toast.makeText(context, "Please sign in first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutPosts.setOnClickListener {
            Toast.makeText(context, "User posts feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.buttonSignIn.setOnClickListener {
            val intent = Intent(requireContext(), AuthActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupObservers() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let { displayUserProfile(it) }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayUserProfile(user: com.firstword.app.models.User) {
        binding.layoutSignedIn.visibility = View.VISIBLE
        binding.layoutSignInPrompt.visibility = View.GONE

        // User info
        binding.textDisplayName.text = user.displayName.ifEmpty { user.handle }
        binding.textHandle.text = "@${user.handle}"
        binding.textBio.text = user.bio // FIXED: Using binding.textBio

        // Load avatar
        if (user.avatarUrl.isNotEmpty()) {
            Glide.with(this)
                .load(user.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(binding.imageAvatar)
        } else {
            binding.imageAvatar.setImageResource(R.drawable.ic_profile)
        }

        // Stats
        binding.textFollowersCount.text = formatCount(user.followersCount)
        binding.textFollowingCount.text = formatCount(user.followingCount)
        binding.textPostsCount.text = formatCount(user.postsCount)

        // Guest user indicator
        binding.textGuestIndicator.visibility = if (user.isGuest) View.VISIBLE else View.GONE

        // Interests
        if (user.interests.isNotEmpty()) {
            binding.textInterests.text = user.interests.joinToString(", ")
            binding.layoutInterests.visibility = View.VISIBLE
        } else {
            binding.layoutInterests.visibility = View.GONE
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count < 1000 -> count.toString()
            count < 1000000 -> String.format("%.1fk", count / 1000.0)
            else -> String.format("%.1fM", count / 1000000.0)
        }
    }

    private fun showSignInPrompt() {
        binding.layoutSignedIn.visibility = View.GONE
        binding.layoutSignInPrompt.visibility = View.VISIBLE
    }

    private fun signOut() {
        auth.signOut()

        // Navigate to auth screen
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_EDIT_PROFILE = 1001
    }
}