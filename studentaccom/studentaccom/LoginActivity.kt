package com.example.studentaccom

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var tabLogin: TextView
    private lateinit var tabRegister: TextView
    private lateinit var loginLayout: LinearLayout
    private lateinit var registerLayout: LinearLayout
    private lateinit var etLoginEmail: EditText
    private lateinit var etLoginPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etStudentId: EditText
    private lateinit var etRegEmail: EditText
    private lateinit var etRegPassword: EditText
    private lateinit var spinnerUniversity: Spinner
    private lateinit var btnRegister: Button
    private lateinit var progressRegister: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (FirebaseAuthManager.isLoggedIn) {
            goToMain(); return
        }

        setContentView(R.layout.activity_login)
        bindViews()
        setupTabs()
        setupUniversitySpinner()
        btnLogin.setOnClickListener    { attemptLogin() }
        btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun bindViews() {
        tabLogin         = findViewById(R.id.tab_login)
        tabRegister      = findViewById(R.id.tab_register)
        loginLayout      = findViewById(R.id.layout_login)
        registerLayout   = findViewById(R.id.layout_register)
        etLoginEmail     = findViewById(R.id.et_login_email)
        etLoginPassword  = findViewById(R.id.et_login_password)
        btnLogin         = findViewById(R.id.btn_login)
        progressLogin    = findViewById(R.id.progress_login)
        etFirstName      = findViewById(R.id.et_first_name)
        etLastName       = findViewById(R.id.et_last_name)
        etStudentId      = findViewById(R.id.et_student_id)
        etRegEmail       = findViewById(R.id.et_reg_email)
        etRegPassword    = findViewById(R.id.et_reg_password)
        spinnerUniversity = findViewById(R.id.spinner_university)
        btnRegister      = findViewById(R.id.btn_register)
        progressRegister = findViewById(R.id.progress_register)
    }

    private fun setupTabs() {
        tabLogin.setOnClickListener {
            tabLogin.setBackgroundResource(R.drawable.bg_tab_active)
            tabLogin.setTextColor(getColor(R.color.primary_dark))
            tabRegister.setBackgroundResource(0)
            tabRegister.setTextColor(getColor(R.color.text_muted))
            loginLayout.visibility    = View.VISIBLE
            registerLayout.visibility = View.GONE
        }
        tabRegister.setOnClickListener {
            tabRegister.setBackgroundResource(R.drawable.bg_tab_active)
            tabRegister.setTextColor(getColor(R.color.primary_dark))
            tabLogin.setBackgroundResource(0)
            tabLogin.setTextColor(getColor(R.color.text_muted))
            loginLayout.visibility    = View.GONE
            registerLayout.visibility = View.VISIBLE
        }
        tabLogin.performClick()
    }

    private fun setupUniversitySpinner() {
        val unis = arrayOf(
            "University of Botswana (UB)",
            "Botswana University of Science & Technology",
            "BAISAGO University",
            "Botswana Open University",
            "Limkokwing University",
            "Botswana School of Business Sciences"
        )
        spinnerUniversity.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_item, unis).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
    }

    private fun attemptLogin() {
        val email    = etLoginEmail.text.toString().trim()
        val password = etLoginPassword.text.toString().trim()
        if (email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields"); return
        }
        progressLogin.visibility = View.VISIBLE
        btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = FirebaseAuthManager.login(email, password)
            progressLogin.visibility = View.GONE
            btnLogin.isEnabled = true
            result.onSuccess { user ->
                sessionManager.saveProfile(
                    user.firstName, user.lastName,
                    user.studentId, user.university, user.email
                )
                goToMain()
            }
            result.onFailure { toast("Login failed: ${it.message}") }
        }
    }

    private fun attemptRegister() {
        val firstName  = etFirstName.text.toString().trim()
        val lastName   = etLastName.text.toString().trim()
        val studentId  = etStudentId.text.toString().trim()
        val email      = etRegEmail.text.toString().trim()
        val password   = etRegPassword.text.toString().trim()
        val university = spinnerUniversity.selectedItem.toString()

        if (firstName.isEmpty() || lastName.isEmpty() ||
            studentId.isEmpty() || email.isEmpty() || password.isEmpty()) {
            toast("Please fill in all fields"); return
        }
        if (password.length < 6) { toast("Password must be at least 6 characters"); return }

        progressRegister.visibility = View.VISIBLE
        btnRegister.isEnabled = false

        lifecycleScope.launch {
            val result = FirebaseAuthManager.register(
                email, password, firstName, lastName, studentId, university
            )
            progressRegister.visibility = View.GONE
            btnRegister.isEnabled = true
            
            result.onSuccess {
                // Firebase automatically signs in the user after registration.
                // We sign them out so they have to log in manually as requested.
                FirebaseAuthManager.logout()
                
                toast("Registration successful! Please login with your details.")
                
                // Clear registration fields
                etRegEmail.setText("")
                etRegPassword.setText("")
                etFirstName.setText("")
                etLastName.setText("")
                etStudentId.setText("")
                
                // Switch to Login tab
                tabLogin.performClick()
            }
            result.onFailure { toast("Registration failed: ${it.message}") }
        }
    }

    private fun goToMain() {
        lifecycleScope.launch { FirebaseManager.seedListingsIfEmpty() }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
