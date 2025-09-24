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
import java.util.*

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var searchAdapter: FeedAdapter
    private val searchResults = mutableListOf<Post>()

    // Recent searches shared preferences key
    private val recentSearchesKey = "recent_searches"
    private val maxRecentSearches = 5

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
        setupRecentSearches()
        showInitialState()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.isNotBlank()) {
                        searchPosts(it)
                        addToRecentSearches(it)
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    showInitialState()
                } else if (newText.length >= 2) {
                    // Real-time search as user types (optional - can be heavy)
                    // searchPosts(newText)
                }
                return true
            }
        })
    }

    private fun setupRecyclerView() {
        searchAdapter = FeedAdapter(searchResults) { post, action ->
            handlePostAction(post, action)
        }

        binding.recyclerViewSearch.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
        }
    }

    private fun setupRecentSearches() {
        val recentSearches = getRecentSearches()
        if (recentSearches.isNotEmpty()) {
            binding.recyclerViewRecentSearches.visibility = View.VISIBLE
            binding.textRecentSearches.visibility = View.VISIBLE

            val recentAdapter = RecentSearchesAdapter(recentSearches) { query ->
                binding.searchView.setQuery(query, true)
            }

            binding.recyclerViewRecentSearches.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = recentAdapter
            }
        } else {
            binding.recyclerViewRecentSearches.visibility = View.GONE
            binding.textRecentSearches.visibility = View.GONE
        }
    }

    private fun searchPosts(query: String) {
        if (query.length < 2) {
            Toast.makeText(context, "Please enter at least 2 characters", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE

        // Clear previous results
        searchResults.clear()
        searchAdapter.notifyDataSetChanged()

        val searchQuery = query.lowercase().trim()

        firestore.collection("posts")
            .whereEqualTo("isHidden", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                for (document in documents) {
                    try {
                        val post = document.toObject(Post::class.java)

                        // Create a safe post object with proper null handling
                        val safePost = createSafePost(post, document.id)

                        // Enhanced search across multiple fields
                        val matches = listOf(
                            safePost.title.lowercase().contains(searchQuery),
                            safePost.content.lowercase().contains(searchQuery),
                            safePost.category.lowercase().contains(searchQuery),
                            safePost.userDisplayName.lowercase().contains(searchQuery),
                            safePost.userHandle.lowercase().contains(searchQuery),
                            safePost.newsSource.lowercase().contains(searchQuery),
                            // Search in tags array
                            safePost.tags.any { tag -> tag.lowercase().contains(searchQuery) }
                        ).any { it }

                        if (matches) {
                            searchResults.add(safePost)
                        }
                    } catch (e: Exception) {
                        // Skip posts that can't be parsed properly
                        println("Error parsing post ${document.id}: ${e.message}")
                    }
                }

                binding.progressBar.visibility = View.GONE

                if (searchResults.isEmpty()) {
                    showEmptyResults()
                } else {
                    showSearchResults()
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
            }
    }

    private fun createSafePost(post: Post, documentId: String): Post {
        return Post(
            id = documentId,
            title = post.title ?: "",
            content = post.content ?: "",
            imageUrl = post.imageUrl ?: "",
            videoUrl = post.videoUrl ?: "",
            userId = post.userId ?: "",
            userHandle = post.userHandle ?: "",
            userDisplayName = post.userDisplayName ?: "",
            userAvatarUrl = post.userAvatarUrl ?: "",
            category = post.category ?: "general",
            sourceUrl = post.sourceUrl ?: "",
            location = post.location,
            tags = post.tags ?: emptyList(),
            newsSource = post.newsSource ?: "",
            likesCount = post.likesCount ?: 0,
            commentsCount = post.commentsCount ?: 0,
            sharesCount = post.sharesCount ?: 0,
            viewsCount = post.viewsCount ?: 0,
            authenticityVotes = post.authenticityVotes ?: com.firstword.app.models.AuthenticityVotes(),
            createdAt = post.createdAt,
            updatedAt = post.updatedAt,
            isVerified = post.isVerified ?: false,
            isReported = post.isReported ?: false,
            isHidden = post.isHidden ?: false
        )
    }

    private fun handlePostAction(post: Post, action: String) {
        when (action) {
            "like", "comment", "share", "vote_true", "vote_fake", "vote_ai" -> {
                // Handle post interactions
                Toast.makeText(context, "Action: $action on post", Toast.LENGTH_SHORT).show()
            }
            "open" -> {
                // Open post detail view
                openPostDetail(post)
            }
        }
    }

    private fun openPostDetail(post: Post) {
        // Navigate to post detail fragment/activity
        Toast.makeText(context, "Opening post: ${post.title}", Toast.LENGTH_SHORT).show()
    }

    // Recent Searches Management
    private fun getRecentSearches(): List<String> {
        val sharedPref = requireContext().getSharedPreferences("search_prefs", 0)
        return sharedPref.getStringSet(recentSearchesKey, mutableSetOf())?.toList() ?: emptyList()
    }

    private fun addToRecentSearches(query: String) {
        val sharedPref = requireContext().getSharedPreferences("search_prefs", 0)
        val recentSearches = sharedPref.getStringSet(recentSearchesKey, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // Add new query and maintain size limit
        recentSearches.add(query)
        if (recentSearches.size > maxRecentSearches) {
            recentSearches.remove(recentSearches.first())
        }

        sharedPref.edit().putStringSet(recentSearchesKey, recentSearches).apply()
        setupRecentSearches() // Refresh recent searches display
    }

    private fun clearRecentSearches() {
        val sharedPref = requireContext().getSharedPreferences("search_prefs", 0)
        sharedPref.edit().remove(recentSearchesKey).apply()
        binding.recyclerViewRecentSearches.visibility = View.GONE
        binding.textRecentSearches.visibility = View.GONE
    }

    // UI State Management
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Recent Searches Adapter
class RecentSearchesAdapter(
    private val searches: List<String>,
    private val onSearchClick: (String) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<RecentSearchesAdapter.ViewHolder>() {

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val textView: android.widget.TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = searches[position]
        holder.itemView.setOnClickListener {
            onSearchClick(searches[position])
        }
    }

    override fun getItemCount(): Int = searches.size
}