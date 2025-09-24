package com.firstword.app.extensions

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

// ImageView extensions
fun ImageView.loadImage(url: String, @DrawableRes placeholder: Int? = null) {
    val request = Glide.with(context).load(url)
    placeholder?.let { request.placeholder(it) }
    request.transition(DrawableTransitionOptions.withCrossFade())
        .error(placeholder ?: 0)
        .into(this)
}

fun ImageView.loadCircularImage(url: String, @DrawableRes placeholder: Int? = null) {
    val request = Glide.with(context).load(url).circleCrop()
    placeholder?.let { request.placeholder(it) }
    request.transition(DrawableTransitionOptions.withCrossFade())
        .error(placeholder ?: 0)
        .into(this)
}

// TextView extensions
fun TextView.setFormattedCount(count: Int) {
    text = when {
        count < 0 -> "0"
        count < 1000 -> count.toString()
        count < 1000000 -> "${count / 1000}K"
        else -> "${count / 1000000}M"
    }
}

// View visibility extensions
fun View.show() { visibility = View.VISIBLE }
fun View.hide() { visibility = View.GONE }
fun View.invisible() { visibility = View.INVISIBLE }

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}