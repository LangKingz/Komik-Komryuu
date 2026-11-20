package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardSourceHistoryBinding
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.ui.pages.read.ReadActivity

class AdapterRiwayatSource(
    private var list: MutableList<RiwayatDetail> = mutableListOf()
) : RecyclerView.Adapter<AdapterRiwayatSource.ViewHolder>() {

    fun updateData(newList: List<RiwayatDetail>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: CardSourceHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: RiwayatDetail) {
            val context = itemView.context
            binding.JudulExtensi.text = item.comic.title

            val numChapter = item.chapter.numChapter
            val chapterText = if (numChapter > 0.0) {
                val formattedNum = if (numChapter % 1.0 == 0.0) {
                    numChapter.toInt().toString()
                } else {
                    numChapter.toString()
                }
                "Chapter $formattedNum"
            } else {
                item.chapter.title.ifBlank { "Chapter Terbaru" }
            }
            binding.tvChapterHistory.text = chapterText
            binding.waktu.text = formatWaktuLalu(item.riwayat.readAt)

            val imageUri = item.comic.localImageUri?.takeIf { it.isNotBlank() }
                ?: item.comic.imageUrl

            Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.corner_gambar)
                .error(R.drawable.ppanime)
                .into(binding.imageView)

            fun resumeReading() {
                val intent = Intent(context, ReadActivity::class.java).apply {
                    putExtra(ReadActivity.EXTRA_URL, item.chapter.link)
                    putExtra(ReadActivity.EXTRA_TITLE, item.comic.title)
                    putExtra(
                        ReadActivity.EXTRA_TYPE,
                        item.chapter.location ?: item.comic.location
                    )
                    putExtra(ReadActivity.EXTRA_ID, item.chapter.id)
                    putExtra("num", item.chapter.numChapter)
                    putExtra(ReadActivity.EXTRA_ID_COMIC, item.comic.id)
                    putExtra("pageRead", item.riwayat.pageRead ?: 0)
                }
                context.startActivity(intent)
            }

            binding.imageButton2.setOnClickListener {
                resumeReading()
            }

            itemView.setOnClickListener {
                resumeReading()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CardSourceHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }
}
