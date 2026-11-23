package com.komik.komik.service.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.komik.komik.databinding.ItemChapterEndBinding
import com.komik.komik.service.component.AlertDialogCostum
import com.komik.komik.service.enumclass.sourceType
import com.komik.komik.service.parser.model.getImages
import com.komik.komik.service.utils.downloadImageManager
import java.io.File

class AdapterImage(
    private val list: MutableList<getImages>,
    private val type: sourceType,
    var refererUrl: String? = null,
    var chapterNumberText: String = "",
    var hasNextChapter: Boolean = true,
    var onSingleTap: (() -> Unit)? = null,
    var onNextChapterClick: (() -> Unit)? = null,
    var onBackToDetailClick: (() -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_IMAGE = 0
        const val VIEW_TYPE_FOOTER = 1
    }

    inner class ImageViewHolder(
        val binding: ImagelayoutBinding,
        val type: sourceType,
        val refererUrl: String
    ) : RecyclerView.ViewHolder(binding.root) {

        val context = itemView.context

        fun bind(data: getImages) {
            // Reset state
            binding.loading.visibility = View.VISIBLE
            binding.layoutRetry.visibility = View.GONE
            binding.imagechapter.setImageDrawable(null)

            val listener = object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loading.visibility = View.GONE
                    binding.layoutRetry.visibility = View.VISIBLE
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    binding.loading.visibility = View.GONE
                    binding.layoutRetry.visibility = View.GONE
                    return false
                }
            }

            val imageStr = data.image.trim()
            val isLocal = imageStr.startsWith("/") || imageStr.startsWith("file://")

            if (isLocal) {
                // Mode Offline: Muat langsung dari File lokal tanpa header jaringan
                val localPath = if (imageStr.startsWith("file://")) imageStr.substring(7) else imageStr
                val localFile = File(localPath)
                if (localFile.exists() && localFile.length() > 0L) {
                    loadWithGlide(localFile, listener)
                } else {
                    binding.loading.visibility = View.GONE
                    binding.layoutRetry.visibility = View.VISIBLE
                }
            } else {
                // Mode Online: Tambahkan header referer & user-agent sesuai sumber
                when {
                    type == sourceType.doujindesu || imageStr.contains("desu.pics") || imageStr.contains("doujindesu") -> {
                        val effectiveReferer = if (!refererUrl.isNullOrBlank() && refererUrl.startsWith("http")) {
                            refererUrl
                        } else {
                            "https://doujin.desu.xxx/"
                        }
                        val headers = LazyHeaders.Builder()
                            .addHeader("Referer", effectiveReferer)
                            .addHeader("Origin", "https://doujin.desu.xxx")
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6_1; like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
                            )
                            .addHeader("X-Requested-With", "XMLHttpRequest")
                            .build()
                        val glideUrl = GlideUrl(imageStr, headers)
                        loadWithGlide(glideUrl, listener)
                    }
                    imageStr.contains("shngm.id") || type == sourceType.shinigami -> {
                        val headers = LazyHeaders.Builder()
                            .addHeader("Referer", "https://app.shinigami.asia/")
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            )
                            .build()
                        val glideUrl = GlideUrl(imageStr, headers)
                        loadWithGlide(glideUrl, listener)
                    }
                    type == sourceType.ikiru || imageStr.contains("ikiru") -> {
                        val effectiveReferer = if (!refererUrl.isNullOrBlank() && refererUrl.startsWith("http")) {
                            refererUrl
                        } else {
                            com.komik.komik.service.client.getIkiruUrl()
                        }
                        val headers = LazyHeaders.Builder()
                            .addHeader("Referer", effectiveReferer)
                            .addHeader("Origin", com.komik.komik.service.client.getIkiruUrl().trimEnd('/'))
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            )
                            .addHeader("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                            .build()
                        val glideUrl = GlideUrl(imageStr, headers)
                        loadWithGlide(glideUrl, listener)
                    }
                    !refererUrl.isNullOrBlank() && refererUrl.startsWith("http") -> {
                        val headers = LazyHeaders.Builder()
                            .addHeader("Referer", refererUrl)
                            .addHeader(
                                "User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            )
                            .build()
                        val glideUrl = GlideUrl(imageStr, headers)
                        loadWithGlide(glideUrl, listener)
                    }
                    else -> {
                        loadWithGlide(imageStr, listener)
                    }
                }
            }

            // Single tap on PhotoView or root toggles HUD
            binding.imagechapter.setOnViewTapListener { _, _, _ ->
                onSingleTap?.invoke()
            }
            binding.imagechapter.setOnPhotoTapListener { _, _, _ ->
                onSingleTap?.invoke()
            }
            binding.imagechapter.setOnClickListener {
                onSingleTap?.invoke()
            }
            binding.root.setOnClickListener {
                onSingleTap?.invoke()
            }

            // Long-press to download (khusus gambar online)
            binding.imagechapter.setOnLongClickListener {
                if (!isLocal) {
                    AlertDialogCostum(
                        context,
                        "Unduh Gambar",
                        "Apakah anda yakin ingin mendownload gambar ini?",
                        onPositive = { _ ->
                            downloadImageManager(context, data.image)
                        },
                        onNegative = { dialog ->
                            dialog.dismiss()
                        }
                    )
                }
                true
            }

            // Retry button
            binding.btnRetry.setOnClickListener {
                bind(data)
            }
        }

        private fun loadWithGlide(url: Any, listener: RequestListener<Drawable>) {
            Glide.with(context)
                .load(url)
                .listener(listener)
                .override(Target.SIZE_ORIGINAL)
                .into(binding.imagechapter)
        }
    }

    inner class FooterViewHolder(
        val binding: ItemChapterEndBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            val titleText = if (chapterNumberText.isNotEmpty()) {
                "Kamu telah menyelesaikan Chapter $chapterNumberText"
            } else {
                "Kamu telah menyelesaikan Chapter ini"
            }
            binding.tvChapterEndTitle.text = titleText

            if (hasNextChapter) {
                binding.btnNextChapterEnd.isEnabled = true
                binding.btnNextChapterEnd.alpha = 1.0f
                binding.btnNextChapterEnd.text = "Lanjut ke Chapter Berikutnya"
                binding.tvChapterEndSubtitle.text = "Lanjutkan petualangan membaca kamu ke chapter berikutnya"
                binding.btnNextChapterEnd.setOnClickListener {
                    onNextChapterClick?.invoke()
                }
            } else {
                binding.btnNextChapterEnd.isEnabled = false
                binding.btnNextChapterEnd.alpha = 0.5f
                binding.btnNextChapterEnd.text = "Sudah di Chapter Terakhir"
                binding.tvChapterEndSubtitle.text = "Selamat! Kamu telah membaca semua chapter yang tersedia."
                binding.btnNextChapterEnd.setOnClickListener(null)
            }

            binding.btnBackToDetail.setOnClickListener {
                onBackToDetailClick?.invoke()
            }

            binding.root.setOnClickListener {
                onSingleTap?.invoke()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < list.size) VIEW_TYPE_IMAGE else VIEW_TYPE_FOOTER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_IMAGE) {
            val binding = ImagelayoutBinding.inflate(inflater, parent, false)
            ImageViewHolder(binding, type, refererUrl.orEmpty())
        } else {
            val binding = ItemChapterEndBinding.inflate(inflater, parent, false)
            FooterViewHolder(binding)
        }
    }

    override fun getItemCount(): Int {
        return if (list.isEmpty()) 0 else list.size + 1
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ImageViewHolder && position < list.size) {
            holder.bind(list[position])
        } else if (holder is FooterViewHolder) {
            holder.bind()
        }
    }

    fun updateList(
        newList: List<getImages>,
        newReferer: String? = null,
        chapterNum: String = chapterNumberText,
        hasNext: Boolean = hasNextChapter
    ) {
        list.clear()
        list.addAll(newList)
        if (newReferer != null) refererUrl = newReferer
        chapterNumberText = chapterNum
        hasNextChapter = hasNext
        notifyDataSetChanged()
    }

    fun updateChapterInfo(chapterNum: String, hasNext: Boolean) {
        chapterNumberText = chapterNum
        hasNextChapter = hasNext
        if (list.isNotEmpty()) {
            notifyItemChanged(list.size)
        }
    }

    fun clear() {
        val size = list.size
        list.clear()
        notifyDataSetChanged()
    }
}
