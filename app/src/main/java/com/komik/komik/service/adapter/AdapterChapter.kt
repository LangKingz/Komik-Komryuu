package com.komik.komik.service.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardChapterBinding
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Riwayats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdapterChapter(
    private val list: List<Chapter>,
    private val onChapterClick: (Chapter) -> Unit
) :
    RecyclerView.Adapter<AdapterChapter.Holder>() {
    class Holder(private val binding: CardChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapter: Chapter) {
            binding.chapter.text = chapter.title
            binding.day.text = chapter.date
        }

        fun insertRiwayat(riwayat: Riwayats) {
            val db: DatabaseRiwayats = DatabaseRiwayats.getDatabase(itemView.context)
            val dao: riwayatRelationDao = db.riwayatdao()

            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatByChapterId(riwayat.chapterId)
                if (data == null) {
                    dao.insertRiwayatRelasi(riwayat)
                } else {
                    dao.updateRiwayatRelasi(riwayat)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val bind = CardChapterBinding.inflate(
            android.view.LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(bind)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(list[position])
        holder.itemView.setOnClickListener {
            onChapterClick(list[position])
        }
    }
}