package com.firstword.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstword.app.databinding.FragmentFeedBinding
import com.google.android.material.snackbar.Snackbar

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

        setupRecyclerView()
        setupObservers()
        setupSwipeRefresh()

        binding.buttonRetry.setOnClickListener {
            viewModel.refreshPosts()
        }
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
            adapter.updatePosts(posts)

            if (posts.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerViewPosts.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerViewPosts.visibility = View.VISIBLE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = isLoading
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                binding.textError.text = it
                binding.layoutError.visibility = View.VISIBLE
            } ?: run {
                binding.layoutError.visibility = View.GONE
            }
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }

    private fun handlePostAction(post: com.firstword.app.models.Post, action: String) {
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
            }
            FeedAdapter.ACTION_SHARE -> {
                // Share post
            }
            FeedAdapter.ACTION_VIEW_PROFILE -> {
                // Navigate to profile
            }
            FeedAdapter.ACTION_VIEW_IMAGE -> {
                // Show full image
            }
            FeedAdapter.ACTION_FOLLOW -> {
                // Handle follow action (already handled in adapter)
            }
            FeedAdapter.ACTION_MENU -> {
                // Show post menu options
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