package com.komik.komik.service.adapter

import android.content.Intent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardChapterBinding
import com.komik.komik.service.database.model.Chapter
import com.komik.komik.ui.pages.read.ReadActivity

class AdapterChapter(private val list: List<Chapter>): RecyclerView.Adapter<AdapterChapter.Holder>() {
    class Holder(private val binding: CardChapterBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(chapter: Chapter) {
            binding.chapter.text = chapter.title
            binding.day.text = chapter.date
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val bind = CardChapterBinding.inflate(android.view.LayoutInflater.from(parent.context),parent,false)
        return Holder(bind)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(list[position])
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context,ReadActivity::class.java)
            intent.putExtra(ReadActivity.EXTRA_URL,list[position].link)
            intent.putExtra(ReadActivity.EXTRA_TITLE,list[position].title)
            intent.putExtra(ReadActivity.EXTRA_TYPE,list[position].location)
            intent.putExtra(ReadActivity.EXTRA_ID,list[position].id)
            holder.itemView.context.startActivity(intent)
        }
    }
}