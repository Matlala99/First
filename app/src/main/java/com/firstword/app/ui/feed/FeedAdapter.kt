package com.firstword.app.ui.feed

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.firstword.app.R
import com.firstword.app.databinding.ItemPostBinding
import com.firstword.app.models.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

class FeedAdapter(
    private val posts: List<Post>,
    private val onPostAction: (Post, String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    companion object {
        const val ACTION_LIKE = "like"
        const val ACTION_COMMENT = "comment"
        const val ACTION_SHARE = "share"
        const val ACTION_VOTE_TRUE = "vote_true"
        const val ACTION_VOTE_FAKE = "vote_fake"
        const val ACTION_VOTE_AI = "vote_ai"
        const val ACTION_VIEW_PROFILE = "view_profile"
        const val ACTION_VIEW_IMAGE = "view_image"
        const val ACTION_FOLLOW = "follow"
        const val ACTION_MENU = "menu"
    }

    private var items: List<Post> = posts
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    // Track following status for each user
    private val followingStatus = mutableMapOf<String, Boolean>()

    fun updatePosts(newPosts: List<Post>) {
        items = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(items.getOrNull(position))
    }

    override fun getItemCount(): Int = items.size

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post?) {
            post ?: return

            with(binding) {
                // User info
                textUserName.text = post.userDisplayName.ifEmpty { post.userHandle }
                textUserHandle.text = "@${post.userHandle}"

                // Load user avatar
                bindUserAvatar(post.userAvatarUrl)

                // Post content
                textPostTitle.text = post.title
                textPostContent.text = post.content
                textTimestamp.text = formatTimestamp(post.createdAt ?: Date())

                // Post image
                bindPostImage(post.imageUrl)

                // Engagement counts
                textLikesCount.text = formatCount(post.likesCount)
                textCommentsCount.text = formatCount(post.commentsCount)
                textSharesCount.text = formatCount(post.sharesCount)

                // Authenticity votes
                textVoteTrueCount.text = post.authenticityVotes.trueCount.toString()
                textVoteFakeCount.text = post.authenticityVotes.fakeCount.toString()
                textVoteAiCount.text = post.authenticityVotes.aiCount.toString()

                // Authenticity summary
                bindAuthenticitySummary(post)

                // Category badge
                bindCategory(post.category)

                // Voting panel visibility
                bindVotingPanel(post)

                // Follow button logic
                bindFollowButton(post)

                // Menu button logic
                bindMenuButton(post)

                // Click listeners
                setClickListeners(post)
            }
        }

        private fun bindFollowButton(post: Post) {
            val currentUserId = auth.currentUser?.uid

            // Hide follow button if:
            // 1. No user is logged in
            // 2. It's the current user's own post
            if (currentUserId == null || currentUserId == post.userId) {
                binding.buttonFollow.visibility = View.GONE
                binding.buttonMenu.visibility = View.VISIBLE
            } else {
                binding.buttonFollow.visibility = View.VISIBLE
                binding.buttonMenu.visibility = View.GONE

                // Check if already following
                checkFollowingStatus(post.userId) { isFollowing ->
                    updateFollowButtonUI(isFollowing, post.userId)
                }
            }
        }

        private fun checkFollowingStatus(targetUserId: String, callback: (Boolean) -> Unit) {
            if (currentUserId.isEmpty()) {
                callback(false)
                return
            }

            db.collection("follows")
                .document("${currentUserId}_$targetUserId")
                .get()
                .addOnSuccessListener { document ->
                    callback(document.exists())
                }
                .addOnFailureListener {
                    callback(false)
                }
        }

        private fun updateFollowButtonUI(isFollowing: Boolean, targetUserId: String) {
            if (isFollowing) {
                binding.buttonFollow.text = "Following"
                binding.buttonFollow.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                )
                binding.buttonFollow.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.white)
                )
                binding.buttonFollow.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                )
            } else {
                binding.buttonFollow.text = "Follow"
                binding.buttonFollow.setBackgroundColor(
                    ContextCompat.getColor(binding.root.context, R.color.transparent)
                )
                binding.buttonFollow.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                )
                binding.buttonFollow.strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(binding.root.context, R.color.primary)
                )
            }

            // Store in cache
            followingStatus[targetUserId] = isFollowing
        }

        private fun bindMenuButton(post: Post) {
            // Show menu button only for current user's posts
            binding.buttonMenu.visibility = if (post.userId == currentUserId) View.VISIBLE else View.GONE
        }

        private fun setClickListeners(post: Post) {
            with(binding) {
                // Engagement buttons
                buttonLike.setOnClickListener { onPostAction(post, ACTION_LIKE) }
                buttonComment.setOnClickListener { onPostAction(post, ACTION_COMMENT) }
                buttonShare.setOnClickListener { onPostAction(post, ACTION_SHARE) }
                buttonVote.setOnClickListener { toggleVotingPanel() }

                // Authenticity vote buttons
                buttonVoteTrue.setOnClickListener { onPostAction(post, ACTION_VOTE_TRUE) }
                buttonVoteFake.setOnClickListener { onPostAction(post, ACTION_VOTE_FAKE) }
                buttonVoteAi.setOnClickListener { onPostAction(post, ACTION_VOTE_AI) }

                // Follow button
                buttonFollow.setOnClickListener {
                    toggleFollow(post.userId, post.userDisplayName)
                }

                // Menu button
                buttonMenu.setOnClickListener { onPostAction(post, ACTION_MENU) }

                // User profile clicks
                imageUserAvatar.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }
                textUserName.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }
                textUserHandle.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }

                // Post image click
                imagePost.setOnClickListener { onPostAction(post, ACTION_VIEW_IMAGE) }
            }
        }

        private fun toggleFollow(targetUserId: String, targetUserName: String) {
            val currentUserId = auth.currentUser?.uid ?: return

            val isCurrentlyFollowing = followingStatus[targetUserId] ?: false
            val followRef = db.collection("follows")
                .document("${currentUserId}_$targetUserId")

            if (isCurrentlyFollowing) {
                // Unfollow
                followRef.delete()
                    .addOnSuccessListener {
                        // Update user counts
                        db.collection("users").document(currentUserId)
                            .update("followingCount", FieldValue.increment(-1))

                        db.collection("users").document(targetUserId)
                            .update("followersCount", FieldValue.increment(-1))

                        // Update UI
                        updateFollowButtonUI(false, targetUserId)

                        // Show toast
                        Toast.makeText(
                            binding.root.context,
                            "Unfollowed $targetUserName",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } else {
                // Follow
                val followData = mapOf(
                    "followerId" to currentUserId,
                    "followedId" to targetUserId,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                followRef.set(followData)
                    .addOnSuccessListener {
                        // Update user counts
                        db.collection("users").document(currentUserId)
                            .update("followingCount", FieldValue.increment(1))

                        db.collection("users").document(targetUserId)
                            .update("followersCount", FieldValue.increment(1))

                        // Update UI
                        updateFollowButtonUI(true, targetUserId)

                        // Show toast
                        Toast.makeText(
                            binding.root.context,
                            "Following $targetUserName",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Create notification for the followed user
                        createFollowNotification(targetUserId, currentUserId)
                    }
            }
        }

        private fun createFollowNotification(targetUserId: String, followerId: String) {
            // Get follower info
            db.collection("users").document(followerId).get()
                .addOnSuccessListener { followerDoc ->
                    val followerName = followerDoc.getString("displayName") ?: "Someone"

                    val notificationData = mapOf(
                        "type" to "follow",
                        "fromUserId" to followerId,
                        "toUserId" to targetUserId,
                        "message" to "$followerName started following you",
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )

                    db.collection("notifications").add(notificationData)
                }
        }

        private fun toggleVotingPanel() {
            with(binding) {
                val isVisible = layoutVoting.visibility == View.VISIBLE
                layoutVoting.visibility = if (isVisible) View.GONE else View.VISIBLE

                // Optional: Add smooth animation
                layoutVoting.alpha = if (isVisible) 0f else 1f
                layoutVoting.animate()
                    .alpha(if (isVisible) 0f else 1f)
                    .setDuration(300)
                    .start()
            }
        }

        private fun bindUserAvatar(avatarUrl: String) {
            val context = binding.root.context
            Glide.with(context)
                .load(if (avatarUrl.isNotEmpty()) avatarUrl else R.drawable.ic_profile)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.imageUserAvatar)
        }

        private fun bindPostImage(imageUrl: String) {
            val context = binding.root.context
            if (imageUrl.isNotEmpty()) {
                binding.imagePost.visibility = View.VISIBLE
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(binding.imagePost)
            } else {
                binding.imagePost.visibility = View.GONE
            }
        }

        private fun bindAuthenticitySummary(post: Post) {
            val context = binding.root.context
            val totalVotes = post.authenticityVotes.getTotalCount()
            if (totalVotes > 0) {
                val percentages = post.authenticityVotes.getAuthenticityPercentage()
                val truePercentage = percentages["true"] ?: 0.0

                // Update authenticity meter
                binding.progressAuthenticity.progress = truePercentage.toInt()
                binding.textAuthenticitySummary.text = "${truePercentage.toInt()}% verified as authentic"

                // Set progress color based on percentage
                val progressColor = when {
                    truePercentage >= 70 -> ContextCompat.getColor(context, R.color.green_500)
                    truePercentage >= 40 -> ContextCompat.getColor(context, R.color.orange_500)
                    else -> ContextCompat.getColor(context, R.color.red_500)
                }
                binding.progressAuthenticity.progressTintList = ColorStateList.valueOf(progressColor)

                binding.layoutAuthenticity.visibility = View.VISIBLE
            } else {
                binding.layoutAuthenticity.visibility = View.GONE
            }
        }

        private fun bindCategory(category: String) {
            if (category.isNotEmpty() && category != "general") {
                binding.textCategory.text = category.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                }
                binding.textCategory.visibility = View.VISIBLE
            } else {
                binding.textCategory.visibility = View.GONE
            }
        }

        private fun bindVotingPanel(post: Post) {
            // Initially hide voting panel
            binding.layoutVoting.visibility = View.GONE

            // Show stats if there are votes
            val totalVotes = post.authenticityVotes.getTotalCount()
            if (totalVotes > 0) {
                binding.textStats.text = buildStatsText(post)
                binding.textStats.visibility = View.VISIBLE
            } else {
                binding.textStats.visibility = View.GONE
            }
        }

        private fun buildStatsText(post: Post): String {
            return "${formatCount(post.likesCount)} likes • " +
                    "${formatCount(post.commentsCount)} comments • " +
                    "${formatCount(post.sharesCount)} shares"
        }

        private fun formatTimestamp(date: Date): String {
            val now = System.currentTimeMillis()
            val diff = now - date.time

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 604800000 -> "${diff / 86400000}d ago"
                else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
            }
        }

        private fun formatCount(count: Int): String {
            return when {
                count < 0 -> "0"
                count < 1000 -> count.toString()
                count < 1000000 -> "${count / 1000}K"
                else -> "${count / 1000000}M"
            }
        }
    }
}

// Helper Toast import
