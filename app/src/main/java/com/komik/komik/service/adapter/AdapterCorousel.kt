package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardCorouselBinding
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.ui.pages.detail.DetailActivity

class AdapterCorousel(
    private var data: List<DataItem>,
    private val onItemClick: ((DataItem) -> Unit)? = null
) : RecyclerView.Adapter<AdapterCorousel.ViewHolder>() {

    class ViewHolder(private val binding: CardCorouselBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DataItem, onClick: ((DataItem) -> Unit)?) {
            val context = itemView.context
            val imageUrl = item.coverImageUrl ?: item.coverPortraitUrl
            Glide.with(context)
                .load(imageUrl)
                .error(R.drawable.placeholder)
                .into(binding.carouselImageView)

            binding.title.text = item.title

            val clickAction = {
                if (onClick != null) {
                    onClick(item)
                } else {
                    val intent = Intent(context, DetailActivity::class.java).apply {
                        putExtra(DetailActivity.URL, "https://api.shinigami.io/manga/${item.mangaId}")
                        putExtra(DetailActivity.LOCATION, "shinigami")
                    }
                    context.startActivity(intent)
                }
            }

            binding.root.setOnClickListener { clickAction() }
            binding.btnBacaSekarang.setOnClickListener { clickAction() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardCorouselBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position], onItemClick)
    }

    fun updateData(newData: List<DataItem>) {
        data = newData
        notifyDataSetChanged()
    }
}