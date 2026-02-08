package com.firstword.app.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstword.app.R
import com.firstword.app.databinding.FragmentFeedBinding
import com.firstword.app.ui.auth.AuthActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d("FeedFragment", "onViewCreated called")

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()
        setupFloatingActionButton()
        setupRetryButton()

        // Load posts
        viewModel.loadPosts()
    }

    private fun setupRecyclerView() {
        adapter = FeedAdapter(emptyList()) { post, action ->
            handlePostAction(post, action)
        }

        binding.recyclerViewPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FeedFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            Log.d("FeedFragment", "Posts observed: ${posts.size}")
            adapter.updatePosts(posts)

            if (posts.isEmpty()) {
                binding.layoutEmpty.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
                binding.recyclerViewPosts.visibility = View.GONE
            } else {
                binding.layoutEmpty.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.recyclerViewPosts.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val isLoadingBoolean = isLoading ?: false
            binding.progressBar.visibility = if (isLoadingBoolean) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = isLoadingBoolean
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.e("FeedFragment", "Error observed: $it")
                showError(it)
                binding.textError.text = it
                binding.layoutError.visibility = View.VISIBLE
                binding.recyclerViewPosts.visibility = View.GONE
                binding.layoutEmpty.visibility = View.GONE
            } ?: run {
                binding.layoutError.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        val neonBlueColor = ContextCompat.getColor(requireContext(), R.color.neon_blue)
        binding.swipeRefresh.setColorSchemeColors(neonBlueColor)

        binding.swipeRefresh.setOnRefreshListener {
            Log.d("FeedFragment", "Swipe refresh triggered")
            viewModel.refreshPosts()
        }
    }

    private fun setupFloatingActionButton() {
        Log.d("FeedFragment", "Setting up FAB")

        binding.fabCreatePost.setOnClickListener {
            Log.d("FeedFragment", "FAB clicked")

            // Check authentication
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                // User not authenticated - show sign in prompt
                showSignInPrompt()
            } else {
                // User authenticated - try to navigate
                navigateToCreatePost()
            }
        }

        // Make sure FAB is visible
        binding.fabCreatePost.visibility = View.VISIBLE
        binding.fabCreatePost.isEnabled = true
    }

    private fun showSignInPrompt() {
        Snackbar.make(binding.root, "Please sign in to create posts", Snackbar.LENGTH_LONG)
            .setAction("SIGN IN") {
                val intent = Intent(requireContext(), AuthActivity::class.java)
                startActivity(intent)
            }
            .show()
    }

    private fun navigateToCreatePost() {
        try {
            // Try to navigate to create post fragment
            findNavController().navigate(R.id.createPostFragment)
            Log.d("FeedFragment", "Successfully navigated to createPostFragment")
        } catch (e: Exception) {
            Log.e("FeedFragment", "Navigation failed: ${e.message}", e)
            Snackbar.make(binding.root, "Cannot create post right now", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupRetryButton() {
        binding.buttonRetry.setOnClickListener {
            Log.d("FeedFragment", "Retry button clicked")
            viewModel.refreshPosts()
        }
    }

    private fun handlePostAction(post: com.firstword.app.models.Post, action: String) {
        Log.d("FeedFragment", "Post action: $action for post: ${post.id}")
        when (action) {
            FeedAdapter.ACTION_LIKE -> {
                viewModel.likePost(post.id)
            }
            FeedAdapter.ACTION_VOTE_TRUE -> {
                viewModel.voteAuthenticity(post.id, "authentic")
            }
            FeedAdapter.ACTION_VOTE_FAKE -> {
                viewModel.voteAuthenticity(post.id, "inauthentic")
            }
            FeedAdapter.ACTION_VOTE_AI -> {
                viewModel.voteAuthenticity(post.id, "unsure")
            }
            FeedAdapter.ACTION_COMMENT -> {
                // Navigate to comments
                Snackbar.make(binding.root, "Comment on post: ${post.id}", Snackbar.LENGTH_SHORT).show()
            }
            FeedAdapter.ACTION_SHARE -> {
                // Share post
                Snackbar.make(binding.root, "Share post: ${post.id}", Snackbar.LENGTH_SHORT).show()
            }
            FeedAdapter.ACTION_VIEW_PROFILE -> {
                // Navigate to profile
                Snackbar.make(binding.root, "View profile: ${post.userDisplayName}", Snackbar.LENGTH_SHORT).show()
            }
            FeedAdapter.ACTION_VIEW_IMAGE -> {
                // Show full image
                if (post.imageUrl.isNotEmpty()) {
                    Snackbar.make(binding.root, "View image", Snackbar.LENGTH_SHORT).show()
                }
            }
            FeedAdapter.ACTION_FOLLOW -> {
                // Handle follow action (already handled in adapter)
                Snackbar.make(binding.root, "Follow action for user: ${post.userId}", Snackbar.LENGTH_SHORT).show()
            }
            FeedAdapter.ACTION_MENU -> {
                // Show post menu options
                Snackbar.make(binding.root, "Post menu for: ${post.id}", Snackbar.LENGTH_SHORT).show()
            }
            else -> {
                Log.w("FeedFragment", "Unknown action: $action")
            }
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Retry") {
                viewModel.refreshPosts()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}