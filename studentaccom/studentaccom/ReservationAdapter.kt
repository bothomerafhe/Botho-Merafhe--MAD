package com.example.studentaccom

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentaccom.R
import com.example.studentaccom.Reservation

class ReservationAdapter : ListAdapter<Reservation, ReservationAdapter.ReservationViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ReservationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReservationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle:    TextView = itemView.findViewById(R.id.tv_res_title)
        private val tvLocation: TextView = itemView.findViewById(R.id.tv_res_location)
        private val tvRef:      TextView = itemView.findViewById(R.id.tv_res_ref)
        private val tvAmount:   TextView = itemView.findViewById(R.id.tv_res_amount)
        private val tvDate:     TextView = itemView.findViewById(R.id.tv_res_date)
        private val tvMethod:   TextView = itemView.findViewById(R.id.tv_res_method)
        private val tvStatus:   TextView = itemView.findViewById(R.id.tv_res_status)

        fun bind(reservation: Reservation) {
            tvTitle.text    = reservation.listingTitle
            // Changed Unicode escape to actual emoji
            tvLocation.text = "📍 ${reservation.location}"
            tvRef.text      = "Ref: ${reservation.referenceNumber}"
            tvAmount.text   = "BWP ${reservation.depositPaid.toInt()} paid"
            tvDate.text     = reservation.paymentDate
            tvMethod.text   = reservation.paymentMethod

            // Handle status with proper formatting and colors
            val statusText = reservation.status.uppercase()
            tvStatus.text = statusText

            // Set status color based on value (optional but recommended)
            when (reservation.status.lowercase()) {
                "completed", "confirmed" -> {
                    tvStatus.setTextColor(itemView.context.getColor(R.color.success_green))
                }
                "pending" -> {
                    tvStatus.setTextColor(itemView.context.getColor(R.color.warning_orange))
                }
                "cancelled" -> {
                    tvStatus.setTextColor(itemView.context.getColor(R.color.danger_red))
                }
                else -> {
                    // Use default color
                }
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Reservation>() {
        override fun areItemsTheSame(old: Reservation, new: Reservation): Boolean {
            return old.referenceNumber == new.referenceNumber
        }

        override fun areContentsTheSame(old: Reservation, new: Reservation): Boolean {
            return old == new
        }
    }
}