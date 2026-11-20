package com.komik.komik.service.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.CardRiwayatBinding
import com.komik.komik.service.data.model.RiwayatDetail
import com.komik.komik.service.utils.formatWaktuLalu
import com.komik.komik.ui.pages.detail.DetailActivity
import com.komik.komik.ui.pages.read.ReadActivity
import java.util.Calendar

class AdapterRiwayat(
    private var rawList: List<RiwayatDetail>,
    private val onItemClick: ((RiwayatDetail) -> Unit)? = null,
    private val onDeleteClick: ((RiwayatDetail) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    private var masterList: List<RiwayatDetail> = rawList
    private var currentQuery: String = ""
    private var currentSource: String = "Semua"
    private var groupItems: List<Any> = emptyList()

    init {
        rebuildGroupItems()
    }

    fun updateData(newList: List<RiwayatDetail>) {
        masterList = newList
        rebuildGroupItems()
        notifyDataSetChanged()
    }

    fun filter(query: String, source: String) {
        currentQuery = query.trim()
        currentSource = source
        rebuildGroupItems()
        notifyDataSetChanged()
    }

    fun getItemAt(position: Int): RiwayatDetail? {
        if (position in groupItems.indices) {
            val item = groupItems[position]
            if (item is RiwayatDetail) {
                return item
            }
        }
        return null
    }

    fun getFilteredItemCount(): Int {
        return groupItems.count { it is RiwayatDetail }
    }

    private fun rebuildGroupItems() {
        if (masterList.isEmpty()) {
            groupItems = emptyList()
            return
        }

        // 1. Filter by query and source
        val filtered = masterList.filter { item ->
            val matchesQuery = if (currentQuery.isBlank()) {
                true
            } else {
                item.comic.title.contains(currentQuery, ignoreCase = true) ||
                item.chapter.title.contains(currentQuery, ignoreCase = true) ||
                item.comic.location.contains(currentQuery, ignoreCase = true)
            }

            val matchesSource = if (currentSource == "Semua" || currentSource.isBlank()) {
                true
            } else {
                item.comic.location.equals(currentSource, ignoreCase = true)
            }

            matchesQuery && matchesSource
        }

        if (filtered.isEmpty()) {
            groupItems = emptyList()
            return
        }

        // 2. Group by Time Group ("Hari Ini", "Kemarin", "Minggu Ini", "Lebih Lama")
        val grouped = filtered.groupBy { getTimeGroup(it.riwayat.readAt) }

        val orderedKeys = listOf("Hari Ini", "Kemarin", "Minggu Ini", "Lebih Lama")
        val result = mutableListOf<Any>()

        for (key in orderedKeys) {
            val itemsInGroup = grouped[key]
            if (!itemsInGroup.isNullOrEmpty()) {
                result.add(key)
                result.addAll(itemsInGroup)
            }
        }

        groupItems = result
    }

    private fun getTimeGroup(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfToday = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startOfYesterday = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_YEAR, -6)
        val startOfWeek = calendar.timeInMillis

        return when {
            timestamp >= startOfToday -> "Hari Ini"
            timestamp >= startOfYesterday -> "Kemarin"
            timestamp >= startOfWeek -> "Minggu Ini"
            else -> "Lebih Lama"
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (groupItems[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun getItemCount(): Int = groupItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = inflater.inflate(R.layout.headergroup, parent, false)
            HeaderViewHolder(view)
        } else {
            val binding = CardRiwayatBinding.inflate(inflater, parent, false)
            ItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(groupItems[position] as String)
            }
            is ItemViewHolder -> {
                val item = groupItems[position] as RiwayatDetail
                holder.bind(item, onItemClick, onDeleteClick)
            }
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(title: String) {
            headerText.text = title
        }
    }

    class ItemViewHolder(private val binding: CardRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            riwayatDetail: RiwayatDetail,
            onItemClick: ((RiwayatDetail) -> Unit)?,
            onDeleteClick: ((RiwayatDetail) -> Unit)?
        ) {
            val context = itemView.context

            // Title & Source
            binding.NameKomik.text = riwayatDetail.comic.title
            binding.tvSourceBadge.text = riwayatDetail.comic.location.ifBlank { "Komik" }

            // Last read time
            val timeText = formatWaktuLalu(riwayatDetail.riwayat.readAt)
            binding.time.text = "• $timeText"

            // Chapter
            val numChapter = riwayatDetail.chapter.numChapter
            val chapterText = if (numChapter > 0.0) {
                val formattedNum = if (numChapter % 1.0 == 0.0) {
                    numChapter.toInt().toString()
                } else {
                    numChapter.toString()
                }
                "Chapter $formattedNum"
            } else {
                riwayatDetail.chapter.title.ifBlank { "Chapter Terbaru" }
            }
            binding.Chapter.text = chapterText

            // Progress Bar & Page Count
            val pageRead = riwayatDetail.riwayat.pageRead ?: 0
            val totalPage = riwayatDetail.riwayat.totalPage ?: 0

            if (totalPage > 0 && pageRead > 0) {
                binding.layoutProgress.visibility = View.VISIBLE
                val progressPercent = ((pageRead.toFloat() / totalPage.toFloat()) * 100f).toInt().coerceIn(0, 100)
                binding.progressBar.progress = progressPercent
                binding.tvProgress.text = "$pageRead/$totalPage"
            } else {
                binding.layoutProgress.visibility = View.GONE
            }

            // Cover Image
            val imageUri = riwayatDetail.comic.localImageUri?.takeIf { it.isNotBlank() }
                ?: riwayatDetail.comic.imageUrl

            Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.corner_gambar_riwayat)
                .error(R.drawable.ppanime)
                .into(binding.Gambar)

            // Click comic cover -> Open DetailActivity
            binding.Gambar.setOnClickListener {
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.URL, riwayatDetail.comic.url)
                    putExtra(DetailActivity.LOCATION, riwayatDetail.comic.location)
                }
                context.startActivity(intent)
            }

            // Click "Lanjut" button -> Directly open ReadActivity
            binding.btnContinueRead.setOnClickListener {
                resumeReading(context, riwayatDetail)
            }

            // Click entire item card -> Directly open ReadActivity
            itemView.setOnClickListener {
                if (onItemClick != null) {
                    onItemClick(riwayatDetail)
                } else {
                    resumeReading(context, riwayatDetail)
                }
            }

            // Delete button
            binding.imageButton.setOnClickListener {
                onDeleteClick?.invoke(riwayatDetail)
            }
        }

        private fun resumeReading(context: Context, riwayatDetail: RiwayatDetail) {
            val intent = Intent(context, ReadActivity::class.java).apply {
                putExtra(ReadActivity.EXTRA_URL, riwayatDetail.chapter.link)
                putExtra(ReadActivity.EXTRA_TITLE, riwayatDetail.comic.title)
                putExtra(
                    ReadActivity.EXTRA_TYPE,
                    riwayatDetail.chapter.location ?: riwayatDetail.comic.location
                )
                putExtra(ReadActivity.EXTRA_ID, riwayatDetail.chapter.id)
                putExtra("num", riwayatDetail.chapter.numChapter)
                putExtra(ReadActivity.EXTRA_ID_COMIC, riwayatDetail.comic.id)
                putExtra("pageRead", riwayatDetail.riwayat.pageRead ?: 0)
            }
            context.startActivity(intent)
        }
    }
}
