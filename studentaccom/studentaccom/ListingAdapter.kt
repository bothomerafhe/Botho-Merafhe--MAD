package com.example.studentaccom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.studentaccom.R
import com.example.studentaccom.Listing

class ListingAdapter(
    private val onItemClick: (Listing) -> Unit
) : ListAdapter<Listing, ListingAdapter.ListingViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivHouse:    ImageView = itemView.findViewById(R.id.iv_listing_image)
        private val tvTitle:    TextView  = itemView.findViewById(R.id.tv_listing_title)
        private val tvLocation: TextView  = itemView.findViewById(R.id.tv_listing_location)
        private val tvType:     TextView  = itemView.findViewById(R.id.tv_listing_type)
        private val tvPrice:    TextView  = itemView.findViewById(R.id.tv_listing_price)
        private val tvDeposit:  TextView  = itemView.findViewById(R.id.tv_listing_deposit)
        private val tvAvail:    TextView  = itemView.findViewById(R.id.tv_listing_avail_date)
        private val tvStatus:   TextView  = itemView.findViewById(R.id.tv_listing_status)
        private val tvAmenities:TextView  = itemView.findViewById(R.id.tv_listing_amenities)

        fun bind(listing: Listing, onItemClick: (Listing) -> Unit) {
            tvTitle.text    = listing.title
            tvLocation.text = "📍 ${listing.location}"
            tvType.text     = listing.type
            tvPrice.text    = "BWP ${listing.price.toInt()}/month"
            tvDeposit.text  = "Deposit: BWP ${listing.depositAmount.toInt()}"
            tvAvail.text    = "Available: ${listing.availabilityDate}"

            val amenitiesPreview = listing.amenities
                .split(",").take(3)
                .joinToString("  •  ") { it.trim() }
            tvAmenities.text = amenitiesPreview

            // Using Coil for real house pictures via URL
            if (listing.imageUrl.isNotEmpty()) {
                ivHouse.load(listing.imageUrl) {
                    crossfade(true)
                    placeholder(R.drawable.img_house_1) // fallback placeholder
                    error(R.drawable.img_house_1)
                }
            } else {
                ivHouse.setImageResource(R.drawable.img_house_1)
            }

            if (listing.isReserved) {
                tvStatus.text = "Reserved"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_reserved)
            } else {
                tvStatus.text = "Available"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_available)
            }
            tvStatus.setTextColor(
                ContextCompat.getColor(itemView.context, R.color.white)
            )

            itemView.setOnClickListener { onItemClick(listing) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(old: Listing, new: Listing) = old.id == new.id
        override fun areContentsTheSame(old: Listing, new: Listing) = old == new
    }
}