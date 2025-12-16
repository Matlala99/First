package com.firstword.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.firstword.app.R
import com.firstword.app.databinding.ActivityFollowersFollowingBinding
import com.firstword.app.ui.adapters.UserAdapter
import com.google.firebase.auth.FirebaseAuth

class FollowersFollowingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFollowersFollowingBinding
    private val viewModel: FollowersViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    private var userId: String = ""
    private var currentUserId: String = ""
    private var isFollowersTab = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowersFollowingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID") ?: FirebaseAuth.getInstance().currentUser?.uid ?: ""
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        isFollowersTab = intent.getBooleanExtra("SHOW_FOLLOWING", true).not()

        setupToolbar()
        setupRecyclerView()
        setupTabs()
        setupObservers()
        setupClickListeners()

        // Load initial data
        loadData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        updateToolbarTitle()
    }

    private fun updateToolbarTitle() {
        val title = when {
            userId == currentUserId && isFollowersTab -> "Your Followers"
            userId == currentUserId && !isFollowersTab -> "You're Following"
            isFollowersTab -> "Followers"
            else -> "Following"
        }
        supportActionBar?.title = title
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter(
            users = emptyList(),
            currentUserId = currentUserId,
            onUserClick = { user ->
                // For now, just show a toast
                android.widget.Toast.makeText(
                    this,
                    "Clicked on ${user.displayName}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            },
            onFollowClick = { user ->
                viewModel.toggleFollow(user.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@FollowersFollowingActivity)
            adapter = userAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    private fun setupTabs() {
        updateTabUI(isFollowersTab)

        binding.tabFollowers.setOnClickListener {
            if (!isFollowersTab) {
                isFollowersTab = true
                updateTabUI(true)
                updateToolbarTitle()
                loadData()
            }
        }

        binding.tabFollowing.setOnClickListener {
            if (isFollowersTab) {
                isFollowersTab = false
                updateTabUI(false)
                updateToolbarTitle()
                loadData()
            }
        }
    }

    private fun updateTabUI(isFollowersSelected: Boolean) {
        if (isFollowersSelected) {
            binding.tabFollowers.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
            binding.tabFollowers.setTextColor(
                ContextCompat.getColor(this, android.R.color.white)
            )
            binding.tabFollowing.setBackgroundColor(
                ContextCompat.getColor(this, R.color.gray_200)
            )
            binding.tabFollowing.setTextColor(
                ContextCompat.getColor(this, android.R.color.black)
            )
        } else {
            binding.tabFollowers.setBackgroundColor(
                ContextCompat.getColor(this, R.color.gray_200)
            )
            binding.tabFollowers.setTextColor(
                ContextCompat.getColor(this, android.R.color.black)
            )
            binding.tabFollowing.setBackgroundColor(
                ContextCompat.getColor(this, R.color.primary)
            )
            binding.tabFollowing.setTextColor(
                ContextCompat.getColor(this, android.R.color.white)
            )
        }
    }

    private fun setupObservers() {
        viewModel.users.observe(this) { users ->
            userAdapter.updateUsers(users)
            binding.textEmpty.visibility = if (users.isEmpty()) View.VISIBLE else View.GONE
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (!isLoading) {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                binding.textError.text = it
                binding.layoutError.visibility = View.VISIBLE
            } ?: run {
                binding.layoutError.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        binding.buttonRetry.setOnClickListener {
            loadData()
        }
    }

    private fun loadData() {
        if (isFollowersTab) {
            viewModel.loadFollowers(userId)
        } else {
            viewModel.loadFollowing(userId)
        }
    }

    private fun refreshData() {
        loadData()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        fun newIntent(context: android.content.Context, userId: String): Intent {
            return Intent(context, FollowersFollowingActivity::class.java).apply {
                putExtra("USER_ID", userId)
            }
        }
    }
}