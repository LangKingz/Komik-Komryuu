package com.komik.komik.service.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardChapterBinding
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.model.Chapter
import com.komik.komik.service.data.model.ChapterwithComic
import com.komik.komik.service.data.model.Comic
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdapterChapter(
    private val list: MutableList<ChapterwithComic>
) :
    RecyclerView.Adapter<AdapterChapter.Holder>() {
    class Holder(private val binding: CardChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        val db: DatabaseRiwayats = DatabaseRiwayats.getDatabase(itemView.context)
        val dao: riwayatRelationDao = db.riwayatdao()
        val chapterdao: chapterDao = db.chapterdao()

        fun bind(chapter: ChapterwithComic) {
            binding.chapter.text = chapter.chapter.title
            binding.day.text = chapter.chapter.date

            if(chapter.chapter.isRead){
                binding.cardChapter.setBackgroundColor(
                    itemView.context.resources.getColor(
                        com.komik.komik.R.color.transparent
                    )
                )
            }
        }

        fun insertRiwayat(chapter_id: Int, comic_id: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatByChapterId(comic_id)
                if (data == null) {
                    dao.insertRiwayatRelasi(
                        Riwayats(
                            chapterId = chapter_id,
                            comicId = comic_id,
                            readAt = System.currentTimeMillis(),
                            isRead = true
                        )
                    )
                    chapterdao.updateIsRead(chapter_id, true)

                } else {
                    dao.updateRiwayatRelasi(
                        data.copy(
                            chapterId = chapter_id,
                            comicId = comic_id,
                            readAt = System.currentTimeMillis(),
                            isRead = true
                        )
                    )
                    chapterdao.updateIsRead(chapter_id, true)

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
                list[position].chapter.id,
                list[position].chapter.comicId
            )

//
            val intent = Intent(holder.itemView.context, ReadActivity::class.java)
            intent.putExtra(ReadActivity.EXTRA_URL, list[position].chapter.link)
            intent.putExtra(ReadActivity.EXTRA_TITLE, list[position].chapter.comicTitle)
            intent.putExtra(ReadActivity.EXTRA_TYPE, list[position].chapter.location)
            intent.putExtra(ReadActivity.EXTRA_ID, list[position].chapter.id)
            intent.putExtra(ReadActivity.EXTRA_ID_COMIC, list[position].chapter.comicId)
            intent.putExtra("num", list[position].chapter.numChapter)
            holder.itemView.context.startActivity(intent)
        }
    }

    fun updateList(newList: List<ChapterwithComic>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun addData(newData: List<ChapterwithComic>) {
        val startPosition = list.size
        list.addAll(newData)
        notifyItemRangeInserted(startPosition, newData.size)

    }



    fun clearData() {
        list.clear()
        notifyDataSetChanged()
    }
}