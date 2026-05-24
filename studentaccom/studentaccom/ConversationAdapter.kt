package com.example.studentaccom


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentaccom.R
import com.example.studentaccom.Message
import java.text.SimpleDateFormat
import java.util.*

class ConversationAdapter(
    private val onItemClick: (Message) -> Unit
) : ListAdapter<Message, ConversationAdapter.ConvoViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConvoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConvoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConvoViewHolder, position: Int) {
        holder.bind(getItem(position), onItemClick)
    }

    class ConvoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvInitials:     TextView = itemView.findViewById(R.id.tv_convo_initials)
        private val tvLandlordName: TextView = itemView.findViewById(R.id.tv_convo_landlord_name)
        private val tvLastMessage:  TextView = itemView.findViewById(R.id.tv_convo_last_message)
        private val tvTime:         TextView = itemView.findViewById(R.id.tv_convo_time)
        private val tvUnread:       TextView = itemView.findViewById(R.id.tv_convo_unread)

        fun bind(message: Message, onItemClick: (Message) -> Unit) {
            val name = message.landlordName.ifEmpty { "Landlord" }
            tvInitials.text = name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2).joinToString("")
            tvLandlordName.text = name
            tvLastMessage.text  = message.content
            tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(message.timestamp))

            tvUnread.visibility = if (!message.isRead && message.senderId.startsWith("landlord"))
                View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(message) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(old: Message, new: Message) = old.listingId == new.listingId
        override fun areContentsTheSame(old: Message, new: Message) = old == new
    }
}