package com.komik.komik.service.adapter

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.CardSourceBinding
import com.komik.komik.databinding.HeaderExtensiBinding
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.component.BottomsheetInfo
import com.komik.komik.service.data.dao.sourceDao
import com.komik.komik.service.data.data.DatabaseRiwayatSource
import com.komik.komik.service.data.model.RiwayatSource
import com.komik.komik.service.utils.PreferenceManager
import com.komik.komik.service.utils.isNetworkAvaible
import com.komik.komik.ui.pages.source.sourceActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AdapterSource(private val rawList: MutableList<DataItems>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ITEM = 1
        const val PREF_PINNED = PreferenceManager.PREF_USER
        const val KEY_PINNED = PreferenceManager.KEY_PINNED_SOURCES

        private val pingClient = OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        private val pingCache = ConcurrentHashMap<String, Long>()
        private val pingChecking = ConcurrentHashMap<String, Boolean>()
    }

    private val masterList = ArrayList(rawList)
    private var displayedList = ArrayList(rawList)
    private var groupItems: List<Any> = emptyList()
    private var currentCategory: String = "Semua"
    private var currentQuery: String = ""
    private var pinnedKeysCache = mutableSetOf<String>()
    private var userRole: String = "user"

    init {
        rebuildGroupedItems()
    }

    private fun rebuildGroupedItems() {
        if (displayedList.isEmpty()) {
            groupItems = emptyList()
            return
        }

        val items = mutableListOf<Any>()

        // 1. Filter Pinned vs Unpinned (Pin Source ke Atas Permanen)
        val pinned = displayedList.filter { isItemPinned(it.id ?: it.name ?: "") }
        val unpinned = displayedList.filter { !isItemPinned(it.id ?: it.name ?: "") }

        if (pinned.isNotEmpty()) {
            items.add("Favorit / Disematkan")
            items.addAll(pinned)
        }

        // 2. Group remaining by type/category
        val grouped = unpinned.groupBy { it.type ?: "Komik" }
        for ((type, listInType) in grouped) {
            items.add(type)
            items.addAll(listInType)
        }

        groupItems = items
    }

    private fun isItemPinned(key: String): Boolean {
        if (key.isBlank()) return false
        return pinnedKeysCache.contains(key)
    }

    fun initPinnedCache(context: Context) {
        val prefManager = PreferenceManager.getInstance(context)
        userRole = prefManager.userRole
        pinnedKeysCache = HashSet(prefManager.getPinnedSources())
        applyFilters()
    }

    private fun isPrivilegedUser(): Boolean {
        val r = userRole.lowercase().trim()
        return r == "admin" || r == "moderator"
    }

    private fun togglePin(context: Context, key: String): Boolean {
        if (key.isBlank()) return false
        val prefManager = PreferenceManager.getInstance(context)
        val isNowPinned = prefManager.togglePinSource(key)
        pinnedKeysCache = HashSet(prefManager.getPinnedSources())
        rebuildGroupedItems()
        notifyDataSetChanged()
        return isNowPinned
    }

    fun filter(query: String) {
        currentQuery = query.trim()
        applyFilters()
    }

    fun filterByCategory(category: String) {
        currentCategory = category
        applyFilters()
    }

    fun filterWithCategory(query: String, category: String) {
        currentQuery = query.trim()
        currentCategory = category
        applyFilters()
    }

    private fun applyFilters() {
        val filtered = masterList.filter { item ->
            // Sembunyikan ekstensi yang isActive == false kecuali Admin/Moderator
            val matchesActive = isPrivilegedUser() || (item.isActive != false)

            val matchesCategory = when (currentCategory) {
                "Semua" -> true
                "Disematkan" -> isItemPinned(item.id ?: item.name ?: "")
                "Dewasa / 18+" -> item.type?.equals("dewasa", ignoreCase = true) == true
                else -> item.type?.contains(currentCategory, ignoreCase = true) == true ||
                        item.location?.contains(currentCategory, ignoreCase = true) == true
            }

            val matchesQuery = if (currentQuery.isEmpty()) {
                true
            } else {
                item.name?.contains(currentQuery, ignoreCase = true) == true ||
                item.location?.contains(currentQuery, ignoreCase = true) == true ||
                item.type?.contains(currentQuery, ignoreCase = true) == true ||
                item.baseUrl?.contains(currentQuery, ignoreCase = true) == true
            }

            matchesActive && matchesCategory && matchesQuery
        }

        displayedList = ArrayList(filtered)
        rebuildGroupedItems()
        notifyDataSetChanged()
    }

    fun updateData(newData: List<DataItems>) {
        masterList.clear()
        masterList.addAll(newData)
        applyFilters()
    }

    fun refreshPings() {
        pingCache.clear()
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (groupItems[position] is String) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun getItemCount(): Int = groupItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            val binding = HeaderExtensiBinding.inflate(inflater, parent, false)
            HeaderViewHolder(binding)
        } else {
            val binding = CardSourceBinding.inflate(inflater, parent, false)
            SourceViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                holder.bind(groupItems[position] as String)
            }
            is SourceViewHolder -> {
                val currentItem = groupItems[position] as DataItems
                val context = holder.itemView.context

                if (pinnedKeysCache.isEmpty()) {
                    val prefManager = PreferenceManager.getInstance(context)
                    userRole = prefManager.userRole
                    pinnedKeysCache = HashSet(prefManager.getPinnedSources())
                }

                val key = currentItem.id ?: currentItem.name ?: ""
                val isPinned = pinnedKeysCache.contains(key)

                holder.bind(currentItem, isPinned, onPinClick = {
                    val pinnedNow = togglePin(context, key)
                    val message = if (pinnedNow) {
                        "Sumber '${currentItem.name}' disematkan ke atas"
                    } else {
                        "Pin sumber '${currentItem.name}' dilepas"
                    }
                    Snackbar.make(holder.itemView, message, Snackbar.LENGTH_SHORT)
                        .setTextColor(ContextCompat.getColor(context, R.color.white))
                        .setBackgroundTint(ContextCompat.getColor(context, R.color.bottom_navbar))
                        .show()
                }, onQuickSearchClick = {
                    handleSourceClick(context, currentItem, autoFocusSearch = true, view = holder.itemView)
                }, onCheckPingClick = {
                    checkLivePing(context, currentItem, holder)
                })

                holder.binding.btnInfoSource.setOnClickListener {
                    val activity = context as? AppCompatActivity
                    if (activity != null) {
                        val bottomSheet = BottomsheetInfo.newInstance(
                            url = currentItem.baseUrl.orEmpty(),
                            size = "300+",
                            name = currentItem.name ?: "Sumber Komik",
                            logo = currentItem.logo.orEmpty()
                        )
                        bottomSheet.show(activity.supportFragmentManager, "bottomsheet_info")
                    }
                }

                // Klik biasa: Buka sumber (dengan pengecekan VIP)
                holder.itemView.setOnClickListener {
                    handleSourceClick(context, currentItem, autoFocusSearch = false, view = holder.itemView)
                }

                // Long click: Quick Search langsung di sumber
                holder.itemView.setOnLongClickListener {
                    handleSourceClick(context, currentItem, autoFocusSearch = true, view = holder.itemView)
                    true
                }
            }
        }
    }

    private fun handleSourceClick(context: Context, item: DataItems, autoFocusSearch: Boolean, view: View) {
        val prefManager = PreferenceManager.getInstance(context)
        if (item.isVipOnly == true && !prefManager.isVipOrAbove()) {
            Snackbar.make(view, "🔒 Sumber '${item.name}' khusus untuk member VIP!", Snackbar.LENGTH_LONG)
                .setAction("Info VIP") {
                    Toast.makeText(context, "Status VIP diperlukan untuk membaca di sumber ini.", Toast.LENGTH_SHORT).show()
                }
                .show()
            return
        }
        openSource(context, item, autoFocusSearch)
    }

    private fun openSource(context: Context, item: DataItems, autoFocusSearch: Boolean) {
        saveRiwayat(context, item)

        if (isNetworkAvaible(context)) {
            val intent = Intent(context, sourceActivity::class.java).apply {
                putExtra(sourceActivity.URL, item.baseUrl)
                putExtra(sourceActivity.location, item.location)
                putExtra(sourceActivity.LOGOURL, item.logo)
                if (autoFocusSearch) {
                    putExtra(sourceActivity.EXTRA_AUTO_FOCUS_SEARCH, true)
                }
            }
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Tidak ada koneksi internet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLivePing(context: Context, item: DataItems, holder: SourceViewHolder) {
        val url = item.baseUrl
        val sourceKey = item.id ?: item.name ?: url.orEmpty()

        holder.binding.tvPing.text = "Menguji..."
        holder.binding.ivPingDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
        Toast.makeText(context, "Mengukur ping '${item.name}'...", Toast.LENGTH_SHORT).show()

        measureLatency(url) { latency ->
            pingCache[sourceKey] = latency
            holder.applyPingUI(latency)
        }
    }

    private fun measureLatency(url: String?, onComplete: (Long) -> Unit) {
        if (url.isNullOrBlank()) {
            onComplete(-1L)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val start = System.currentTimeMillis()
            val latency = try {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
                    .build()
                val resp = pingClient.newCall(req).execute()
                val duration = System.currentTimeMillis() - start
                val success = resp.isSuccessful || resp.code in 200..399 || resp.code == 403
                resp.close()
                if (success) duration else -1L
            } catch (_: Exception) {
                -1L
            }

            withContext(Dispatchers.Main) {
                onComplete(latency)
            }
        }
    }

    private fun saveRiwayat(context: Context, item: DataItems) {
        val db = DatabaseRiwayatSource.getDatabase(context)
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
                    )
                )
            } else {
                dao.insertRiwayatSource(
                    RiwayatSource(
                        extensi = item.name.toString(),
                        url = item.baseUrl.toString(),
                        imageUrl = item.logo.toString(),
                        location = item.location.toString(),
                    )
                )
            }
        }
    }

    inner class HeaderViewHolder(val binding: HeaderExtensiBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.headerText.text = title
        }
    }

    inner class SourceViewHolder(val binding: CardSourceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: DataItems,
            isPinned: Boolean,
            onPinClick: () -> Unit,
            onQuickSearchClick: () -> Unit,
            onCheckPingClick: () -> Unit
        ) {
            val context = itemView.context
            binding.JudulExtensi.text = item.name ?: "Sumber"
            binding.profilExtensi.text = item.name?.firstOrNull()?.toString()?.uppercase() ?: "S"

            binding.tvRegion.text = item.location?.take(8) ?: "ID"
            binding.tvCategory.text = item.type?.replaceFirstChar { it.uppercase() } ?: "Komik"
            binding.tvVersion.text = "v1.2.0"

            if (item.type?.equals("dewasa", ignoreCase = true) == true) {
                binding.tag.visibility = View.VISIBLE
                binding.tag.text = "18+"
            } else {
                binding.tag.visibility = View.GONE
            }

            // Lencana VIP Only
            if (item.isVipOnly == true) {
                binding.badgeVipOnly.visibility = View.VISIBLE
            } else {
                binding.badgeVipOnly.visibility = View.GONE
            }

            // Image logo & avatar fallback
            if (!item.logo.isNullOrBlank()) {
                binding.imageView7.visibility = View.VISIBLE
                binding.profilExtensi.visibility = View.GONE
                Glide.with(context)
                    .load(item.logo)
                    .placeholder(R.drawable.ppanime)
                    .error(R.drawable.ppanime)
                    .into(binding.imageView7)
            } else {
                binding.imageView7.visibility = View.GONE
                binding.profilExtensi.visibility = View.VISIBLE
            }

            // Pin Indicator
            if (isPinned) {
                binding.star.setImageResource(R.drawable.push_pin_fill)
                binding.star.setColorFilter(ContextCompat.getColor(context, R.color.color_star))
            } else {
                binding.star.setImageResource(R.drawable.push_pin)
                binding.star.setColorFilter(ContextCompat.getColor(context, R.color.gray_400))
            }

            binding.star.setOnClickListener { onPinClick() }
            binding.btnSearchSource.setOnClickListener { onQuickSearchClick() }
            binding.btnCheckPing.setOnClickListener { onCheckPingClick() }

            // Ping Cache & UI
            val sourceKey = item.id ?: item.name ?: item.baseUrl.orEmpty()
            val cachedPing = pingCache[sourceKey]

            if (cachedPing != null) {
                applyPingUI(cachedPing)
            } else {
                applyPingUI(null)
                if (pingChecking[sourceKey] != true && !item.baseUrl.isNullOrBlank()) {
                    pingChecking[sourceKey] = true
                    measureLatency(item.baseUrl) { latency ->
                        pingCache[sourceKey] = latency
                        pingChecking.remove(sourceKey)
                        applyPingUI(latency)
                    }
                }
            }
        }

        fun applyPingUI(ping: Long?) {
            val context = itemView.context
            when {
                ping == null -> {
                    binding.tvPing.text = "-- ms"
                    binding.tvPing.setTextColor(ContextCompat.getColor(context, R.color.gray_400))
                    binding.ivPingDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9CA3AF"))
                    binding.badgeServerStatus.text = "● Siap"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                    binding.badgeServerStatus.setTextColor(ContextCompat.getColor(context, R.color.text_emerald_400))
                }
                ping < 0 -> {
                    binding.tvPing.text = "Offline"
                    binding.tvPing.setTextColor(ContextCompat.getColor(context, R.color.red))
                    binding.ivPingDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                    binding.badgeServerStatus.text = "● Maintenance"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_offline)
                    binding.badgeServerStatus.setTextColor(ContextCompat.getColor(context, R.color.red))
                }
                ping < 300 -> {
                    binding.tvPing.text = "${ping}ms"
                    binding.tvPing.setTextColor(ContextCompat.getColor(context, R.color.text_emerald_400))
                    binding.ivPingDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                    binding.badgeServerStatus.text = "● Online"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                    binding.badgeServerStatus.setTextColor(ContextCompat.getColor(context, R.color.text_emerald_400))
                }
                else -> {
                    binding.tvPing.text = "${ping}ms"
                    binding.tvPing.setTextColor(Color.parseColor("#F59E0B"))
                    binding.ivPingDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                    binding.badgeServerStatus.text = "● Online"
                    binding.badgeServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                    binding.badgeServerStatus.setTextColor(Color.parseColor("#F59E0B"))
                }
            }
        }
    }
}
