package com.firstword.app.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstword.app.databinding.FragmentSearchBinding
import com.firstword.app.models.Post
import com.firstword.app.ui.feed.FeedAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var searchAdapter: FeedAdapter
    private val searchResults = mutableListOf<Post>()
    private val trendingKeywords = listOf(
        "Breaking News", "Politics", "Technology", "Climate", "Sports",
        "Economy", "Health", "Science", "Entertainment", "Local"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        setupSearchView()
        setupRecyclerView()
        setupTrendingKeywords()
        showInitialState()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchPosts(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    showInitialState()
                } else {
                    // Optional: Implement real-time search as user types
                    // searchPosts(newText)
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        searchAdapter = FeedAdapter(searchResults) { post, action ->
            // Handle post actions (same as feed)
            when (action) {
                "like", "comment", "share", "vote_true", "vote_fake", "vote_ai" -> {
                    Toast.makeText(context, "Please go to the feed to interact with posts", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    Toast.makeText(context, "Action: $action", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.recyclerViewSearch.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun setupTrendingKeywords() {
        // Create a simple adapter for trending keywords
        val keywordAdapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
                return object : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {}
            }

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val textView = holder.itemView as android.widget.TextView
                textView.text = trendingKeywords[position]
                textView.setPadding(16, 8, 16, 8)

                holder.itemView.setOnClickListener {
                    binding.searchView.setQuery(trendingKeywords[position], true)
                }
            }

            override fun getItemCount(): Int = trendingKeywords.size
        }

        binding.recyclerViewTrending.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = keywordAdapter
        }
    }

    private fun showInitialState() {
        binding.layoutInitialState.visibility = View.VISIBLE
        binding.layoutSearchResults.visibility = View.GONE
        binding.layoutEmptyResults.visibility = View.GONE
    }

    private fun showSearchResults() {
        binding.layoutInitialState.visibility = View.GONE
        binding.layoutSearchResults.visibility = View.VISIBLE
        binding.layoutEmptyResults.visibility = View.GONE
    }

    private fun showEmptyResults() {
        binding.layoutInitialState.visibility = View.GONE
        binding.layoutSearchResults.visibility = View.GONE
        binding.layoutEmptyResults.visibility = View.VISIBLE
    }

    private fun searchPosts(query: String) {
        if (query.length < 2) {
            Toast.makeText(context, "Please enter at least 2 characters", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.textSearchQuery.text = "Searching for \"$query\""

        // Search in multiple fields using Firestore's array-contains and string search
        firestore.collection("posts")
            .whereEqualTo("isHidden", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100) // Increase limit for better search results
            .get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                val searchQuery = query.lowercase()

                for (document in documents) {
                    val post = document.toObject(Post::class.java)

                    // Enhanced search across multiple fields
                    val matches = listOf(
                        post.title.lowercase().contains(searchQuery),
                        post.content.lowercase().contains(searchQuery),
                        post.category.lowercase().contains(searchQuery),
                        post.userDisplayName.lowercase().contains(searchQuery),
                        post.userHandle.lowercase().contains(searchQuery),
                        post.newsSource.lowercase().contains(searchQuery),
                        // Search in tags array
                        post.tags.any { tag -> tag.lowercase().contains(searchQuery) }
                    ).any { it }

                    if (matches) {
                        searchResults.add(post)
                    }
                }

                binding.progressBar.visibility = View.GONE

                if (searchResults.isEmpty()) {
                    showEmptyResults()
                    binding.textEmptyQuery.text = "No results found for \"$query\""
                } else {
                    showSearchResults()
                    binding.textResultsCount.text = "${searchResults.size} results for \"$query\""
                    searchAdapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    context,
                    "Search failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showEmptyResults()
                binding.textEmptyQuery.text = "Search error for \"$query\""
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}