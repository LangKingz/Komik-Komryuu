package com.komik.komik.ui.beranda

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import cn.pedant.SweetAlert.SweetAlertDialog
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.FragmentHomefragmentBinding
import com.komik.komik.service.adapter.AdapterRiwayatSource
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.adapter.AdapterSource
import com.komik.komik.service.database.dao.sourceDao
import com.komik.komik.service.database.data.DatabaseRiwayatSource
import com.komik.komik.service.database.model.RiwayatSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import taimoor.sultani.sweetalert2.Sweetalert

class homefragment : Fragment() {

    var _binding: FragmentHomefragmentBinding? = null
    val binding get() = _binding!!
    private lateinit var adapter: AdapterSource
    private lateinit var viewmodel: Homeviewmodel

    private lateinit var db: DatabaseRiwayatSource
    private lateinit var dao: sourceDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomefragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewmodel = ViewModelProvider(this).get(Homeviewmodel::class.java)

        db = DatabaseRiwayatSource.getDatabase(requireContext())
        dao = db.riwayatdao()



        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        inisialisasi recyclerview
        binding.latestopen.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.listSource.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        viewmodel.source.observe(viewLifecycleOwner, { source ->
            adapter = AdapterSource(source as MutableList<ListItem>)
            binding.listSource.adapter = adapter
        })

        viewmodel.loading.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                binding.loading.visibility = View.VISIBLE
            } else {
                binding.loading.visibility = View.GONE
            }
        })

        viewmodel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                Sweetalert(requireContext(), Sweetalert.ERROR_TYPE)
                    .setTitleText("Terjadi Kesalahan")
                    .setContentText(error)
                    .show()
            }
        })

        binding.nestedScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > 10) {
                binding.filter.text = "Filter"

            }
        }

        loadLatest()

    }

    fun loadLatest() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = dao.getRiwayatSourceLive()

            withContext(Dispatchers.Main) {
                if (data != null) {
                    data.observe(viewLifecycleOwner, { riwayat ->
                        if (riwayat != null) {

                            binding.latestopen.adapter =
                                AdapterRiwayatSource(riwayat as MutableList<RiwayatSource>)
                            binding.latestopen.visibility = View.VISIBLE
                            binding.titleLatest.visibility = View.VISIBLE
                        }
                    })
                } else {
                    binding.latestopen.visibility = View.GONE
                    binding.titleLatest.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}