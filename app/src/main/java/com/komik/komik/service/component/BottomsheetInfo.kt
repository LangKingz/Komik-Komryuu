package com.komik.komik.service.component

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.komik.komik.databinding.InfoBsBinding
import java.util.zip.Inflater

class BottomsheetInfo : BottomSheetDialogFragment() {

    private  var _binding: InfoBsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = InfoBsBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val url = arguments?.getString(ARG_URL)
        val size = arguments?.getString(ARG_SIZE)

        binding.sizeCard.text = "Jumlah isi Kontent : $size Komik"
        binding.webs.text = "Website : $url \nDeskripsi : Belum tersedia"

    }

    companion object{

        private const val ARG_URL = "arg_url"
        private const val ARG_SIZE = "arg_size"

        fun newInstance(url: String, size: String) : BottomsheetInfo{
            val fragment = BottomsheetInfo()
            val args = Bundle().apply {
                putString(ARG_URL,url)
                putString(ARG_SIZE,size)
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