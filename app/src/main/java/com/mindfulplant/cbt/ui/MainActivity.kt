package com.mindfulplant.cbt.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mindfulplant.cbt.R
import com.mindfulplant.cbt.databinding.ActivityMainBinding
import com.mindfulplant.cbt.notifications.NotificationHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Android 13+ requires a runtime grant before notifications can be shown.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* reminder simply stays silent if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the fixed bottom navigation above Android's gesture/navigation
        // area on Android 15+, instead of letting edge-to-edge consume its
        // icon and label space.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.statusBarColor = getColor(R.color.bg_screen)
        window.navigationBarColor = getColor(R.color.bg_screen)
        window.decorView.systemUiVisibility = 0
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationHelper.hasPermission(this)) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Hide the bottom nav bar on the auth screens (Login/Register),
        // matching the wireframes - it should only appear once logged in.
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility =
                if (destination.id == R.id.loginFragment ||
                    destination.id == R.id.registerFragment ||
                    destination.id == R.id.newThoughtRecordFragment
                ) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
        }
    }
}
