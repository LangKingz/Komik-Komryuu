package com.komik.komik.service.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.komik.komik.R
import com.komik.komik.databinding.ImagelayoutBinding
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.utils.downloadImageManager

class AdapterImage(
    private val list: MutableList<getImages>,
    private val type: sourceType,
    val refererUrl: String? = null
) :
    RecyclerView.Adapter<AdapterImage.ViewHolder>() {
    class ViewHolder(
        val binding: ImagelayoutBinding,
        val type: sourceType,
        val refererUrl: String
    ) : RecyclerView.ViewHolder(binding.root) {

        val context = itemView.context
        fun bind(data: getImages) {
            when (type) {
                sourceType.doujindesu -> {
                    val headers = LazyHeaders.Builder()
                        .addHeader("Referer", refererUrl)
                        .addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1; like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                        )
                        .addHeader("X-Requested-With", "XMLHttpRequest")
                        .build()

                    val glideUrl = GlideUrl(data.image, headers)
                    Glide.with(itemView.context)
                        .load(glideUrl)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.loading.visibility = View.GONE
                                binding.imagechapter.setImageResource(R.drawable.placeholder)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.loading.visibility = View.GONE
                                return false
                            }
                        })
                        .override(Target.SIZE_ORIGINAL)
                        .into(binding.imagechapter)
                }

                else -> {
                    Glide.with(itemView.context)
                        .load(data.image)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.loading.visibility = View.GONE
                                binding.imagechapter.setImageResource(R.drawable.placeholder)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                binding.loading.visibility = View.GONE
                                return false
                            }
                        })
                        .override(Target.SIZE_ORIGINAL)
                        .into(binding.imagechapter)
                }

            }

            binding.imagechapter.setOnLongClickListener {
                AlertDialogCostum(
                    context,
                    "Unduh",
                    "Apakah anda yakin ingin mendownload gambar ini?",
                    onPositive = { dialog ->
                        downloadImageManager(context, data.image)
                    },
                    onNegative = { dialog ->
                        dialog.dismiss()
                    }

                )
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ImagelayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, type, refererUrl.toString())
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])

        val context = holder.itemView.context


    }

    fun updateList(newList: List<getImages>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    fun addList(newList: List<getImages>) {
        val currentSize = list.size
        list.addAll(newList)
        notifyItemRangeInserted(currentSize, newList.size)
    }

    fun clear() {
        val size = list.size
        list.clear()
        // Gunakan notifyItemRangeRemoved agar lebih efisien dan ada animasinya
        notifyItemRangeRemoved(0, size)
    }

}