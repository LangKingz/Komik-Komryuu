package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.CardChapterBinding
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.DownloadDao
import com.komik.komik.service.data.dao.chapterDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.model.ChapterwithComic
import com.komik.komik.service.data.model.DownloadChapter
import com.komik.komik.service.data.model.Riwayats
import com.komik.komik.service.utils.ChapterDownloadManager
import com.komik.komik.service.utils.isNetworkAvaible
import com.komik.komik.ui.pages.read.ReadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdapterChapter(
    initialList: List<ChapterwithComic> = emptyList(),
    private var onChapterClicked: ((ChapterwithComic) -> Unit)? = null
) : ListAdapter<ChapterwithComic, AdapterChapter.Holder>(DIFF_CALLBACK) {

    enum class FilterType { ALL, UNREAD, OFFLINE }

    private var lastReadChapterId: Int? = null
    private var masterList: List<ChapterwithComic> = initialList
    private var currentFilterType: FilterType = FilterType.ALL
    private var downloadedChapterIds: Set<Int> = emptySet()

    init {
        if (initialList.isNotEmpty()) {
            submitList(initialList)
        }
    }

    class Holder(val binding: CardChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        val db: DatabaseRiwayats = DatabaseRiwayats.getDatabase(itemView.context)
        val dao: riwayatRelationDao = db.riwayatdao()
        val chapterdao: chapterDao = db.chapterdao()
        val downloadDao: DownloadDao = db.downloadDao()

        fun bind(item: ChapterwithComic, isLastRead: Boolean) {
            val context = itemView.context
            binding.chapter.text = item.chapter.title
            binding.day.text = item.chapter.date

            when {
                isLastRead -> {
                    binding.cardChapter.setBackgroundResource(R.drawable.bg_chapter_last_read)
                    binding.badgeStatus.visibility = View.VISIBLE
                    binding.badgeStatus.text = "Terakhir Dibaca"
                    binding.badgeStatus.setBackgroundResource(R.drawable.badge_last_read)
                    binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.chapter.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(context, R.color.brand))
                }
                item.chapter.isRead -> {
                    binding.cardChapter.setBackgroundResource(R.drawable.bg_chapter_item)
                    binding.badgeStatus.visibility = View.VISIBLE
                    binding.badgeStatus.text = "Dibaca"
                    binding.badgeStatus.setBackgroundResource(R.drawable.badge_read)
                    binding.badgeStatus.setTextColor(ContextCompat.getColor(context, R.color.gray_400))
                    binding.chapter.setTextColor(ContextCompat.getColor(context, R.color.gray_400))
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
                }
                else -> {
                    binding.cardChapter.setBackgroundResource(R.drawable.bg_chapter_item)
                    binding.badgeStatus.visibility = View.GONE
                    binding.chapter.setTextColor(ContextCompat.getColor(context, R.color.white))
                    binding.ivArrow.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
                }
            }

            // Sync Download state from Room & Manager
            CoroutineScope(Dispatchers.IO).launch {
                val isDownloading = ChapterDownloadManager.isChapterActive(item.chapter.id) ||
                        ChapterDownloadManager.downloadStatusFlow.value[item.chapter.id] == DownloadChapter.STATUS_DOWNLOADING
                val download = if (!isDownloading) downloadDao.getDownloadByChapterIdSync(item.chapter.id) else null
                val isCompleted = download != null && download.status == DownloadChapter.STATUS_COMPLETED

                withContext(Dispatchers.Main) {
                    if (isDownloading) {
                        binding.pbDownload.visibility = View.VISIBLE
                        binding.ivDownload.visibility = View.GONE
                    } else if (isCompleted) {
                        binding.pbDownload.visibility = View.GONE
                        binding.ivDownload.visibility = View.VISIBLE
                        binding.ivDownload.setImageResource(R.drawable.cloud_check)
                        binding.ivDownload.setColorFilter(ContextCompat.getColor(context, R.color.text_emerald_400))
                    } else {
                        binding.pbDownload.visibility = View.GONE
                        binding.ivDownload.visibility = View.VISIBLE
                        binding.ivDownload.setImageResource(R.drawable.baseline_download_24)
                        binding.ivDownload.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
                    }
                }
            }

            // Click listener on Download Button
            binding.btnDownload.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val download = downloadDao.getDownloadByChapterIdSync(item.chapter.id)
                    withContext(Dispatchers.Main) {
                        if (download != null && download.status == DownloadChapter.STATUS_COMPLETED) {
                            MaterialAlertDialogBuilder(context)
                                .setTitle("Hapus Unduhan?")
                                .setMessage("Chapter ${item.chapter.title} sudah terunduh offline. Apakah Anda ingin menghapusnya dari penyimpanan?")
                                .setPositiveButton("Hapus") { _, _ ->
                                    ChapterDownloadManager.deleteDownloadedChapter(context, item.chapter.id, item.chapter.comicId)
                                    binding.ivDownload.setImageResource(R.drawable.baseline_download_24)
                                    binding.ivDownload.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
                                    Toast.makeText(context, "Unduhan ${item.chapter.title} dihapus", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("Batal", null)
                                .show()
                        } else if (ChapterDownloadManager.isChapterActive(item.chapter.id)) {
                            Toast.makeText(context, "Chapter ${item.chapter.title} sedang dalam proses unduh...", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Mengunduh ${item.chapter.title}...", Toast.LENGTH_SHORT).show()
                            binding.pbDownload.visibility = View.VISIBLE
                            binding.ivDownload.visibility = View.GONE
                            ChapterDownloadManager.downloadChapter(context, item.chapter, item.comic) {
                                binding.pbDownload.visibility = View.GONE
                                binding.ivDownload.visibility = View.VISIBLE
                                binding.ivDownload.setImageResource(R.drawable.cloud_check)
                                binding.ivDownload.setColorFilter(ContextCompat.getColor(context, R.color.text_emerald_400))
                            }
                        }
                    }
                }
            }
        }

        fun insertRiwayat(chapterId: Int, comicId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatByChapterId(comicId)
                if (data == null) {
                    dao.insertRiwayatRelasi(
                        Riwayats(
                            chapterId = chapterId,
                            comicId = comicId,
                            readAt = System.currentTimeMillis(),
                            isRead = true
                        )
                    )
                } else {
                    dao.updateRiwayatRelasi(
                        data.copy(
                            chapterId = chapterId,
                            comicId = comicId,
                            readAt = System.currentTimeMillis(),
                            isRead = true
                        )
                    )
                }
                chapterdao.updateIsRead(chapterId, true)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = CardChapterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val isLastRead = (item.chapter.id == lastReadChapterId)
        holder.bind(item, isLastRead)

        holder.itemView.setOnClickListener {
            holder.insertRiwayat(item.chapter.id, item.chapter.comicId)
            setLastReadChapterId(item.chapter.id)

            onChapterClicked?.invoke(item)

            val context = holder.itemView.context
            CoroutineScope(Dispatchers.IO).launch {
                val download = holder.downloadDao.getDownloadByChapterIdSync(item.chapter.id)
                val isDownloaded = download != null && download.status == DownloadChapter.STATUS_COMPLETED

                withContext(Dispatchers.Main) {
                    if (isDownloaded || isNetworkAvaible(context)) {
                        val intent = Intent(context, ReadActivity::class.java).apply {
                            putExtra(ReadActivity.EXTRA_URL, item.chapter.link)
                            putExtra(ReadActivity.EXTRA_TITLE, item.chapter.comicTitle)
                            putExtra(ReadActivity.EXTRA_TYPE, item.chapter.location)
                            putExtra(ReadActivity.EXTRA_ID, item.chapter.id)
                            putExtra(ReadActivity.EXTRA_ID_COMIC, item.chapter.comicId)
                            putExtra("num", item.chapter.numChapter)
                        }
                        context.startActivity(intent)
                    } else {
                        Snackbar.make(holder.itemView, "Tidak ada koneksi internet dan chapter ini belum diunduh", Snackbar.LENGTH_SHORT)
                            .setAction("Batal") {}
                            .show()
                    }
                }
            }
        }
    }

    fun setLastReadChapterId(id: Int?) {
        if (lastReadChapterId == id) return
        val prevId = lastReadChapterId
        lastReadChapterId = id
        val items = currentList
        prevId?.let { pid ->
            val idx = items.indexOfFirst { it.chapter.id == pid }
            if (idx != -1) notifyItemChanged(idx)
        }
        id?.let { nid ->
            val idx = items.indexOfFirst { it.chapter.id == nid }
            if (idx != -1) notifyItemChanged(idx)
        }
    }

    fun setMasterList(list: List<ChapterwithComic>, downloadedIds: Set<Int> = downloadedChapterIds) {
        masterList = list
        downloadedChapterIds = downloadedIds
        applyFilterInternal()
    }

    fun updateDownloadedIds(downloadedIds: Set<Int>) {
        downloadedChapterIds = downloadedIds
        if (currentFilterType == FilterType.OFFLINE) {
            applyFilterInternal()
        }
    }

    fun setFilterType(filterType: FilterType) {
        currentFilterType = filterType
        applyFilterInternal()
    }

    fun filterDisplayList(filterType: FilterType) {
        setFilterType(filterType)
    }

    private fun applyFilterInternal() {
        val filtered = when (currentFilterType) {
            FilterType.ALL -> masterList
            FilterType.UNREAD -> masterList.filter { !it.chapter.isRead }
            FilterType.OFFLINE -> masterList.filter { downloadedChapterIds.contains(it.chapter.id) }
        }
        submitList(filtered)
    }

    fun updateList(newList: List<ChapterwithComic>) {
        masterList = newList
        applyFilterInternal()
    }

    fun addData(newData: List<ChapterwithComic>) {
        val updated = currentList.toMutableList()
        updated.addAll(newData)
        submitList(updated)
    }

    fun clearData() {
        submitList(emptyList())
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ChapterwithComic>() {
            override fun areItemsTheSame(
                oldItem: ChapterwithComic,
                newItem: ChapterwithComic
            ): Boolean {
                return oldItem.chapter.id == newItem.chapter.id
            }

            override fun areContentsTheSame(
                oldItem: ChapterwithComic,
                newItem: ChapterwithComic
            ): Boolean {
                return oldItem.chapter == newItem.chapter &&
                        oldItem.comic.id == newItem.comic.id
            }
        }
    }
}