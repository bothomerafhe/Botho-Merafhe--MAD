package com.example.studentaccom

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.studentaccom.R
import com.example.studentaccom.FirebaseAuthManager
import com.example.studentaccom.FirebaseManager
import com.example.studentaccom.NotificationHelper
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!FirebaseAuthManager.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        NotificationHelper.createNotificationChannel(this)

        // Ensure database is seeded and migrated with photos
        lifecycleScope.launch {
            FirebaseManager.seedListingsIfEmpty()
        }

        bottomNav = findViewById(R.id.bottom_nav)

        if (savedInstanceState == null) loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home    -> loadFragment(HomeFragment())
                R.id.nav_map     -> loadFragment(MapFragment())
                R.id.nav_alerts  -> loadFragment(AlertsFragment())
                R.id.nav_chat    -> loadFragment(ChatFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
