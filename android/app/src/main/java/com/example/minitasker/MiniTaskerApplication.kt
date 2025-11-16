package com.example.minitasker

import android.app.Application
import com.example.minitasker.data.repository.ServiceLocator

class MiniTaskerApplication : Application() {
    val authRepository by lazy { ServiceLocator.provideAuthRepository(this) }
    val projectRepository by lazy { ServiceLocator.provideProjectRepository(this) }
    val taskRepository by lazy { ServiceLocator.provideTaskRepository(this) }
    val statsRepository by lazy { ServiceLocator.provideStatsRepository(this) }
}
