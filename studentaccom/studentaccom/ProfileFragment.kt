package com.example.studentaccom

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ListenerRegistration
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.LoginActivity
import com.example.studentaccom.ReservationAdapter
import com.example.studentaccom.SessionManager

class ProfileFragment : Fragment() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfileStudentId: TextView
    private lateinit var tvProfileUniversity: TextView
    private lateinit var tvReservationCount: TextView
    private lateinit var rvReservations: RecyclerView
    private lateinit var btnLogout: Button
    private lateinit var tvInitials: TextView
    private lateinit var reservationAdapter: ReservationAdapter
    private var listenerReg: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        populateProfile()
        loadReservations()

        btnLogout.setOnClickListener { confirmLogout() }
    }

    private fun bindViews(view: View) {
        tvProfileName       = view.findViewById(R.id.tv_profile_name)
        tvProfileEmail      = view.findViewById(R.id.tv_profile_email)
        tvProfileStudentId  = view.findViewById(R.id.tv_profile_student_id)
        tvProfileUniversity = view.findViewById(R.id.tv_profile_university)
        tvReservationCount  = view.findViewById(R.id.tv_reservation_count)
        rvReservations      = view.findViewById(R.id.rv_reservations)
        btnLogout           = view.findViewById(R.id.btn_logout)
        tvInitials          = view.findViewById(R.id.tv_profile_initials)

        reservationAdapter = ReservationAdapter()
        rvReservations.layoutManager = LinearLayoutManager(requireContext())
        rvReservations.adapter = reservationAdapter
    }

    private fun populateProfile() {
        val session = SessionManager(requireContext())
        val name    = session.getUserName()
        val email   = session.getUserEmail()
        val sid     = session.getStudentId()
        val uni     = session.getUniversity()

        tvProfileName.text       = name.ifEmpty { "Student User" }
        tvProfileEmail.text      = email.ifEmpty { "email@example.com" }
        tvProfileStudentId.text  = "Student ID: ${sid.ifEmpty { "Not provided" }}"
        tvProfileUniversity.text = uni.ifEmpty { "University not specified" }

        // Generate initials from name
        val initials = if (name.isNotEmpty()) {
            name.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
        } else {
            "S"
        }
        tvInitials.text = initials
    }

    private fun loadReservations() {
        val uid = FirebaseAuthManager.currentUserId
        if (uid.isEmpty()) { // Fixed: Using isEmpty() since currentUserId returns String (not nullable)
            tvReservationCount.text = "0 Reservation(s)"
            return
        }

        listenerReg = FirebaseManager.listenToReservations(uid) { reservations: List<Reservation>, error: Exception? ->
            if (!isAdded) return@listenToReservations

            if (error != null) {
                Toast.makeText(requireContext(), "Error loading reservations: ${error.message}", Toast.LENGTH_SHORT).show()
                tvReservationCount.text = "0 Reservation(s)"
                return@listenToReservations
            }

            val reservationList = reservations ?: emptyList()
            val count = reservationList.size
            tvReservationCount.text = "$count Reservation(s)"

            reservationAdapter.submitList(reservationList)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle("Log Out")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Log Out") { _, _ ->
                // Clear local session
                SessionManager(requireContext()).clearSession()

                // Sign out from Firebase Auth (using logout method from FirebaseAuthManager)
                FirebaseAuthManager.logout()

                // Navigate to Login Activity
                startActivity(Intent(requireContext(), LoginActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
    }
}