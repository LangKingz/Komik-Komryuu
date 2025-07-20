package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardSourceHistoryBinding
import com.komik.komik.service.data.model.RiwayatSource
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.ui.pages.source.sourceActivity

class AdapterRiwayatSource(private val list: MutableList<RiwayatSource>): RecyclerView.Adapter<AdapterRiwayatSource.viewHolder>() {
    class viewHolder(private val binding: CardSourceHistoryBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RiwayatSource){
            binding.JudulExtensi.text = item.extensi
            binding.waktu.text = formatWaktuLalu(item.createdAt)

            val url = when(item.location){
                "ikiru" -> "https://ikiru.id/favicon-32x32.png"
                "comicscan" -> "https://lc3.cosmicscans.asia/wp-content/uploads/2024/03/logo_cosmic.png"
                "shinigami" -> "https://06.shinigami.asia/favicon.png"
                "komikcast" -> "https://komikcast.li/wp-content/uploads/2021/02/cropped-logo-kc-32x32.png"
                "kiryuu" -> "https://kiryuu02.com/wp-content/uploads/2021/10/cropped-logo-icon-kiryuu-1-456248-udlqjluy-194445-3fNc9Wlc-32x32.png"
                "komiku" -> "https://komikindo.rip/"
                "manhwaindo"-> "https://manhwaindo.com/wp-content/uploads/2024/02/cropped-android-chrome-512x512-1-192x192.png"
                "batoto-all"-> "https://bato.si/static-assets/img/bato-favicon.ico?v2"
                else -> ""
            }

            Glide.with(itemView.context)
                .load(url)
                .error(R.drawable.placeholder)
                .into(binding.imageView)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val binding = CardSourceHistoryBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.bind(list[position])

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, sourceActivity::class.java).apply {
                putExtra(sourceActivity.URL,list[position].url)
                putExtra(sourceActivity.location,list[position].location)
            }
            holder.itemView.context.startActivity(intent)

        }
    }
}