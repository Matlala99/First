package com.firstword.app.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.firstword.app.databinding.ItemTrendingKeywordBinding

class TrendingKeywordsAdapter(
    private val keywords: List<String>,
    private val onKeywordClick: (String) -> Unit
) : RecyclerView.Adapter<TrendingKeywordsAdapter.KeywordViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeywordViewHolder {
        val binding = ItemTrendingKeywordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KeywordViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: KeywordViewHolder, position: Int) {
        holder.bind(keywords[position])
    }
    
    override fun getItemCount(): Int = keywords.size
    
    inner class KeywordViewHolder(
        private val binding: ItemTrendingKeywordBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        
        fun bind(keyword: String) {
            binding.textKeyword.text = keyword
            binding.root.setOnClickListener {
                onKeywordClick(keyword)
            }
        }
    }
}
