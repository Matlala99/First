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
import java.text.SimpleDateFormat
import java.util.*

class FeedAdapter(
    private val posts: List<Post>,
    private val onPostAction: (Post, String) -> Unit
) : RecyclerView.Adapter<FeedAdapter.PostViewHolder>() {

    companion object {
        private const val ACTION_LIKE = "like"
        private const val ACTION_COMMENT = "comment"
        private const val ACTION_SHARE = "share"
        private const val ACTION_VOTE_TRUE = "vote_true"
        private const val ACTION_VOTE_FAKE = "vote_fake"
        private const val ACTION_VOTE_AI = "vote_ai"
        private const val ACTION_VIEW_PROFILE = "view_profile"
        private const val ACTION_VIEW_IMAGE = "view_image"
        private const val ACTION_TOGGLE_VOTE = "toggle_vote"
    }

    private var items: List<Post> = posts

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

                // Click listeners
                setClickListeners(post)
            }
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

                // User profile clicks
                val userInfoLayout = binding.root.findViewById<View>(R.id.image_user_avatar)?.parent as? ViewGroup
                userInfoLayout?.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }

                // Fallback: individual user info clicks
                imageUserAvatar.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }
                textUserName.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }
                textUserHandle.setOnClickListener { onPostAction(post, ACTION_VIEW_PROFILE) }

                imagePost.setOnClickListener { onPostAction(post, ACTION_VIEW_IMAGE) }
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
            val totalVotes = post.authenticityVotes.totalCount
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
            val totalVotes = post.authenticityVotes.totalCount
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