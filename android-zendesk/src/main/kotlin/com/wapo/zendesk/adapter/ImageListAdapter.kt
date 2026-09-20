package com.wapo.zendesk.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.wapo.zendesk.databinding.ImageListItemBinding
import com.wapo.zendesk.model.Status
import com.wapo.zendesk.model.ZendeskImage

class ImageListAdapter :
    ListAdapter<ZendeskImage, ImageListAdapter.ImageItemViewHolder>(ZendeskImageDiffCallback) {
    class ImageItemViewHolder(val binding: ImageListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    var onCloseClicked: ((ZendeskImage, Int) -> Unit)? = null
    var onImageClicked: ((ZendeskImage, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ImageListItemBinding.inflate(inflater, parent, false)
        return ImageItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageItemViewHolder, position: Int) {
        val zendeskImage = getItem(position)
        holder.binding.apply {
            if (image.tag != zendeskImage.uri) {
                Glide.with(holder.itemView.context)
                    .load(zendeskImage.uri)
                    .centerCrop()
                    .into(image)
            }
            image.tag = zendeskImage.uri
            bindProgress(zendeskImage.status, this)
            buttonClose.setOnClickListener {
                val image = getItem(holder.adapterPosition)
                onCloseClicked?.invoke(image, holder.adapterPosition)
            }
            image.setOnClickListener {
                val image = getItem(holder.adapterPosition)
                onImageClicked?.invoke(image, holder.adapterPosition)
            }
        }
    }

    private fun bindProgress(status: Status, binding: ImageListItemBinding) {
        when (status) {
            Status.IDLE -> {
                binding.image.alpha = 1f
                binding.imageProgress.visibility = View.GONE
                binding.iconDone.visibility = View.GONE
            }
            Status.PENDING -> {
                binding.image.alpha = 0.5f
                binding.imageProgress.visibility = View.GONE
                binding.iconDone.visibility = View.GONE
            }
            Status.PROGRESS -> {
                binding.image.alpha = 1f
                binding.imageProgress.visibility = View.VISIBLE
                binding.iconDone.visibility = View.GONE
            }
            Status.FINISHED -> {
                binding.image.alpha = 1f
                binding.imageProgress.visibility = View.GONE
                binding.iconDone.visibility = View.VISIBLE
            }
        }
    }
}

object ZendeskImageDiffCallback : DiffUtil.ItemCallback<ZendeskImage>() {
    override fun areItemsTheSame(oldItem: ZendeskImage, newItem: ZendeskImage): Boolean {
        return oldItem.uri == newItem.uri
    }

    override fun areContentsTheSame(oldItem: ZendeskImage, newItem: ZendeskImage): Boolean {
        return oldItem == newItem && oldItem.status == newItem.status
    }
}