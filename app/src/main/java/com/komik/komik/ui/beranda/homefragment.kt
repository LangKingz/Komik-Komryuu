package com.komik.komik.ui.beranda

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.komik.komik.R
import com.komik.komik.databinding.FragmentHomefragmentBinding
import com.komik.komik.service.adapter.AdapterRiwayatSource
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.adapter.AdapterSource
import com.komik.komik.service.api.model.DataItems
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.riwayatDao
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.dao.sourceDao
import com.komik.komik.service.data.data.DatabaseRiwayatSource
import com.komik.komik.service.data.model.RiwayatSource
import com.komik.komik.ui.pages.read.ReadActivity
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
    private lateinit var riwayatdao: riwayatRelationDao
    private lateinit var dbUtama : DatabaseRiwayats

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomefragmentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        viewmodel = ViewModelProvider(this).get(Homeviewmodel::class.java)

        db = DatabaseRiwayatSource.getDatabase(requireContext())
        dao = db.riwayatdao()
        dbUtama = DatabaseRiwayats.getDatabase(requireContext())
        riwayatdao = dbUtama.riwayatdao()

        init()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewmodel.source.observe(viewLifecycleOwner, { source ->
            adapter = AdapterSource(source as MutableList<DataItems>)
            binding.listSource.adapter = adapter

            val spanCount = 2

            val layoumanager = GridLayoutManager(requireContext(), spanCount)

            layoumanager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup(){
                override fun getSpanSize(position: Int): Int {
                    return when(adapter.getItemViewType(position)){
                        AdapterSource.VIEW_TYPE_HEADER -> spanCount
                        AdapterSource.VIEW_TYPE_ITEM -> 1
                        else -> 1
                    }
                }
            }

            binding.listSource.layoutManager = layoumanager
        })

        viewmodel.loading.observe(viewLifecycleOwner, { loading ->
            if (loading) {
                binding.loading.visibility = View.VISIBLE
                binding.layoutRefresh.visibility = View.GONE
            } else {
                binding.loading.visibility = View.GONE
                binding.layoutRefresh.visibility = View.GONE
            }
        })

        viewmodel.error.observe(viewLifecycleOwner, { error ->
            if (error != null) {
                Sweetalert(requireContext(), Sweetalert.ERROR_TYPE)
                    .setTitleText("Terjadi Kesalahan")
                    .setContentText(error)
                    .show()

                binding.loading.visibility = View.GONE

                binding.layoutRefresh.visibility = View.VISIBLE
            }
        })

        binding.nestedScrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (scrollY > 10) {
                binding.filter.text = "Filter"

            }
        }

        loadLatest()

        binding.refresh.setOnClickListener {
            viewmodel.fetchSource()
        }

    }

    fun loadLatest() {
        CoroutineScope(Dispatchers.IO).launch {
            val data = dao.getRiwayatSourceLive()
            val dataRiwayat = riwayatdao.getLastRiwayats()

            withContext(Dispatchers.Main) {
                data.observe(viewLifecycleOwner, { riwayat ->
                    if (riwayat.isNotEmpty()) {
                        binding.latestopen.adapter =
                            AdapterRiwayatSource(riwayat as MutableList<RiwayatSource>)
                        binding.latestopen.visibility = View.VISIBLE
                        binding.titleLatest.visibility = View.VISIBLE
                    } else {
                        binding.latestopen.visibility = View.GONE
                        binding.titleLatest.visibility = View.GONE
                    }
                })

                dataRiwayat.observe(viewLifecycleOwner){riwayat ->
                    if (riwayat != null) {
                        binding.textView.visibility = View.VISIBLE
                        binding.frameDragon.visibility = View.VISIBLE
                        binding.judulManhwa.text = riwayat.comic.title
                        binding.chapter.text = riwayat.chapter.title
                        Glide.with(requireContext())
                            .load(riwayat.comic.localImageUri)
                            .error(R.drawable.placeholder)
                            .into(binding.imageHistory)

                        val pageRead = riwayat.riwayat.pageRead
                        val totalPage = riwayat.riwayat.totalPage

// Cek dulu semua kemungkinan
                        val progressPercentage = if (pageRead != null && totalPage != null && totalPage > 0) {
                            (pageRead.toDouble() / totalPage.toDouble()) * 100
                        } else {
                            0
                        }
                        binding.progressBaca.progress = progressPercentage.toInt()


                        binding.frameDragon.setOnClickListener {
                            val intent = Intent(requireContext(), ReadActivity::class.java)
                            intent.putExtra(ReadActivity.EXTRA_URL, riwayat.chapter.link)
                            intent.putExtra(ReadActivity.EXTRA_TYPE, riwayat.comic.location)
                            intent.putExtra("num", riwayat.chapter.numChapter)
                            intent.putExtra(ReadActivity.EXTRA_ID, riwayat.chapter.id)
                            intent.putExtra(ReadActivity.EXTRA_TITLE, riwayat.comic.title)
                            intent.putExtra(ReadActivity.EXTRA_ID_COMIC, riwayat.comic.id)
                            intent.putExtra("pageRead", pageRead)
                            requireContext().startActivity(intent)
                        }
                    }else{
                        binding.frameDragon.visibility = View.GONE
                        binding.textView.visibility = View.GONE
                    }
                }

            }
        }
    }

    fun init (){
        binding.latestopen.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}