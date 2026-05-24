package com.example.studentaccom


import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.AlertPreference
import com.example.studentaccom.NotificationHelper
import kotlinx.coroutines.launch
class AlertsFragment : Fragment() {

    private lateinit var swPriceAlert: Switch
    private lateinit var swLocationAlert: Switch
    private lateinit var swDateAlert: Switch
    private lateinit var etMinPrice: EditText
    private lateinit var etMaxPrice: EditText
    private lateinit var spinnerLocation: Spinner
    private lateinit var etAvailDate: EditText
    private lateinit var btnSavePrefs: Button
    private lateinit var btnTestAlert: Button
    private lateinit var progressAlerts: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ) = inflater.inflate(R.layout.fragment_alerts, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupLocationSpinner()
        loadSavedPreferences()

        btnSavePrefs.setOnClickListener { savePreferences() }
        btnTestAlert.setOnClickListener {
            NotificationHelper.sendPriceAlert(
                requireContext(), "Kgosi Lodge 5 — Test Alert", 1800.0, 999
            )
            Toast.makeText(requireContext(), "Test notification sent!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindViews(view: View) {
        swPriceAlert    = view.findViewById(R.id.sw_price_alert)
        swLocationAlert = view.findViewById(R.id.sw_location_alert)
        swDateAlert     = view.findViewById(R.id.sw_date_alert)
        etMinPrice      = view.findViewById(R.id.et_alert_min_price)
        etMaxPrice      = view.findViewById(R.id.et_alert_max_price)
        spinnerLocation = view.findViewById(R.id.spinner_alert_location)
        etAvailDate     = view.findViewById(R.id.et_alert_date)
        btnSavePrefs    = view.findViewById(R.id.btn_save_preferences)
        btnTestAlert    = view.findViewById(R.id.btn_test_alert)
        progressAlerts  = view.findViewById(R.id.progress_alerts)
    }

    private fun setupLocationSpinner() {
        val areas = arrayOf("Any Area","Gaborone West","Village","Tlokweng",
            "Mogoditshane","Broadhurst","Phakalane","Extension 11","Block 8")
        spinnerLocation.adapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item, areas).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun loadSavedPreferences() {
        val uid = FirebaseAuthManager.currentUserId
        progressAlerts.visibility = View.VISIBLE
        lifecycleScope.launch {
            val pref = FirebaseManager.getAlertPreference(uid)
            progressAlerts.visibility = View.GONE
            pref?.let {
                swPriceAlert.isChecked    = it.priceAlertEnabled
                swLocationAlert.isChecked = it.locationAlertEnabled
                swDateAlert.isChecked     = it.dateAlertEnabled
                if (it.minPrice > 0) etMinPrice.setText(it.minPrice.toInt().toString())
                if (it.maxPrice < 10000) etMaxPrice.setText(it.maxPrice.toInt().toString())
                if (it.availabilityDate.isNotEmpty()) etAvailDate.setText(it.availabilityDate)
                val areas = arrayOf("Any Area","Gaborone West","Village","Tlokweng",
                    "Mogoditshane","Broadhurst","Phakalane","Extension 11","Block 8")
                val idx = areas.indexOf(it.preferredLocation).takeIf { i -> i >= 0 } ?: 0
                spinnerLocation.setSelection(idx)
            }
        }
    }

    private fun savePreferences() {
        val uid      = FirebaseAuthManager.currentUserId
        val minPrice = etMinPrice.text.toString().toDoubleOrNull() ?: 0.0
        val maxPrice = etMaxPrice.text.toString().toDoubleOrNull() ?: 10000.0
        val location = if (spinnerLocation.selectedItemPosition == 0) ""
        else spinnerLocation.selectedItem.toString()
        val date = etAvailDate.text.toString().trim()

        progressAlerts.visibility = View.VISIBLE
        btnSavePrefs.isEnabled = false

        lifecycleScope.launch {
            val pref = AlertPreference(
                userId = uid,
                minPrice = minPrice,
                maxPrice = maxPrice,
                preferredLocation = location,
                availabilityDate = date,
                priceAlertEnabled = swPriceAlert.isChecked,
                locationAlertEnabled = swLocationAlert.isChecked,
                dateAlertEnabled = swDateAlert.isChecked
            )
            FirebaseManager.saveAlertPreference(pref)

            // Fire matching notifications
            if (pref.priceAlertEnabled) {
                val matches = FirebaseManager.getMatchingListings(minPrice, maxPrice)
                matches.take(3).forEachIndexed { i, listing ->
                    NotificationHelper.sendPriceAlert(
                        requireContext(), listing.title, listing.price, i + 100
                    )
                }
            }
            if (pref.locationAlertEnabled && location.isNotEmpty()) {
                val locMatches = FirebaseManager.getListingsByLocation(location)
                if (locMatches.isNotEmpty()) {
                    NotificationHelper.sendLocationAlert(
                        requireContext(), location, locMatches.size, 200
                    )
                }
            }

            progressAlerts.visibility = View.GONE
            btnSavePrefs.isEnabled = true
            Toast.makeText(requireContext(), "\u2705 Preferences saved!", Toast.LENGTH_SHORT).show()
        }
    }
}