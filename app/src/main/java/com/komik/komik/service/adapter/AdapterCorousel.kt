package com.komik.komik.service.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.databinding.CardCorouselBinding
import com.komik.komik.service.api.model.DataItem

class AdapterCorousel(private val data: List<DataItem>): RecyclerView.Adapter<AdapterCorousel.viewHolder>() {
    class viewHolder(private val binding: CardCorouselBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(data: DataItem){
            Glide.with(itemView.context)
                .load(data.coverImageUrl)
                .into(binding.carouselImageView)
            binding.title.text = data.title
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val binding = CardCorouselBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolder(binding)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.bind(data[position])
    }
}