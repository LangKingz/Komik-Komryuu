package com.komik.komik.service.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.ItemPustakaGridBinding
import com.komik.komik.databinding.ItemPustakaListBinding
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.model.LibraryDetail
import com.komik.komik.ui.pages.detail.DetailActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdapterPustaka(
    private val list: MutableList<LibraryDetail>,
    var isListMode: Boolean = false,
    var spanCount: Int = 3
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_GRID = 0
        const val VIEW_TYPE_LIST = 1
    }

    var isSelectionMode: Boolean = false
        set(value) {
            field = value
            if (!value) selectedIds.clear()
            notifyDataSetChanged()
        }

    val selectedIds = mutableSetOf<Int>()
    var onSelectionChangedListener: ((Int) -> Unit)? = null
    var onItemDeletedListener: (() -> Unit)? = null

    private val unreadCache = mutableMapOf<Int, Int>()

    override fun getItemViewType(position: Int): Int {
        return if (isListMode) VIEW_TYPE_LIST else VIEW_TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_LIST) {
            val binding = ItemPustakaListBinding.inflate(inflater, parent, false)
            ListViewHolder(binding)
        } else {
            val binding = ItemPustakaGridBinding.inflate(inflater, parent, false)
            GridViewHolder(binding)
        }
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = list[position]
        val context = holder.itemView.context

        loadUnreadCount(context, item.comic.id) { unread ->
            if (holder is GridViewHolder) {
                holder.bind(item, isSelectionMode, selectedIds.contains(item.comic.id), unread)
            } else if (holder is ListViewHolder) {
                holder.bind(item, isSelectionMode, selectedIds.contains(item.comic.id), unread)
            }
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(item.comic.id)
            } else {
                val intent = Intent(context, DetailActivity::class.java).apply {
                    putExtra(DetailActivity.URL, item.comic.url)
                    putExtra(DetailActivity.LOCATION, item.comic.location)
                }
                context.startActivity(intent)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
            }
            toggleSelection(item.comic.id)
            true
        }

        if (holder is ListViewHolder) {
            holder.binding.btnDelete.setOnClickListener {
                AlertDialogCostum(
                    context,
                    "Hapus Komik",
                    "Hapus ${item.comic.title} dari pustaka?",
                    onPositive = { dialog ->
                        removeItemAt(holder.bindingAdapterPosition, context)
                        dialog.dismiss()
                    },
                    onNegative = { it.dismiss() }
                ).show()
            }
        }
    }

    fun toggleSelection(comicId: Int) {
        if (selectedIds.contains(comicId)) {
            selectedIds.remove(comicId)
        } else {
            selectedIds.add(comicId)
        }
        if (selectedIds.isEmpty()) {
            isSelectionMode = false
        }
        notifyDataSetChanged()
        onSelectionChangedListener?.invoke(selectedIds.size)
    }

    private fun loadUnreadCount(context: Context, comicId: Int, onResult: (Int) -> Unit) {
        val cached = unreadCache[comicId]
        if (cached != null) {
            onResult(cached)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseRiwayats.getDatabase(context)
            val unread = try {
                db.chapterdao().getUnreadChapterCount(comicId)
            } catch (_: Exception) {
                0
            }
            unreadCache[comicId] = unread
            withContext(Dispatchers.Main) {
                onResult(unread)
            }
        }
    }

    inner class GridViewHolder(val binding: ItemPustakaGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: LibraryDetail, isSelectMode: Boolean, isSelected: Boolean, unreadCount: Int) {
            binding.judul.text = data.comic.title
            binding.judul.textSize = if (spanCount == 2) 13.5f else 12f

            binding.tvSourceBadge.text = data.comic.location.ifBlank { "Komik" }

            binding.tvStatus.text = buildString {
                if (!data.comic.status.isNullOrBlank()) {
                    append(data.comic.status)
                } else {
                    append("Tersimpan")
                }
            }

            if (data.comic.tags == "adult") {
                binding.tagAdult.visibility = View.VISIBLE
            } else {
                binding.tagAdult.visibility = View.GONE
            }

            if (unreadCount > 0) {
                binding.badgeUnread.visibility = View.VISIBLE
                binding.badgeUnread.text = "+$unreadCount"
            } else {
                binding.badgeUnread.visibility = View.GONE
            }

            if (isSelectMode) {
                binding.cbSelect.visibility = View.VISIBLE
                binding.cbSelect.isChecked = isSelected
                binding.cbSelect.setOnClickListener {
                    toggleSelection(data.comic.id)
                }
            } else {
                binding.cbSelect.visibility = View.GONE
            }

            Glide.with(itemView.context)
                .load(data.comic.localImageUri ?: data.comic.imageUrl)
                .error(R.drawable.placeholder)
                .into(binding.coverUrl)
        }
    }

    inner class ListViewHolder(val binding: ItemPustakaListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: LibraryDetail, isSelectMode: Boolean, isSelected: Boolean, unreadCount: Int) {
            binding.judul.text = data.comic.title
            binding.tvLocation.text = data.comic.location
            binding.tvStatus.text = if (!data.comic.status.isNullOrBlank()) {
                data.comic.status
            } else {
                "Tersimpan di Pustaka"
            }

            if (data.comic.tags == "adult") {
                binding.tagAdult.visibility = View.VISIBLE
            } else {
                binding.tagAdult.visibility = View.GONE
            }

            if (unreadCount > 0) {
                binding.badgeUnread.visibility = View.VISIBLE
                binding.badgeUnread.text = "$unreadCount Baru"
            } else {
                binding.badgeUnread.visibility = View.GONE
            }

            if (isSelectMode) {
                binding.cbSelect.visibility = View.VISIBLE
                binding.cbSelect.isChecked = isSelected
                binding.cbSelect.setOnClickListener {
                    toggleSelection(data.comic.id)
                }
                binding.btnDelete.visibility = View.GONE
            } else {
                binding.cbSelect.visibility = View.GONE
                binding.btnDelete.visibility = View.VISIBLE
            }

            Glide.with(itemView.context)
                .load(data.comic.localImageUri ?: data.comic.imageUrl)
                .error(R.drawable.placeholder)
                .into(binding.coverUrl)
        }
    }

    fun setData(newData: List<LibraryDetail>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }

    fun updateData(newData: List<LibraryDetail>) {
        list.clear()
        list.addAll(newData)
        notifyDataSetChanged()
    }

    fun clearData() {
        list.clear()
        notifyDataSetChanged()
    }

    fun removeItemAt(position: Int, context: Context) {
        if (position in 0 until list.size) {
            val itemToDelete = list[position]
            val dbUtama = DatabaseRiwayats.getDatabase(context)
            val daoComic = dbUtama.comicdao()
            val libraryDao = dbUtama.libraryDao()

            CoroutineScope(Dispatchers.IO).launch {
                daoComic.deleteChapterById(itemToDelete.comic.id)
                daoComic.deleteGenreByTitle(itemToDelete.comic.title)
                daoComic.deleteComicByUrl(itemToDelete.comic.id)
                libraryDao.deleteLibrary(itemToDelete.comic.id)
            }

            list.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, list.size)
            onItemDeletedListener?.invoke()
        }
    }

    fun deleteSelectedItems(context: Context) {
        if (selectedIds.isEmpty()) return
        val itemsToDelete = list.filter { selectedIds.contains(it.comic.id) }
        val dbUtama = DatabaseRiwayats.getDatabase(context)
        val daoComic = dbUtama.comicdao()
        val libraryDao = dbUtama.libraryDao()

        CoroutineScope(Dispatchers.IO).launch {
            for (item in itemsToDelete) {
                daoComic.deleteChapterById(item.comic.id)
                daoComic.deleteGenreByTitle(item.comic.title)
                daoComic.deleteComicByUrl(item.comic.id)
                libraryDao.deleteLibrary(item.comic.id)
            }
        }

        list.removeAll(itemsToDelete)
        selectedIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onItemDeletedListener?.invoke()
    }
}
