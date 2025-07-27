package com.komik.komik.ui.pustaka

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.komik.komik.R
import com.komik.komik.databinding.FragmentPustakaBinding
import com.komik.komik.service.adapter.AdapterPustaka
import com.komik.komik.service.data.Database.DatabaseRiwayats
import com.komik.komik.service.data.dao.LibraryDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PustakaFragment : Fragment() {
    private var _binding: FragmentPustakaBinding? = null
    val binding get() = _binding!!

    private lateinit var adapter: AdapterPustaka

    private lateinit var db: DatabaseRiwayats
    private lateinit var library: LibraryDao

    private lateinit var viewmodel: PustakaViewmodel

    private var isGrid :Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPustakaBinding.inflate(inflater, container, false)
        val root = binding.root

        setHasOptionsMenu(true)

        init()

        binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), 3)
        adapter = AdapterPustaka(mutableListOf())

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fetchLiveData()
    }

    private fun fetchLiveData() {
        binding.loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val data = library.getLibrary()
            withContext(Dispatchers.Main) {
                data.observe(viewLifecycleOwner) { data ->
                    if (data.isNotEmpty()) {
                        adapter.setData(data)
                        binding.listPustaka.adapter = adapter
                        binding.loading.visibility = View.GONE
                    } else {
                        binding.placeholder.visibility = View.VISIBLE
                        binding.listPustaka.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun search(query: String){
        CoroutineScope(Dispatchers.IO).launch {
            val data = library.searchFavoriteLibraryComics(query)

            withContext(Dispatchers.Main){
                data.observe(viewLifecycleOwner) { data ->
                    if (data.isNotEmpty()) {
                        adapter.updateData(data)
                        binding.listPustaka.adapter = adapter
                        binding.loading.visibility = View.GONE
                        binding.placeholder.visibility = View.GONE
                    } else {
                        binding.placeholder.visibility = View.VISIBLE
                        adapter.clearData()
                    }
                }
            }

        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_pustaka, menu)

        val searchItem = menu.findItem(R.id.search_pustaka)
        val filter = menu.findItem(R.id.filter)
        val grid = menu.findItem(R.id.grid_layout)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.apply {
            setOnQueryTextListener(object : SearchView.OnQueryTextListener,
                androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    search(newText.toString())
                    return false
                }
            })
        }

        filter.setOnMenuItemClickListener {
            Snackbar.make(binding.root, "Coming Soon", Snackbar.LENGTH_SHORT).show()
            false
        }

        grid.setOnMenuItemClickListener {
            isGrid = !isGrid
            if(isGrid){
                binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), 2)
            }else{
                binding.listPustaka.layoutManager = GridLayoutManager(requireContext(), 3)
            }

//            Snackbar.make(binding.root, "Coming Soon", Snackbar.LENGTH_SHORT).show()
            false
        }
    }


    private fun init() {
        db = DatabaseRiwayats.getDatabase(requireContext())
        library = db.libraryDao()

        viewmodel = ViewModelProvider(this, PustakaProvider(library))[PustakaViewmodel::class]
        (activity as androidx.appcompat.app.AppCompatActivity).setSupportActionBar(binding.appbar)
    }

}