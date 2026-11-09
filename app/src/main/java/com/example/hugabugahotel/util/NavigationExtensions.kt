package com.example.hugabugahotel.util

import android.app.Activity
import android.content.Intent
import com.example.hugabugahotel.R
import com.example.hugabugahotel.ui.account.MyBookingsActivity
import com.example.hugabugahotel.ui.account.ProfileActivity
import com.example.hugabugahotel.ui.caverns.CavernsListActivity
import com.example.hugabugahotel.ui.experiences.ExperiencesActivity
import com.example.hugabugahotel.ui.home.HomeActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Configura a barra de navegação inferior padrão para reutilizar Activities existentes
 * na pilha de tarefas (via FLAG_ACTIVITY_REORDER_TO_FRONT), evitando loop infinito de Activities.
 */
fun BottomNavigationView.setupMainNavigation(currentActivity: Activity, currentItemId: Int) {
    selectedItemId = currentItemId
    setOnItemSelectedListener { item ->
        if (item.itemId == currentItemId) return@setOnItemSelectedListener true

        val targetClass = when (item.itemId) {
            R.id.nav_home -> HomeActivity::class.java
            R.id.nav_caverns -> CavernsListActivity::class.java
            R.id.nav_experiences -> ExperiencesActivity::class.java
            R.id.nav_reservations -> MyBookingsActivity::class.java
            R.id.nav_profile -> ProfileActivity::class.java
            else -> null
        }

        if (targetClass != null) {
            val intent = Intent(currentActivity, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            currentActivity.startActivity(intent)
            true
        } else {
            false
        }
    }
}
