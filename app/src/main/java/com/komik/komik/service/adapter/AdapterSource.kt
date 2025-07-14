package com.komik.komik.service.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.komik.komik.databinding.CardSourceBinding
import com.komik.komik.ui.pages.source.sourceActivity
import com.komik.komik.service.api.model.ListItem
import com.komik.komik.service.database.dao.sourceDao
import com.komik.komik.service.database.data.DatabaseRiwayatSource
import com.komik.komik.service.database.model.RiwayatSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AdapterSource(private val list : MutableList<ListItem>): RecyclerView.Adapter<AdapterSource.SourceViewHolder>() {
    class SourceViewHolder(private val binding : CardSourceBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ListItem){
            binding.JudulExtensi.text = item.extensi
            binding.profilExtensi.text = item.extensi?.get(0).toString()
        }

        fun updateData(item: ListItem){
            val db: DatabaseRiwayatSource = DatabaseRiwayatSource.getDatabase(itemView.context)
            val dao: sourceDao = db.riwayatdao()

            CoroutineScope(Dispatchers.IO).launch {
                val data = dao.getRiwayatSource()
                if(data != null){
                    dao.updateRiwayatSource(
                        data.copy(
                            extensi = item.extensi.toString(),
                            url = item.url.toString(),
                            imageUrl = item.createdAt.toString(),
                            location = item.location.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }else{
                    dao.insertRiwayatSource(
                        RiwayatSource(
                            extensi = item.extensi.toString(),
                            url = item.url.toString(),
                            imageUrl = item.createdAt.toString(),
                            location = item.location.toString()
                        )
                    )
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SourceViewHolder {
        val binding = CardSourceBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return SourceViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: SourceViewHolder, position: Int) {
        holder.bind(list[position])

        holder.itemView.setOnClickListener {

            holder.updateData(list[position])

            val intent = Intent(holder.itemView.context, sourceActivity::class.java).apply {
                putExtra(sourceActivity.URL,list[position].url)
                putExtra(sourceActivity.location,list[position].location)
            }
            holder.itemView.context.startActivity(intent)
        }
    }
}