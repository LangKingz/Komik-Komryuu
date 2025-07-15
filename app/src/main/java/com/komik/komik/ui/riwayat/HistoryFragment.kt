package com.komik.komik.ui.riwayat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.komik.komik.databinding.FragmentHistoryBinding
import com.komik.komik.service.adapter.AdapterRiwayat
import com.komik.komik.service.data.dao.riwayatRelationDao
import com.komik.komik.service.data.Database.DatabaseRiwayats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HistoryFragment : Fragment() {

    private lateinit var _binding: FragmentHistoryBinding
    private val binding get() = _binding

    private lateinit var db: DatabaseRiwayats
    private lateinit var dao:riwayatRelationDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       _binding= FragmentHistoryBinding.inflate(inflater,container,false)

        db = DatabaseRiwayats.getDatabase(requireContext())
        dao = db.riwayatdao()

        val root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.listRiwayat.layoutManager = LinearLayoutManager(requireContext())
        CoroutineScope(Dispatchers.IO).launch {
            val data = dao.getRiwayatDetail()
           withContext(Dispatchers.Main){
               if(data != null){
                   data.observe(viewLifecycleOwner){list->
                       binding.listRiwayat.adapter = AdapterRiwayat(list)
                   }
               }else{
                   binding.listRiwayat.adapter = AdapterRiwayat(mutableListOf())
               }
           }
        }

    }

}