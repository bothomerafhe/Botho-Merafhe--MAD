package com.example.studentaccom


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.Listing
import com.example.studentaccom.ListingAdapter

class MapFragment : Fragment() {

    private lateinit var rvNearbyListings: RecyclerView
    private lateinit var btnDirectionsUB: Button
    private lateinit var btnDirectionsBUCT: Button
    private lateinit var btnDirectionsBaisago: Button
    private lateinit var tvNearbyCount: TextView
    private lateinit var spinnerAreaFilter: Spinner
    private lateinit var progressMap: ProgressBar
    private lateinit var adapter: ListingAdapter

    private val areaDistances = mapOf(
        "Village"        to "1.2 km",
        "Extension 11"   to "2.5 km",
        "Broadhurst"     to "3.1 km",
        "Block 8"        to "3.7 km",
        "Gaborone West"  to "4.8 km",
        "Tlokweng"       to "5.2 km",
        "Mogoditshane"   to "9.4 km",
        "Phakalane"      to "11.6 km"
    )

    private val areaKeys = arrayOf(
        "Village","Extension 11","Broadhurst","Block 8",
        "Gaborone West","Tlokweng","Mogoditshane","Phakalane"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_map, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupAreaSpinner()
        setupCampusButtons()
        loadNearbyListings("Village")
    }

    private fun bindViews(view: View) {
        rvNearbyListings    = view.findViewById(R.id.rv_nearby_listings)
        btnDirectionsUB     = view.findViewById(R.id.btn_directions_ub)
        btnDirectionsBUCT   = view.findViewById(R.id.btn_directions_buct)
        btnDirectionsBaisago = view.findViewById(R.id.btn_directions_baisago)
        tvNearbyCount       = view.findViewById(R.id.tv_nearby_count)
        spinnerAreaFilter   = view.findViewById(R.id.spinner_area_filter)
        progressMap         = view.findViewById(R.id.progress_map)

        adapter = ListingAdapter { listing -> openDetail(listing) }
        rvNearbyListings.layoutManager = LinearLayoutManager(requireContext())
        rvNearbyListings.adapter = adapter
    }

    private fun setupAreaSpinner() {
        val displayAreas = arrayOf(
            "Village (1.2 km from UB)",
            "Extension 11 (2.5 km)",
            "Broadhurst (3.1 km)",
            "Block 8 (3.7 km)",
            "Gaborone West (4.8 km)",
            "Tlokweng (5.2 km)",
            "Mogoditshane (9.4 km)",
            "Phakalane (11.6 km)"
        )
        spinnerAreaFilter.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, displayAreas
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerAreaFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                loadNearbyListings(areaKeys[pos])
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupCampusButtons() {
        btnDirectionsUB.setOnClickListener {
            openMaps(-24.6544, 25.9083, "University of Botswana")
        }
        btnDirectionsBUCT.setOnClickListener {
            openMaps(-24.6781, 25.9222, "Botswana University of Science and Technology")
        }
        btnDirectionsBaisago.setOnClickListener {
            openMaps(-24.6573, 25.9062, "BAISAGO University Gaborone")
        }
    }

    private fun openMaps(lat: Double, lng: Double, label: String) {
        val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://maps.google.com/?q=$lat,$lng")))
        }
    }

    private fun loadNearbyListings(area: String) {
        progressMap.visibility = View.VISIBLE
        lifecycleScope.launch {
            val listings = FirebaseManager.getListingsByLocation(area)
            progressMap.visibility = View.GONE
            val dist = areaDistances[area] ?: ""
            tvNearbyCount.text = "${listings.size} listings in $area · $dist from UB"
            adapter.submitList(listings.toMutableList())
        }
    }

    private fun openDetail(listing: Listing) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ListingDetailFragment.newInstance(listing.id))
            .addToBackStack(null).commit()
    }
}