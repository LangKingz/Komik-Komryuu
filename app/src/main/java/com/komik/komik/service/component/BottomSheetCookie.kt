package com.komik.komik.service.component

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.komik.komik.R
import com.komik.komik.databinding.CookieBsBinding

class BottomSheetCookie() : BottomSheetDialogFragment() {

    private lateinit var binding: CookieBsBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val bottomsheet: BottomSheetDialog = BottomSheetDialog(
            requireContext()
        )
        binding = CookieBsBinding.inflate(layoutInflater)
        bottomsheet.setContentView(binding.root)

        val pref = requireActivity().getSharedPreferences("app_settings",Context.MODE_PRIVATE)
        val cookie = pref.getString("cookie","-")
        binding.cookie.setText(cookie)

        bottomsheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        return bottomsheet
    }
}