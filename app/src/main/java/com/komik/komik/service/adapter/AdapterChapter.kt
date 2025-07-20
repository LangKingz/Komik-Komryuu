package com.komik.komik.service.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardChapterBinding
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdapterChapter(
    private val list: List<Chapter>,
    private val data : Comic
) :
    RecyclerView.Adapter<AdapterChapter.Holder>() {
    class Holder(private val binding: CardChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapter: Chapter) {
            binding.chapter.text = chapter.title
            binding.day.text = chapter.date
        }

        fun insertRiwayat(chapter_id: Int, comic_id: Int) {
            val db: DatabaseRiwayats = DatabaseRiwayats.getDatabase(itemView.context)
            val dao: riwayatRelationDao = db.riwayatdao()

            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatByChapterId(comic_id)
                if (data == null) {
                    dao.insertRiwayatRelasi(
                        Riwayats(
                            chapterId = chapter_id,
                            comicId = comic_id
                        )
                    )
                } else {
                    dao.updateRiwayatRelasi(
                        data.copy(
                            chapterId = chapter_id,
                            comicId = comic_id,
                            readAt = System.currentTimeMillis()
                        )
                    )
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

            holder.insertRiwayat(
                list[position].id,
                data.id
            )

//
            val intent = Intent(holder.itemView.context, ReadActivity::class.java)
            intent.putExtra(ReadActivity.EXTRA_URL, list[position].link)
            intent.putExtra(ReadActivity.EXTRA_TITLE, list[position].title)
            intent.putExtra(ReadActivity.EXTRA_TYPE, list[position].location)
            intent.putExtra(ReadActivity.EXTRA_ID, list[position].id)
            holder.itemView.context.startActivity(intent)
        }
    }
}