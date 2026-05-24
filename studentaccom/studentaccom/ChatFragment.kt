package com.example.studentaccom
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.Message
import com.example.studentaccom.ConversationAdapter

class ChatFragment : Fragment() {

    private lateinit var rvConversations: RecyclerView
    private lateinit var tvEmptyChat: TextView
    private lateinit var adapter: ConversationAdapter
    private var listenerReg: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_chat, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvConversations = view.findViewById(R.id.rv_conversations)
        tvEmptyChat     = view.findViewById(R.id.tv_empty_chat)

        adapter = ConversationAdapter { message -> openConversation(message) }
        rvConversations.layoutManager = LinearLayoutManager(requireContext())
        rvConversations.adapter = adapter

        val uid = FirebaseAuthManager.currentUserId
        listenerReg = FirebaseManager.listenToAllConversations(uid) { messages ->
            if (!isAdded) return@listenToAllConversations

            val conversations = messages
                .groupBy { it.listingId }
                .mapValues { entry -> entry.value.maxByOrNull { it.timestamp }!! }
                .values
                .sortedByDescending { it.timestamp }
                .toMutableList()

            if (conversations.isEmpty()) {
                tvEmptyChat.visibility     = View.VISIBLE
                rvConversations.visibility = View.GONE
            } else {
                tvEmptyChat.visibility     = View.GONE
                rvConversations.visibility = View.VISIBLE
                adapter.submitList(conversations)
            }
        }
    }

    private fun openConversation(message: Message) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                ChatConversationFragment.newInstance(
                    message.listingId,
                    message.landlordName,
                    ""
                )
            )
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.let { FirebaseManager.stopListeningAll(it) }
    }
}