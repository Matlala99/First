package com.firstword.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firstword.app.R
import com.firstword.app.databinding.ItemUserBinding
import com.firstword.app.models.User

class UserAdapter(
    private var users: List<User>,
    private val currentUserId: String,
    private var followingIds: Set<String> = emptySet(),
    private val onUserClick: (User) -> Unit,
    private val onFollowClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    fun updateUsers(newUsers: List<User>, newFollowingIds: Set<String>? = null) {
        users = newUsers
        newFollowingIds?.let { followingIds = it }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textDisplayName.text = user.displayName.ifEmpty { user.handle }
            binding.textHandle.text = "@${user.handle}"
            binding.textBio.text = user.bio
            binding.textFollowersCount.text = formatCount(user.followersCount)
            binding.textFollowingCount.text = formatCount(user.followingCount)

            // Load avatar
            Glide.with(binding.root.context)
                .load(user.avatarUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile)
                .into(binding.imageAvatar)

            // Follow button logic
            if (user.id == currentUserId) {
                binding.buttonFollow.visibility = View.GONE
            } else {
                binding.buttonFollow.visibility = View.VISIBLE
                val isFollowing = followingIds.contains(user.id)

                if (isFollowing) {
                    binding.buttonFollow.text = "Following"
                    // Use a secondary style for following state
                    binding.buttonFollow.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    binding.buttonFollow.setStrokeColorResource(R.color.primary)
                    binding.buttonFollow.setStrokeWidthResource(R.dimen.button_stroke_width)
                    binding.buttonFollow.setTextColor(binding.root.context.getColor(R.color.primary))
                } else {
                    binding.buttonFollow.text = "Follow"
                    binding.buttonFollow.setBackgroundColor(binding.root.context.getColor(R.color.primary))
                    binding.buttonFollow.setStrokeWidth(0)
                    binding.buttonFollow.setTextColor(binding.root.context.getColor(R.color.background))
                }

                binding.buttonFollow.setOnClickListener {
                    onFollowClick(user)
                }
            }

            // Click on user item
            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }

        private fun formatCount(count: Int): String {
            return when {
                count < 1000 -> count.toString()
                count < 1000000 -> String.format("%.1fk", count / 1000.0)
                else -> String.format("%.1fM", count / 1000000.0)
            }
        }
    }
}