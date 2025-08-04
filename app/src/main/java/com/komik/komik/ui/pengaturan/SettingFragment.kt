package com.komik.komik.ui.pengaturan

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.komik.komik.R
import com.komik.komik.databinding.FragmentSettingBinding
import com.komik.komik.service.component.BottomSheetCookie


class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pref = requireActivity().getSharedPreferences("app_settings",Context.MODE_PRIVATE)
        val cookie = pref.getString("cookie","-")
        val fullscreen = pref.getBoolean("fullscreen",false)
        val chapterPindah = pref.getBoolean("pindah",false)
        val otomatisUpdate = pref.getBoolean("update",false)
        val safeMode = pref.getBoolean("safemode",false)

        binding.subCookie.text = cookie
        binding.fullscreen.isChecked = fullscreen
        binding.pindah.isChecked = chapterPindah
        binding.update.isChecked = otomatisUpdate
        binding.safemode.isChecked = safeMode

        binding.safemode.setOnCheckedChangeListener { buttonView, isChecked ->
            val editor = pref.edit()
            editor.putBoolean("safemode",isChecked)
            editor.apply()
        }


        binding.fullscreen.setOnCheckedChangeListener { _, isChecked ->
            val editor = pref.edit()
            editor.putBoolean("fullscreen",isChecked)
            editor.apply()
        }

        binding.pindah.setOnCheckedChangeListener { _, isChecked ->
            val editor = pref.edit()
            editor.putBoolean("pindah",isChecked)
            editor.apply()
        }

        binding.update.setOnCheckedChangeListener { _, isChecked ->
            val editor = pref.edit()
            editor.putBoolean("update",isChecked)
            editor.apply()
        }

        binding.CookieBs.setOnClickListener {
            val bottomSheet = BottomSheetCookie()
            bottomSheet.show(parentFragmentManager,"Cookie")
        }


    }

    override fun onResume() {
        super.onResume()


    }


}