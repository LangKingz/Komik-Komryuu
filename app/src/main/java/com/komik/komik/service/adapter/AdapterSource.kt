package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.CardSourceBinding
import com.komik.komik.databinding.HeaderExtensiBinding
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.ui.pages.source.sourceActivity
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.data.dao.sourceDao
import com.komik.komik.service.data.data.DatabaseRiwayatSource
import com.komik.komik.service.data.model.RiwayatSource
import com.komik.komik.service.utils.downloadImageManager
import com.komik.komik.service.utils.isNetworkAvaible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.checkerframework.checker.units.qual.s

class AdapterSource(private val list: MutableList<DataItems>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
    }

    private var groupData = list.groupBy { it.type }

    private var groupItems = groupData.flatMap { (type, item) ->
        listOf(type) + item
    }

    override fun getItemViewType(position: Int): Int {
        return if (groupItems[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    class headerViewHolder(private val binding: HeaderExtensiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: String) {
            binding.headerText.text = "⭐ Extensi $item"
        }

    }


    class SourceViewHolder(private val binding: CardSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val context = itemView.context
        fun bind(item: DataItems) {
            binding.JudulExtensi.text = item.name
            binding.profilExtensi.text = item.name?.get(0).toString()

            when(item.type){
                "dewasa" -> {
                    binding.tag.visibility = View.VISIBLE
                    binding.tag.text = "18+"
                    binding.tag.setTextColor(context.resources.getColor(R.color.red))
                }

            }

            Glide.with(itemView.context)
                .load(item.logo)
                .error(R.drawable.placeholder)
                .into(binding.imageView7)

            binding.star.setOnClickListener {
                downloadImageManager(context, item.logo.toString())
            }

        }

        fun updateData(item: DataItems) {
            val db: DatabaseRiwayatSource = DatabaseRiwayatSource.getDatabase(itemView.context)
            val dao: sourceDao = db.riwayatdao()

            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatSource()
                if (data != null) {
                    dao.updateRiwayatSource(
                        data.copy(
                            extensi = item.name.toString(),
                            url = item.baseUrl.toString(),
                            imageUrl = item.logo.toString(),
                            location = item.location.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    dao.insertRiwayatSource(
                        RiwayatSource(
                            extensi = item.name.toString(),
                            url = item.baseUrl.toString(),
                            imageUrl = item.logo.toString(),
                            location = item.location.toString()
                        )
                    )
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val viewHeader =
                    HeaderExtensiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                headerViewHolder(viewHeader)
            }

            VIEW_TYPE_ITEM -> {
                val binding =
                    CardSourceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                return SourceViewHolder(binding)
            }

            else -> error("Unknown view type")
        }
    }

    override fun getItemCount(): Int {
        return groupItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is headerViewHolder -> {
                holder.bind(groupItems[position] as String)
            }

            is SourceViewHolder -> {

                val currentItem = groupItems[position] as DataItems

                holder.bind(currentItem)

                val context = holder.itemView.context

                holder.itemView.setOnClickListener {

                    holder.updateData(currentItem)

                    if (isNetworkAvaible(context)) {
                        val intent =
                            Intent(holder.itemView.context, sourceActivity::class.java).apply {
                                putExtra(sourceActivity.URL, currentItem.baseUrl)
                                putExtra(sourceActivity.location, currentItem.location)
                            }
                        holder.itemView.context.startActivity(intent)
                    } else {
                        Snackbar.make(
                            holder.itemView,
                            "Tidak ada koneksi internet",
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction("Cancel", {})
                            .setTextColor(context.getColor(R.color.white))
                            .setBackgroundTint(context.getColor(R.color.background_slate_800))
                            .show()
                    }
                }
            }
        }


    }
}