package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardRiwayatBinding
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.ui.pages.detail.DetailActivity
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdapterRiwayat(private val list: List<RiwayatDetail>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    private var groupData = list.groupBy {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it.riwayat.readAt))
    }

    private var groupItems = groupData.flatMap { (date, items) ->
        listOf(date) + items
    }

    override fun getItemViewType(position: Int): Int {
        return if (groupItems[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    class ItemViewHolder(private val binding: CardRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context = itemView.context
        val db = DatabaseRiwayats.getDatabase(context)
        val riwayatDao = db.riwayatdao()

        fun delete(id: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                riwayatDao.deleteRiwayatRelasi(id)
            }
        }

        fun bind(riwayatDetail: RiwayatDetail) {
            binding.time.text =
                "dari ${riwayatDetail.comic.location} • ${formatWaktuLalu(riwayatDetail.riwayat.readAt)}"
            binding.NameKomik.text = riwayatDetail.comic.title
            binding.Chapter.text = riwayatDetail.chapter.title
            Glide.with(itemView.context)
                .load(riwayatDetail.comic.localImageUri)
                .into(binding.Gambar)

            binding.Gambar.setOnClickListener {
                val intent = Intent(itemView.context, DetailActivity::class.java)
                intent.putExtra(DetailActivity.URL, riwayatDetail.comic.url)
                intent.putExtra(DetailActivity.LOCATION, riwayatDetail.comic.location)
                itemView.context.startActivity(intent)
            }

            binding.imageButton.setOnClickListener {
                delete(riwayatDetail.riwayat.id)
            }
        }

    }

    class HeaderViewholder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(header: String) {

            headerText.text = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            ).format(Date(System.currentTimeMillis()))

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.headergroup, parent, false)
            HeaderViewholder(view)
        } else {
            val binding =
                CardRiwayatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return groupItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewholder) {
            holder.bind(groupItems[position] as String)
        } else if (holder is ItemViewHolder) {
            holder.bind(groupItems[position] as RiwayatDetail)
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, ReadActivity::class.java).apply {
                    putExtra(
                        ReadActivity.EXTRA_URL,
                        (groupItems[position] as RiwayatDetail).chapter.link
                    )
                    putExtra(
                        ReadActivity.EXTRA_TITLE,
                        (groupItems[position] as RiwayatDetail).comic.title
                    )
                    putExtra(
                        ReadActivity.EXTRA_TYPE,
                        (groupItems[position] as RiwayatDetail).chapter.location
                    )
                    putExtra(
                        ReadActivity.EXTRA_ID,
                        (groupItems[position] as RiwayatDetail).chapter.id
                    )
                    putExtra(
                        "num",
                        (groupItems[position] as RiwayatDetail).chapter.numChapter
                    )
                }
                holder.itemView.context.startActivity(intent)
            }
        }


    }

}