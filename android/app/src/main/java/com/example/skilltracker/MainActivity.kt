package com.example.skilltracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // УБИРАЕМ настройку ActionBar, т.к. его нет
        // setupActionBarWithNavController(navController)
    }

    // И заодно можно убрать/закомментировать override onSupportNavigateUp, если он есть,
    // или оставить – он просто не будет использовать ActionBar.
}

