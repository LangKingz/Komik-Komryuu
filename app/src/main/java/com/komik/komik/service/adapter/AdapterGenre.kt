package com.komik.komik.service.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardGenreBinding
import com.komik.komik.service.data.model.Genre

class AdapterGenre(private val list: List<Genre>):RecyclerView.Adapter<AdapterGenre.ViewHolder>() {
    class ViewHolder(private val binding: CardGenreBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(genre: Genre){
            binding.genre.text = genre.genre
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(list[position].genre.isNotEmpty()){
            holder.bind(list[position])
        }
    }
}