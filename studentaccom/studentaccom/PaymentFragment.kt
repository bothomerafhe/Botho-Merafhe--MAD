package com.example.studentaccom

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.Reservation
import com.example.studentaccom.NotificationHelper
import java.util.*

class PaymentFragment : Fragment() {

    companion object {
        private const val ARG_LISTING_ID = "listing_id"
        fun newInstance(listingId: String): PaymentFragment {
            return PaymentFragment().apply {
                arguments = Bundle().apply { putString(ARG_LISTING_ID, listingId) }
            }
        }
    }

    private var selectedPaymentMethod = "Orange Money"
    private var listingTitle = ""
    private var listingLocation = ""
    private var depositAmount = 0.0

    private lateinit var layoutStep1: LinearLayout
    private lateinit var layoutStep2: LinearLayout
    private lateinit var tvListingTitle: TextView
    private lateinit var tvDepositAmount: TextView
    private lateinit var tvProcessingFee: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var etMobileNumber: EditText
    private lateinit var layoutCardFields: LinearLayout
    private lateinit var etCardNumber: EditText
    private lateinit var etCardExpiry: EditText
    private lateinit var etCardCvv: EditText
    private lateinit var btnPay: Button
    private lateinit var btnCancel: Button
    private lateinit var tvReceiptRef: TextView
    private lateinit var tvReceiptListing: TextView
    private lateinit var tvReceiptLocation: TextView
    private lateinit var tvReceiptAmount: TextView
    private lateinit var tvReceiptDate: TextView
    private lateinit var tvReceiptMethod: TextView
    private lateinit var btnDone: Button
    private lateinit var progressPayment: ProgressBar
    private lateinit var ibBack: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_payment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        loadListing()
        setupPaymentMethods()
    }

    private fun bindViews(view: View) {
        layoutStep1       = view.findViewById(R.id.layout_payment_step1)
        layoutStep2       = view.findViewById(R.id.layout_payment_step2)
        tvListingTitle    = view.findViewById(R.id.tv_payment_listing_title)
        tvDepositAmount   = view.findViewById(R.id.tv_deposit_amount)
        tvProcessingFee   = view.findViewById(R.id.tv_processing_fee)
        tvTotalAmount     = view.findViewById(R.id.tv_total_amount)
        etMobileNumber    = view.findViewById(R.id.et_mobile_number)
        layoutCardFields  = view.findViewById(R.id.layout_card_fields)
        etCardNumber      = view.findViewById(R.id.et_card_number)
        etCardExpiry      = view.findViewById(R.id.et_card_expiry)
        etCardCvv         = view.findViewById(R.id.et_card_cvv)
        btnPay            = view.findViewById(R.id.btn_pay_now)
        btnCancel         = view.findViewById(R.id.btn_cancel_payment)
        tvReceiptRef      = view.findViewById(R.id.tv_receipt_ref)
        tvReceiptListing  = view.findViewById(R.id.tv_receipt_listing)
        tvReceiptLocation = view.findViewById(R.id.tv_receipt_location)
        tvReceiptAmount   = view.findViewById(R.id.tv_receipt_amount)
        tvReceiptDate     = view.findViewById(R.id.tv_receipt_date)
        tvReceiptMethod   = view.findViewById(R.id.tv_receipt_method)
        btnDone           = view.findViewById(R.id.btn_done)
        progressPayment   = view.findViewById(R.id.progress_payment)
        ibBack            = view.findViewById(R.id.ib_payment_back)

        ibBack.setOnClickListener    { parentFragmentManager.popBackStack() }
        btnCancel.setOnClickListener { parentFragmentManager.popBackStack() }
        btnDone.setOnClickListener {
            parentFragmentManager.popBackStack(null,
                androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        btnPay.setOnClickListener { processPayment() }
    }

    private fun loadListing() {
        val listingId = arguments?.getString(ARG_LISTING_ID)
        if (listingId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Listing ID not found", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        lifecycleScope.launch {
            try {
                val listing = FirebaseManager.getListingById(listingId)
                listing?.let { listingData ->
                    listingTitle = listingData.title
                    listingLocation = listingData.location
                    depositAmount = listingData.depositAmount

                    tvListingTitle.text = listingData.title
                    tvDepositAmount.text = "BWP ${listingData.depositAmount.toInt()}"
                    tvProcessingFee.text = "BWP 25"
                    tvTotalAmount.text = "BWP ${(listingData.depositAmount + 25).toInt()}"
                } ?: run {
                    Toast.makeText(requireContext(), "Listing not found", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading listing: ${e.message}", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun setupPaymentMethods() {
        val rgMethods = view?.findViewById<RadioGroup>(R.id.rg_payment_methods)
        if (rgMethods == null) {
            return
        }

        rgMethods.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            when (checkedId) {
                R.id.rb_orange_money -> {
                    selectedPaymentMethod = "Orange Money"
                    layoutCardFields.visibility = View.GONE
                    etMobileNumber.visibility = View.VISIBLE
                    etMobileNumber.hint = "Orange Money number"
                }
                R.id.rb_my_zaka -> {
                    selectedPaymentMethod = "MyZaka"
                    layoutCardFields.visibility = View.GONE
                    etMobileNumber.visibility = View.VISIBLE
                    etMobileNumber.hint = "Mascom number"
                }
                R.id.rb_card -> {
                    selectedPaymentMethod = "Card"
                    layoutCardFields.visibility = View.VISIBLE
                    etMobileNumber.visibility = View.GONE
                }
            }
        }
    }

    private fun processPayment() {
        val listingId = arguments?.getString(ARG_LISTING_ID)
        if (listingId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Listing ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate inputs
        when {
            selectedPaymentMethod != "Card" -> {
                val mobileNumber = etMobileNumber.text.toString().trim()
                if (mobileNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter your mobile number", Toast.LENGTH_SHORT).show()
                    return
                }
                if (mobileNumber.length < 10) {
                    Toast.makeText(requireContext(), "Please enter a valid mobile number", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            selectedPaymentMethod == "Card" -> {
                val cardNumber = etCardNumber.text.toString().trim()
                if (cardNumber.length < 16) {
                    Toast.makeText(requireContext(), "Enter a valid card number", Toast.LENGTH_SHORT).show()
                    return
                }
                val expiry = etCardExpiry.text.toString().trim()
                if (expiry.length < 5) {
                    Toast.makeText(requireContext(), "Enter valid expiry date (MM/YY)", Toast.LENGTH_SHORT).show()
                    return
                }
                val cvv = etCardCvv.text.toString().trim()
                if (cvv.length < 3) {
                    Toast.makeText(requireContext(), "Enter valid CVV", Toast.LENGTH_SHORT).show() // Fixed: LENGTH_SHOW -> LENGTH_SHORT
                    return
                }
            }
        }

        progressPayment.visibility = View.VISIBLE
        btnPay.isEnabled = false
        btnPay.text = "Processing..."

        lifecycleScope.launch {
            try {
                delay(2000) // Simulate payment processing

                val uid = FirebaseAuthManager.currentUserId
                if (uid.isEmpty()) { // Fixed: changed from isNullOrEmpty() to isEmpty()
                    Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
                    progressPayment.visibility = View.GONE
                    btnPay.isEnabled = true
                    btnPay.text = "Pay Now"
                    return@launch
                }

                val refNumber = "TXN-${System.currentTimeMillis().toString().takeLast(8)}"
                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

                // 1. Mark listing as reserved in Firestore
                FirebaseManager.reserveListing(listingId, uid)

                // 2. Save reservation document
                val reservation = Reservation(
                    userId = uid,
                    listingId = listingId,
                    listingTitle = listingTitle,
                    location = listingLocation,
                    depositPaid = depositAmount,
                    referenceNumber = refNumber,
                    paymentMethod = selectedPaymentMethod,
                    paymentDate = dateStr,
                    status = "confirmed"
                )
                FirebaseManager.saveReservation(reservation)

                progressPayment.visibility = View.GONE

                // 3. Show receipt
                showReceipt(refNumber, dateStr)

                // 4. Local notification
                NotificationHelper.sendReservationConfirmation(
                    requireContext(), listingTitle, refNumber
                )

            } catch (e: Exception) {
                progressPayment.visibility = View.GONE
                btnPay.isEnabled = true
                btnPay.text = "Pay Now"
                Toast.makeText(requireContext(), "Payment failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showReceipt(refNumber: String, dateStr: String) {
        layoutStep1.visibility = View.GONE
        layoutStep2.visibility = View.VISIBLE
        ibBack.visibility = View.GONE

        tvReceiptRef.text = refNumber
        tvReceiptListing.text = listingTitle
        tvReceiptLocation.text = listingLocation
        tvReceiptAmount.text = "BWP ${(depositAmount + 25).toInt()}"
        tvReceiptDate.text = dateStr
        tvReceiptMethod.text = selectedPaymentMethod
    }
}