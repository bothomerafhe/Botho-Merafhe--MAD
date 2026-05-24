package com.example.studentaccom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.Listing
import kotlinx.coroutines.launch

class ListingDetailFragment : Fragment() {

    companion object {
        private const val ARG_LISTING_ID = "listing_id"
        fun newInstance(listingId: String): ListingDetailFragment {
            return ListingDetailFragment().apply {
                arguments = Bundle().apply { putString(ARG_LISTING_ID, listingId) }
            }
        }
    }

    private lateinit var ivHouseImage: ImageView
    private lateinit var tvTitle: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvType: TextView
    private lateinit var tvPrice: TextView
    private lateinit var tvDeposit: TextView
    private lateinit var tvAvailDate: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvAmenities: TextView
    private lateinit var tvLandlordName: TextView
    private lateinit var tvLandlordPhone: TextView
    private lateinit var tvStatus: TextView
    private lateinit var btnReserve: Button
    private lateinit var btnChat: Button
    private lateinit var btnViewOnMap: Button
    private lateinit var ibBack: ImageButton
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_listing_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        ibBack.setOnClickListener { parentFragmentManager.popBackStack() }

        val listingId = arguments?.getString(ARG_LISTING_ID)
        if (listingId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Listing ID not found", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        loadListing(listingId)
    }

    private fun bindViews(view: View) {
        ivHouseImage     = view.findViewById(R.id.iv_house_image)
        tvTitle          = view.findViewById(R.id.tv_detail_title)
        tvLocation       = view.findViewById(R.id.tv_detail_location)
        tvType           = view.findViewById(R.id.tv_detail_type)
        tvPrice          = view.findViewById(R.id.tv_detail_price)
        tvDeposit        = view.findViewById(R.id.tv_detail_deposit)
        tvAvailDate      = view.findViewById(R.id.tv_detail_avail_date)
        tvDescription    = view.findViewById(R.id.tv_detail_description)
        tvAmenities      = view.findViewById(R.id.tv_detail_amenities)
        tvLandlordName   = view.findViewById(R.id.tv_landlord_name)
        tvLandlordPhone  = view.findViewById(R.id.tv_landlord_phone)
        tvStatus         = view.findViewById(R.id.tv_detail_status)
        btnReserve       = view.findViewById(R.id.btn_reserve)
        btnChat          = view.findViewById(R.id.btn_chat_landlord)
        btnViewOnMap     = view.findViewById(R.id.btn_view_on_map)
        ibBack           = view.findViewById(R.id.ib_back)
        progressBar      = view.findViewById(R.id.progress_detail)
    }

    private fun loadListing(listingId: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val listing = FirebaseManager.getListingById(listingId)
                progressBar.visibility = View.GONE

                if (listing != null) {
                    populateViews(listing)
                } else {
                    Toast.makeText(requireContext(), "Listing not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading listing", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateViews(listing: Listing) {
        tvTitle.text       = listing.title
        tvLocation.text    = "📍 ${listing.location}, Gaborone"
        tvType.text        = listing.type
        tvPrice.text       = "BWP ${listing.price.toInt()}/month"

        // FIXED: Standard deposit is 1 month's rent (not 25%)
        // This ensures deposit is never higher than rent in an unreasonable way
        val deposit = if (listing.depositAmount > 0) {
            // If custom deposit is set, ensure it's reasonable (between 0 and 2x rent)
            listing.depositAmount.toInt().coerceIn(0, listing.price.toInt() * 2)
        } else {
            // Default to 1 month's rent as deposit (standard practice)
            listing.price.toInt()
        }

        tvDeposit.text     = "Deposit: BWP $deposit"
        tvAvailDate.text   = "Available from: ${listing.availabilityDate}"
        tvDescription.text = listing.description
        tvLandlordName.text = listing.landlordName
        tvLandlordPhone.text = listing.landlordPhone

        val amenityText = if (listing.amenities.isNullOrBlank()) {
            "• WiFi\n• Water Included\n• Secure Parking"
        } else {
            listing.amenities.split(",")
                .joinToString("\n") { "✓  ${it.trim()}" }
        }
        tvAmenities.text = amenityText

        // Load real house picture using Coil with crossfade for smooth loading
        if (listing.imageUrl.isNotEmpty()) {
            ivHouseImage.load(listing.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.img_house_1)
                error(R.drawable.img_house_1)
            }
        } else {
            ivHouseImage.setImageResource(R.drawable.img_house_1)
        }

        btnViewOnMap.setOnClickListener {
            openInGoogleMaps(listing)
        }

        btnChat.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ChatConversationFragment.newInstance(
                    listing.id,
                    listing.landlordName,
                    listing.title
                ))
                .addToBackStack(null)
                .commit()
        }

        val uid = FirebaseAuthManager.currentUserId
        val isMyReservation = listing.isReserved && listing.reservedByUserId == uid

        when {
            isMyReservation -> {
                tvStatus.text = "Reserved by You"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_available)
                btnReserve.text = "Already Reserved"
                btnReserve.isEnabled = false
                btnReserve.alpha = 0.6f
            }
            listing.isReserved -> {
                tvStatus.text = "🔒 Reserved"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_reserved)
                btnReserve.text = "Room Unavailable"
                btnReserve.isEnabled = false
                btnReserve.alpha = 0.6f
            }
            else -> {
                tvStatus.text = "✅ Available"
                tvStatus.setBackgroundResource(R.drawable.bg_badge_available)
                btnReserve.text = "Reserve — Pay BWP $deposit Deposit"
                btnReserve.isEnabled = true
                btnReserve.alpha = 1f
                btnReserve.setOnClickListener {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PaymentFragment.newInstance(listing.id))
                        .addToBackStack(null)
                        .commit()
                }
            }
        }
    }

    private fun openInGoogleMaps(listing: Listing) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${listing.latitude},${listing.longitude}&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(mapIntent)
        } else {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${listing.latitude},${listing.longitude}")
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }
}