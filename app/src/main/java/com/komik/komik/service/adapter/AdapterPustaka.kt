package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardDailyBinding
import com.komik.komik.service.data.model.Library
import com.komik.komik.service.data.model.LibraryDetail
import com.komik.komik.ui.pages.detail.DetailActivity

class AdapterPustaka(private val list : MutableList<LibraryDetail>) : RecyclerView.Adapter<AdapterPustaka.ViewHolder>() {
    class ViewHolder(private val binding: CardDailyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: LibraryDetail){
            binding.judul.text = data.comic.title

            val finalpath: String
            val comic = data.library

            if(comic.imageLokal != null){
                finalpath = comic.imageLokal
            }else{
                finalpath = data.comic.imageUrl
            }

            Glide.with(itemView.context)
                .load(finalpath)
                .error(R.drawable.placeholder)
                .into(binding.coverUrl)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardDailyBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java).apply {
                putExtra(DetailActivity.URL, list[position].comic.url)
                putExtra(DetailActivity.LOCATION, list[position].comic.location)
            }

            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateData(newData: List<LibraryDetail>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }

    fun addData(newData: List<LibraryDetail>) {
        list.addAll(newData)
        notifyItemRangeInserted(list.size - newData.size, newData.size)
    }

    fun clearData() {
        list.clear()
        notifyDataSetChanged()
    }

    fun setData(newData: List<LibraryDetail>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }
}