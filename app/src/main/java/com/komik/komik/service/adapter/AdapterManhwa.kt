package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardDailyBinding
import com.komik.komik.service.parser.model.getListUpdate
import com.komik.komik.ui.pages.detail.DetailActivity

class AdapterManhwa(private val list: MutableList<getListUpdate>) :
    RecyclerView.Adapter<AdapterManhwa.viewHolder>() {
    class viewHolder(private val binding: CardDailyBinding) :
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: getListUpdate) {
                binding.judul.text = item.title
                Glide.with(itemView.context)
                    .load(item.urlImage)
                    .error(R.drawable.placeholder)
                    .into(binding.coverUrl)
            }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val binding = CardDailyBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.bind(list[position])

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, DetailActivity::class.java)
            intent.putExtra(DetailActivity.URL,list[position].link)
            intent.putExtra(DetailActivity.LOCATION,list[position].location)
            holder.itemView.context.startActivity(intent)
        }
    }

    fun clear() {
        val size = list.size
        list.clear()
        // Gunakan notifyItemRangeRemoved agar lebih efisien dan ada animasinya
        notifyItemRangeRemoved(0, size)
    }

    fun addData(newData: List<getListUpdate>) {
        list.addAll(newData)
        notifyItemRangeInserted(list.size - newData.size, newData.size)
    }

    fun updateData(newData: List<getListUpdate>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }
}