package com.spendwise.app

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.spendwise.app.data.preferences.UserPreferences
import com.spendwise.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        userPrefs = UserPreferences(this)
        
        // Prevent Dark Mode flicker executing natively before lifecycle attachment
        AppCompatDelegate.setDefaultNightMode(
            if (userPrefs.isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupDrawerActions()
        setupBackPressDispatcher()
    }

    override fun onResume() {
        super.onResume()
        if (userPrefs.userEmail.isNotEmpty()) {
            updateNavHeader()
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        binding.toolbar.setNavigationOnClickListener {
            hideKeyboard()
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        // Top level destinations synchronizing UI hiding bounds 
        navController.addOnDestinationChangedListener { _, destination, _ ->
            hideKeyboard()
            val isAuth = destination.id == R.id.loginFragment ||
                    destination.id == R.id.registerFragment ||
                    destination.id == R.id.splashFragment ||
                    destination.id == R.id.onboardingFragment

            if (isAuth) {
                hideNavigation()
            } else {
                showNavigation()
                syncBottomNavWithDrawer(destination.id)
            }
        }
    }

    fun updateNavHeader() {
        val headerView = binding.navigationView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tvUserName)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        
        tvName?.text = userPrefs.userName.ifEmpty { "SpendWise User" }
        tvEmail?.text = userPrefs.userEmail.ifEmpty { "" }
    }

    private fun setupDrawerActions() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> navController.navigate(R.id.dashboardFragment)
                R.id.nav_history -> navController.navigate(R.id.expenseHistoryFragment)
                R.id.nav_goals -> navController.navigate(R.id.budgetGoalsFragment)
                R.id.nav_analytics -> navController.navigate(R.id.analyticsFragment)
                R.id.nav_profile -> navController.navigate(R.id.profileFragment)
                R.id.nav_settings -> navController.navigate(R.id.settingsFragment)
                R.id.nav_logout -> {
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.sign_out)
                        .setMessage(R.string.sign_out_confirm)
                        .setPositiveButton(R.string.sign_out) { _, _ ->
                            resetSession()
                            navController.navigate(
                                R.id.loginFragment,
                                null,
                                NavOptions.Builder().setPopUpTo(R.id.nav_graph, true).build()
                            )
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun syncBottomNavWithDrawer(destinationId: Int) {
        val menu = binding.navigationView.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId == destinationId) {
                item.isChecked = true
            }
        }
    }

    private fun setupBackPressDispatcher() {
        onBackPressedDispatcher.addCallback(this) {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * Ends the current user's session: clears the user's saved preferences (dark mode, notification
     * and onboarding choices are kept) AND every cached ViewModel
     * (expenses, budgets, last auth result) so the next user never sees the previous user's data
     * and the login screen can't replay the previous successful login.
     */
    fun resetSession() {
        userPrefs.clearSession()
        viewModelStore.clear()
    }

    fun hideNavigation() {
        binding.bottomNavigationView.visibility = android.view.View.GONE
        binding.toolbar.visibility = android.view.View.GONE
    }

    fun showNavigation() {
        binding.bottomNavigationView.visibility = android.view.View.VISIBLE
        binding.toolbar.visibility = android.view.View.VISIBLE
    }

    fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }
}
