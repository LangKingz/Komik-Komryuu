package com.komik.komik.service.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.komik.komik.R
import com.komik.komik.databinding.ImagelayoutBinding
import com.komik.komik.service.parser.model.getImages

class AdapterImage(private val list: List<getImages>): RecyclerView.Adapter<AdapterImage.ViewHolder>() {
    class ViewHolder(val binding: ImagelayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(data: getImages) {
            Glide.with(itemView.context)
                .load(data.image)
                .listener(object : RequestListener<Drawable>{
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loading.visibility = View.GONE
                        binding.imagechapter.setImageResource(R.drawable.placeholder)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding.loading.visibility = View.GONE
                        return false
                    }
                })
                .override(Target.SIZE_ORIGINAL)
                .into(binding.imagechapter)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ImagelayoutBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.bind(list[position])
    }

}