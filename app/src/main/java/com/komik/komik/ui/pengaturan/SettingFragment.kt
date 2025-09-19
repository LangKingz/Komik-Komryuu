package com.komik.komik.ui.pengaturan

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.komik.komik.R
import com.komik.komik.databinding.FragmentSettingBinding
import com.komik.komik.service.component.BottomSheetCookie
import java.util.jar.Manifest


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
        val notif = pref.getBoolean("notif",false)

        binding.subCookie.text = cookie
        binding.fullscreen.isChecked = fullscreen
        binding.pindah.isChecked = chapterPindah
        binding.update.isChecked = otomatisUpdate
        binding.safemode.isChecked = safeMode
        binding.Notification.isChecked = notif

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

        binding.Notification.setOnCheckedChangeListener { _, isChecked ->
            launcherPermissionNotfication.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }


    }

    private val launcherPermissionNotfication = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){granted ->
        if (granted) {
            val pref = requireActivity().getSharedPreferences("app_settings",Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("notif",true)
            editor.apply()
        }else{
            val pref = requireActivity().getSharedPreferences("app_settings",Context.MODE_PRIVATE)
            val editor = pref.edit()
            editor.putBoolean("notif",false)
            editor.apply()
            Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()


    }


}