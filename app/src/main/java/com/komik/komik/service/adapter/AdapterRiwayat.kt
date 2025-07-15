package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.databinding.CardRiwayatBinding
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.ui.pages.detail.DetailActivity

class AdapterRiwayat(private val list: List<RiwayatDetail>): RecyclerView.Adapter<AdapterRiwayat.ViewHolder>() {
    class ViewHolder(private val binding:CardRiwayatBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(riwayatDetail: RiwayatDetail){
            binding.time.text = "dari ${riwayatDetail.comic.location} • ${formatWaktuLalu(riwayatDetail.riwayat.readAt)}"
            binding.NameKomik.text = riwayatDetail.comic.title
            binding.Chapter.text = riwayatDetail.chapter.title
            Glide.with(itemView.context)
                .load(riwayatDetail.comic.imageUrl)
                .into(binding.Gambar)

            binding.Gambar.setOnClickListener {
                val intent = Intent(itemView.context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.URL,riwayatDetail.comic.url)
                intent.putExtra(DetailActivity.LOCATION,riwayatDetail.comic.location)
                itemView.context.startActivity(intent)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardRiwayatBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

}