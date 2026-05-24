package com.example.studentaccom

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.ListenerRegistration
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.Listing
import com.example.studentaccom.ListingAdapter
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private lateinit var rvListings: RecyclerView
    private lateinit var tvResultCount: TextView
    private lateinit var tvWelcome: TextView
    private lateinit var tvAvailableCount: TextView
    private lateinit var chipGroupAreas: ChipGroup
    private lateinit var ibFilter: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: ListingAdapter

    private var allListings: List<Listing> = emptyList()
    private var selectedLocation = ""
    private var filterMinPrice = 0.0
    private var filterMaxPrice = 0.0
    private var filterDate = ""
    private var filterType = ""
    private var listenerReg: ListenerRegistration? = null

    private val areas = listOf(
        "All","Gaborone West","Village","Tlokweng",
        "Mogoditshane","Broadhurst","Phakalane","Extension 11","Block 8"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)
        setupRecyclerView()
        setupAreaChips()
        startListeningToListings()

        val name = SessionManager(requireContext())
            .getUserName().split(" ").firstOrNull() ?: "Student"
        tvWelcome.text = "Good day, $name 👋"

        ibFilter.setOnClickListener { showFilterSheet() }
    }

    private fun bindViews(view: View) {
        rvListings       = view.findViewById(R.id.rv_listings)
        tvResultCount    = view.findViewById(R.id.tv_result_count)
        tvWelcome        = view.findViewById(R.id.tv_welcome)
        tvAvailableCount = view.findViewById(R.id.tv_available_count)
        chipGroupAreas   = view.findViewById(R.id.chip_group_areas)
        ibFilter         = view.findViewById(R.id.ib_filter)
        progressBar      = view.findViewById(R.id.progress_bar)
    }

    private fun setupRecyclerView() {
        adapter = ListingAdapter { listing -> openDetail(listing) }
        rvListings.layoutManager = LinearLayoutManager(requireContext())
        rvListings.adapter = adapter
    }

    private fun setupAreaChips() {
        chipGroupAreas.removeAllViews()
        areas.forEach { area ->
            val chip = Chip(requireContext()).apply {
                text = area
                isCheckable = true
                isChecked = area == "All"
                setChipBackgroundColorResource(R.color.chip_background_selector)
                setTextColor(resources.getColorStateList(R.color.chip_text_selector, null))
            }
            chip.setOnClickListener {
                selectedLocation = if (area == "All") "" else area
                applyFilters()
            }
            chipGroupAreas.addView(chip)
        }
    }

    private fun startListeningToListings() {
        progressBar.visibility = View.VISIBLE

        listenerReg = FirebaseManager.listenToListings { listings, error ->
            if (!isAdded) return@listenToListings

            progressBar.visibility = View.GONE

            if (error != null) {
                Toast.makeText(requireContext(), "Error loading listings: ${error.message}", Toast.LENGTH_SHORT).show()
                return@listenToListings
            }

            allListings = listings

            // Calculate available count (listings that are not reserved)
            val availableCount = listings.count { listing -> !listing.isReserved }
            val totalSize = listings.size
            val areasCount = areas.size - 1 // excluding "All"

            tvAvailableCount.text = "$availableCount available · $totalSize total · $areasCount areas"
            applyFilters()
        }
    }

    private fun applyFilters() {
        var filtered = allListings

        if (selectedLocation.isNotEmpty()) {
            filtered = filtered.filter { it.location.equals(selectedLocation, ignoreCase = true) }
        }

        if (filterMinPrice > 0.0) {
            filtered = filtered.filter { it.price >= filterMinPrice }
        }

        if (filterMaxPrice > 0.0) {
            filtered = filtered.filter { it.price <= filterMaxPrice }
        }

        if (filterDate.isNotEmpty()) {
            filtered = filtered.filter { it.availabilityDate <= filterDate }
        }

        if (filterType.isNotEmpty()) {
            filtered = filtered.filter { it.type.equals(filterType, ignoreCase = true) }
        }

        adapter.submitList(filtered.toMutableList())
        tvResultCount.text = "${filtered.size} found"
    }

    private fun showFilterSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_filter, null)

        val etMin       = sheetView.findViewById<EditText>(R.id.et_min_price)
        val etMax       = sheetView.findViewById<EditText>(R.id.et_max_price)
        val etDate      = sheetView.findViewById<EditText>(R.id.et_avail_date)
        val spinType    = sheetView.findViewById<Spinner>(R.id.spinner_type)
        val spinLoc     = sheetView.findViewById<Spinner>(R.id.spinner_location_filter)
        val btnApply    = sheetView.findViewById<Button>(R.id.btn_apply_filter)
        val btnClear    = sheetView.findViewById<Button>(R.id.btn_clear_filter)

        if (filterMinPrice > 0.0) etMin.setText(filterMinPrice.toInt().toString())
        if (filterMaxPrice > 0.0) etMax.setText(filterMaxPrice.toInt().toString())
        if (filterDate.isNotEmpty()) etDate.setText(filterDate)

        val types = listOf("All Types","Single Room","Double Room","Bachelor Flat",
            "1-Bed Apartment","2-Bed Apartment","Ensuite Room","Shared House")
        val typeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinType.adapter = typeAdapter

        val locOptions = listOf("Any Area","Gaborone West","Village","Tlokweng",
            "Mogoditshane","Broadhurst","Phakalane","Extension 11","Block 8")
        val locAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, locOptions)
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinLoc.adapter = locAdapter

        btnApply.setOnClickListener {
            filterMinPrice = etMin.text.toString().toDoubleOrNull() ?: 0.0
            filterMaxPrice = etMax.text.toString().toDoubleOrNull() ?: 0.0
            filterDate = etDate.text.toString().trim()
            filterType = if (spinType.selectedItemPosition == 0) "" else spinType.selectedItem.toString()

            val selectedLocationFromSheet = if (spinLoc.selectedItemPosition == 0) "" else spinLoc.selectedItem.toString()
            if (selectedLocationFromSheet.isNotEmpty()) {
                selectedLocation = selectedLocationFromSheet
            }

            applyFilters()
            dialog.dismiss()
        }

        btnClear.setOnClickListener {
            filterMinPrice = 0.0
            filterMaxPrice = 0.0
            filterDate = ""
            filterType = ""
            selectedLocation = ""
            applyFilters()
            dialog.dismiss()
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun openDetail(listing: Listing) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ListingDetailFragment.newInstance(listing.id))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerReg?.remove()
    }
}