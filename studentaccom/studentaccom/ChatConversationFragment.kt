package com.example.studentaccom

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatConversationFragment : Fragment() {

    companion object {
        private const val ARG_LISTING_ID    = "listing_id"
        private const val ARG_LANDLORD_NAME = "landlord_name"
        private const val ARG_LISTING_TITLE = "listing_title"

        fun newInstance(
            listingId: String,
            landlordName: String,
            listingTitle: String
        ) = ChatConversationFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_LISTING_ID,    listingId)
                putString(ARG_LANDLORD_NAME, landlordName)
                putString(ARG_LISTING_TITLE, listingTitle)
            }
        }
    }

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvLandlordName: TextView
    private lateinit var tvListingTitle: TextView
    private lateinit var ibBackChat: ImageButton
    private lateinit var adapter: MessageAdapter
    private var listenerReg: ValueEventListener? = null
    private var currentListingId: String = ""
    private var replyIndex = 0

    private val autoReplies = listOf(
        "Hello! Thank you for your interest. The room is still available.",
        "Yes, the deposit secures the room for you immediately.",
        "You are welcome to view the property this weekend between 9am and 4pm.",
        "Please bring your student ID and a copy of your registration letter.",
        "All utilities (water and electricity) are included in the monthly rent.",
        "The property is very close to public transport routes.",
        "I will send you the exact address once we confirm a viewing time.",
        "The contract is month-to-month with one month's notice required to vacate.",
        "Security is provided 24 hours. There is a gate with a secure lock.",
        "Thank you for reaching out! I look forward to meeting you."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_chat_conversation, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentListingId = arguments?.getString(ARG_LISTING_ID)    ?: ""
        val landlordName = arguments?.getString(ARG_LANDLORD_NAME) ?: "Landlord"
        val listingTitle = arguments?.getString(ARG_LISTING_TITLE) ?: ""

        bindViews(view, landlordName, listingTitle)
        setupRecyclerView()
        startListeningToMessages(currentListingId)
        markRead(currentListingId)

        btnSend.setOnClickListener { sendMessage(currentListingId, landlordName) }
    }

    private fun bindViews(view: View, landlordName: String, listingTitle: String) {
        rvMessages      = view.findViewById(R.id.rv_messages)
        etMessage       = view.findViewById(R.id.et_message_input)
        btnSend         = view.findViewById(R.id.btn_send_message)
        tvLandlordName  = view.findViewById(R.id.tv_chat_landlord_name)
        tvListingTitle  = view.findViewById(R.id.tv_chat_listing_title)
        ibBackChat      = view.findViewById(R.id.ib_back_chat)

        tvLandlordName.text = landlordName
        tvListingTitle.text = if (listingTitle.isNotEmpty()) "Re: $listingTitle"
        else "Accommodation enquiry"

        ibBackChat.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupRecyclerView() {
        val uid = FirebaseAuthManager.currentUserId
        adapter = MessageAdapter(uid)
        rvMessages.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        rvMessages.adapter = adapter
    }

    private fun startListeningToMessages(listingId: String) {
        val uid = FirebaseAuthManager.currentUserId
        // Fixed: Swapped uid and listingId to match FirebaseManager.listenToConversation(listingId, uid, ...)
        listenerReg = FirebaseManager.listenToConversation(listingId, uid) { messages ->
            if (!isAdded) return@listenToConversation
            adapter.submitList(messages.toMutableList())
            if (messages.isNotEmpty()) {
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }
    }

    private fun markRead(listingId: String) {
        val uid = FirebaseAuthManager.currentUserId
        lifecycleScope.launch {
            // Fixed: Swapped uid and listingId to match FirebaseManager.markMessagesRead(listingId, uid)
            FirebaseManager.markMessagesRead(listingId, uid)
        }
    }

    private fun sendMessage(listingId: String, landlordName: String) {
        val content = etMessage.text.toString().trim()
        if (content.isEmpty()) return

        val uid      = FirebaseAuthManager.currentUserId
        val userName = SessionManager(requireContext()).getUserName()

        val message = Message(
            senderId     = uid,
            receiverId   = "landlord_$listingId",
            listingId    = listingId,
            content      = content,
            timestamp    = System.currentTimeMillis(),
            isRead       = false,
            senderName   = userName,
            landlordName = landlordName
        )
        etMessage.text.clear()

        lifecycleScope.launch {
            FirebaseManager.sendMessage(message)

            // Simulated landlord auto-reply after a short delay
            delay(1500)
            val reply = autoReplies[replyIndex % autoReplies.size]
            replyIndex++

            val landlordReply = Message(
                senderId     = "landlord_$listingId",
                receiverId   = uid,
                listingId    = listingId,
                content      = reply,
                timestamp    = System.currentTimeMillis(),
                isRead       = false,
                senderName   = landlordName,
                landlordName = landlordName
            )
            FirebaseManager.sendMessage(landlordReply)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.let {
            // Fixed: Removed uid to match FirebaseManager.stopListening(listingId, listener)
            FirebaseManager.stopListening(currentListingId, it)
        }
    }
}
