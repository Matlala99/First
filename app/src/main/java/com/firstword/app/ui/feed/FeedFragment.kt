package com.firstword.app.ui.feed

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstword.app.R
import com.firstword.app.databinding.FragmentFeedBinding
import com.firstword.app.models.Post
import com.firstword.app.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class FeedFragment : Fragment() {

    companion object {
        private const val TAG = "FeedFragment"
        private const val POST_LIMIT = 50
        private val AUTH_REQUIRED_ACTIONS = setOf("like", "comment", "vote_true", "vote_fake", "vote_ai")
    }

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var feedAdapter: FeedAdapter
    private val posts = mutableListOf<Post>()
    private var postsListener: ListenerRegistration? = null
    private var currentCategory = "All"

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

        initializeFirebase()
        setupUI()
        loadFeed()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
    }

    private fun setupUI() {
        setupRecyclerView()
        setupSwipeRefresh()
        setupFab()
        setupCategoryFilter()
        checkUserAuthentication()
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter(posts) { post, action ->
            handlePostAction(post, action)
        }

        binding.recyclerViewFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            setHasFixedSize(true)
            itemAnimator = null
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadFeed()
        }

        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.primary,
            R.color.secondary,
            R.color.success
        )
    }

    private fun setupFab() {
        binding.fabCreatePost.setOnClickListener {
            handleCreatePostClick()
        }
        updateFabAppearance()
    }

    private fun handleCreatePostClick() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            navigateToAuthActivity("create_post")
        } else {
            navigateToCreatePost()
        }
    }

    private fun setupCategoryFilter() {
        val categories = resources.getStringArray(R.array.feed_categories)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerCategory.adapter = adapter

        binding.spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentCategory = categories[position]
                filterPostsByCategory(currentCategory)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun checkUserAuthentication() {
        val isAuthenticated = auth.currentUser != null
        updateFabAppearance(isAuthenticated)
    }

    private fun updateFabAppearance(isAuthenticated: Boolean = auth.currentUser != null) {
        binding.fabCreatePost.alpha = if (isAuthenticated) 1.0f else 0.7f
        binding.fabCreatePost.isEnabled = isAuthenticated
    }

    private fun loadFeed() {
        if (!isAdded) return

        binding.swipeRefreshLayout.isRefreshing = true
        binding.emptyState.visibility = View.GONE

        postsListener?.remove()

        val query = firestore.collection("posts")
            .whereEqualTo("isHidden", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(POST_LIMIT.toLong())

        setupPostsListener(query)
    }

    private fun filterPostsByCategory(category: String) {
        if (category == "All") {
            loadFeed()
            return
        }

        if (!isAdded) return

        binding.swipeRefreshLayout.isRefreshing = true
        binding.emptyState.visibility = View.GONE

        postsListener?.remove()

        val query = firestore.collection("posts")
            .whereEqualTo("category", category.lowercase())
            .whereEqualTo("isHidden", false)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(POST_LIMIT.toLong())

        setupPostsListener(query)
    }

    private fun setupPostsListener(query: Query) {
        postsListener = query.addSnapshotListener { documents, error ->
            binding.swipeRefreshLayout.isRefreshing = false

            if (error != null) {
                Log.e(TAG, "Error loading posts: ${error.message}", error)
                showError("Failed to load feed")
                showEmptyState()
                return@addSnapshotListener
            }

            posts.clear()
            documents?.forEach { document ->
                try {
                    val post = document.toObject(Post::class.java).copy(id = document.id)
                    posts.add(post)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing post document: ${document.id}", e)
                }
            }

            feedAdapter.updatePosts(posts.toList())
            updateEmptyState()
        }
    }

    private fun handlePostAction(post: Post, action: String) {
        if (AUTH_REQUIRED_ACTIONS.contains(action) && auth.currentUser == null) {
            showAuthenticationRequiredMessage(action, post.id)
            return
        }

        when (action) {
            "like" -> toggleLike(post)
            "comment" -> openComments(post)
            "share" -> sharePost(post)
            "vote_true" -> voteAuthenticity(post, "true")
            "vote_fake" -> voteAuthenticity(post, "fake")
            "vote_ai" -> voteAuthenticity(post, "ai")
            "view_profile" -> viewUserProfile(post.userId)
            "view_image" -> viewImage(post)
            else -> Log.w(TAG, "Unknown post action: $action")
        }
    }

    private fun showAuthenticationRequiredMessage(action: String, postId: String) {
        Toast.makeText(context, "Please sign in to interact with posts", Toast.LENGTH_SHORT).show()
        navigateToAuthActivity("post_interaction", postId, action)
    }

    private fun navigateToAuthActivity(source: String, postId: String? = null, action: String? = null) {
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("source", source)
            postId?.let { putExtra("post_id", it) }
            action?.let { putExtra("action", it) }
            putExtra("return_to_feed", true)
        }

        startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun navigateToCreatePost() {
        try {
            findNavController().navigate(R.id.action_feedFragment_to_createPostFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to create post failed", e)
            showQuickPostDialog()
        }
    }

    private fun showQuickPostDialog() {
        Toast.makeText(
            context,
            "Create post feature will be available in the next update",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun toggleLike(post: Post) {
        val currentUser = auth.currentUser ?: return
        val postRef = firestore.collection("posts").document(post.id)
        val likeRef = firestore.collection("likes").document("${post.id}_${currentUser.uid}")

        likeRef.get().addOnSuccessListener { document ->
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(postRef)
                val currentPost = snapshot.toObject(Post::class.java) ?: return@runTransaction

                if (document.exists()) {
                    transaction.delete(likeRef)
                    transaction.update(postRef, "likesCount", (currentPost.likesCount - 1).coerceAtLeast(0))
                } else {
                    val likeData = mapOf(
                        "postId" to post.id,
                        "userId" to currentUser.uid,
                        "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                    transaction.set(likeRef, likeData)
                    transaction.update(postRef, "likesCount", currentPost.likesCount + 1)
                }
            }.addOnSuccessListener {
                // Success handled by listener
            }.addOnFailureListener { e ->
                Log.e(TAG, "Toggle like failed", e)
                showError("Failed to toggle like")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Like check failed", e)
            showError("Failed to check like status")
        }
    }

    private fun voteAuthenticity(post: Post, voteType: String) {
        val currentUser = auth.currentUser ?: return
        val voteRef = firestore.collection("authenticity_votes").document("${post.id}_${currentUser.uid}")
        val postRef = firestore.collection("posts").document(post.id) // Fixed: Added postRef definition

        voteRef.get().addOnSuccessListener { document ->
            firestore.runTransaction { transaction ->
                val postSnapshot = transaction.get(postRef)
                val currentPost = postSnapshot.toObject(Post::class.java) ?: return@runTransaction

                val oldVote = if (document.exists()) document.getString("voteType") else null
                val votes = currentPost.authenticityVotes

                // Calculate new counts
                var trueCount = votes.trueCount
                var fakeCount = votes.fakeCount
                var aiCount = votes.aiCount

                // Remove old vote
                when (oldVote) {
                    "true" -> trueCount--
                    "fake" -> fakeCount--
                    "ai" -> aiCount--
                }

                // Add new vote
                when (voteType) {
                    "true" -> trueCount++
                    "fake" -> fakeCount++
                    "ai" -> aiCount++
                }

                // Ensure counts don't go negative
                trueCount = trueCount.coerceAtLeast(0)
                fakeCount = fakeCount.coerceAtLeast(0)
                aiCount = aiCount.coerceAtLeast(0)

                // Update vote document
                val voteData = mapOf(
                    "postId" to post.id,
                    "userId" to currentUser.uid,
                    "voteType" to voteType,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                transaction.set(voteRef, voteData)

                // Update post counts
                transaction.update(
                    postRef,
                    mapOf(
                        "authenticityVotes.trueCount" to trueCount,
                        "authenticityVotes.fakeCount" to fakeCount,
                        "authenticityVotes.aiCount" to aiCount,
                        "authenticityVotes.totalCount" to (trueCount + fakeCount + aiCount)
                    )
                )
            }.addOnSuccessListener {
                Toast.makeText(context, "Vote recorded!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Log.e(TAG, "Vote failed", e)
                showError("Failed to record vote")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Vote check failed", e)
            showError("Failed to check vote status")
        }
    }

    private fun openComments(post: Post) {
        Toast.makeText(context, "Comments feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun sharePost(post: Post) {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, buildShareText(post))
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Share Post"))
        } catch (e: Exception) {
            Log.e(TAG, "Share failed", e)
            showError("Failed to share post")
        }
    }

    private fun buildShareText(post: Post): String {
        return "${post.title}\n\n${post.content}\n\n— Shared via FirstWord App"
    }

    private fun viewUserProfile(userId: String) {
        Toast.makeText(context, "Profile feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun viewImage(post: Post) {
        if (post.imageUrl.isNotEmpty()) {
            Toast.makeText(context, "Image viewer coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showError(message: String) {
        if (isAdded) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateEmptyState() {
        binding.emptyState.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        checkUserAuthentication()
    }

    override fun onPause() {
        super.onPause()
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsListener?.remove()
        _binding = null
    }
}