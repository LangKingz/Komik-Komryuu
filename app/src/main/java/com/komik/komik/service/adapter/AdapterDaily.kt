package com.komik.komik.service.adapter

import android.content.Intent
import android.provider.ContactsContract.Data
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.databinding.CardDailyBinding
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.ui.pages.detail.DetailActivity

class AdapterDaily(private val list: List<DataItem>): RecyclerView.Adapter<AdapterDaily.ViewHolder>() {
    class ViewHolder(private val binding: CardDailyBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(data: DataItem){
            binding.judul.text = data.title
            Glide.with(itemView.context)
                .load(data.coverImageUrl)
                .into(binding.coverUrl)
        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardDailyBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return  list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])


    }
}