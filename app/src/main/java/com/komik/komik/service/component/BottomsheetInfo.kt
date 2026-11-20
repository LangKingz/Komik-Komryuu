package com.komik.komik.service.component

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.komik.komik.R
import com.komik.komik.databinding.InfoBsBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

class BottomsheetInfo : BottomSheetDialogFragment() {

    private var _binding: InfoBsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val url = arguments?.getString(ARG_URL).orEmpty()
        val name = arguments?.getString(ARG_NAME) ?: "Ekstensi Sumber"
        val logo = arguments?.getString(ARG_LOGO).orEmpty()
        val size = arguments?.getString(ARG_SIZE) ?: "300+"

        binding.tvBsTitle.text = name
        binding.webs.text = if (url.isNotEmpty()) url else "Tidak tersedia"
        binding.sizeCard.text = "$size Komik"

        if (logo.isNotEmpty()) {
            Glide.with(this)
                .load(logo)
                .placeholder(R.drawable.ppanime)
                .error(R.drawable.ppanime)
                .into(binding.imgBsLogo)
        }

        binding.btnVisitWebsite.setOnClickListener {
            if (url.isNotEmpty()) {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                } catch (_: Exception) {}
            }
        }

        measureServerPing(url)
    }

    private fun measureServerPing(url: String) {
        if (url.isBlank()) {
            binding.tvBsPing.text = "URL tidak valid"
            return
        }

        binding.tvBsPing.text = "Mengukur latensi server..."

        CoroutineScope(Dispatchers.IO).launch {
            val startTime = System.currentTimeMillis()
            try {
                val targetUrl = if (!url.startsWith("http")) "https://$url" else url
                val request = Request.Builder()
                    .url(targetUrl)
                    .head()
                    .build()

                val okHttpClient = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val response = okHttpClient.newCall(request).execute()
                val duration = System.currentTimeMillis() - startTime
                val isSuccess = response.isSuccessful || response.code in 200..399

                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    val speedQuality = when {
                        duration < 150 -> "Sangat Cepat"
                        duration < 400 -> "Normal"
                        else -> "Lambat"
                    }
                    binding.tvBsPing.text = "$duration ms ($speedQuality)"

                    if (isSuccess) {
                        binding.tvBsServerStatus.text = "● Online / Aktif"
                        binding.tvBsServerStatus.setBackgroundResource(R.drawable.badge_status_online)
                        binding.tvBsServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_emerald_400))
                    } else {
                        binding.tvBsServerStatus.text = "● Maintenance (${response.code})"
                        binding.tvBsServerStatus.setBackgroundResource(R.drawable.badge_status_offline)
                        binding.tvBsServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (_binding == null) return@withContext
                    binding.tvBsPing.text = "Timeout / Terproteksi"
                    binding.tvBsServerStatus.text = "● Maintenance / Offline"
                    binding.tvBsServerStatus.setBackgroundResource(R.drawable.badge_status_offline)
                    binding.tvBsServerStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
                }
            }
        }
    }

    companion object {
        private const val ARG_URL = "arg_url"
        private const val ARG_NAME = "arg_name"
        private const val ARG_LOGO = "arg_logo"
        private const val ARG_SIZE = "arg_size"

        fun newInstance(
            url: String,
            size: String = "300+",
            name: String = "Ekstensi Sumber",
            logo: String = ""
        ): BottomsheetInfo {
            val fragment = BottomsheetInfo()
            val args = Bundle().apply {
                putString(ARG_URL, url)
                putString(ARG_NAME, name)
                putString(ARG_LOGO, logo)
                putString(ARG_SIZE, size)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}