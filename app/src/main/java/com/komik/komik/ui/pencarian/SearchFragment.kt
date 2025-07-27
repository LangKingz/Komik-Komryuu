package com.komik.komik.ui.pencarian

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.carousel.CarouselLayoutManager
import com.komik.komik.R
import com.komik.komik.databinding.FragmentSearchBinding
import com.komik.komik.service.api.model.DataItem
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.adapter.AdapterCorousel
import com.komik.komik.service.adapter.AdapterDaily
import com.komik.komik.service.adapter.AdapterSource

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewmodel: Searchviewmodel
    private lateinit var adapter: AdapterCorousel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        val root = binding.root

        viewmodel = ViewModelProvider(this).get(Searchviewmodel::class.java)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvCarousel.layoutManager = CarouselLayoutManager()
        binding.rvComicList.layoutManager = GridLayoutManager(requireContext(),2)

//        viewmodel.corousel.observe(viewLifecycleOwner,{data->
//            adapter = AdapterCorousel(data as List<DataItem>)
//            binding.rvCarousel.adapter = adapter
//        })

//        viewmodel.daily.observe(viewLifecycleOwner,{data->
//            val adapter = AdapterSource(data as MutableList<ListItem>)
//            binding.rvComicList.adapter = adapter
//        })

//        viewmodel.loading.observe(viewLifecycleOwner,{loading->
//            if(loading){
//                binding.loading.visibility = View.VISIBLE
//            }else{
//                binding.loading.visibility = View.GONE
//            }
//        })
    }

}