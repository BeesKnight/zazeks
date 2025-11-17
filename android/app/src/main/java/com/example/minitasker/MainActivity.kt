package com.example.minitasker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.minitasker.ui.navigation.Screen
import com.example.minitasker.ui.screen.auth.AuthScreen
import com.example.minitasker.ui.screen.auth.AuthViewModel
import com.example.minitasker.ui.screen.projects.ProjectsScreen
import com.example.minitasker.ui.screen.projects.ProjectsViewModel
import com.example.minitasker.ui.screen.stats.StatsScreen
import com.example.minitasker.ui.screen.stats.StatsViewModel
import com.example.minitasker.ui.screen.taskdetail.TaskDetailScreen
import com.example.minitasker.ui.screen.taskdetail.TaskDetailViewModel
import com.example.minitasker.ui.screen.tasks.TasksScreen
import com.example.minitasker.ui.screen.tasks.TasksViewModel
import com.example.minitasker.ui.theme.MiniTaskerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MiniTaskerApplication
        setContent {
            MiniTaskerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MiniTaskerNavHost(app)
                }
            }
        }
    }
}

@Composable
fun MiniTaskerNavHost(app: MiniTaskerApplication) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = SimpleFactory { AuthViewModel(app.authRepository) })
    val projectsViewModel: ProjectsViewModel = viewModel(factory = SimpleFactory { ProjectsViewModel(app.projectRepository, app.authRepository) })
    val tasksViewModel: TasksViewModel = viewModel(factory = SimpleFactory { TasksViewModel(app.taskRepository) })
    val taskDetailViewModel: TaskDetailViewModel = viewModel(factory = SimpleFactory { TaskDetailViewModel(app.taskRepository) })
    val statsViewModel: StatsViewModel = viewModel(factory = SimpleFactory { StatsViewModel(app.statsRepository) })

    LaunchedEffect(Unit) {
        tasksViewModel.onTasksChanged = { projectId -> statsViewModel.load(projectId) }
        taskDetailViewModel.onTaskChanged = { projectId -> statsViewModel.load(projectId) }
    }

    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(viewModel = authViewModel) {
                navController.navigate(Screen.Projects.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            }
        }
        composable(Screen.Projects.route) {
            ProjectsScreen(
                viewModel = projectsViewModel,
                onProjectSelected = {
                    navController.navigate(Screen.Tasks.create(it.id, it.name))
                },
                onShowStats = {
                    navController.navigate(Screen.Stats.create(it.id, it.name))
                },
                onLoggedOut = {
                    authViewModel.reset()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Projects.route) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Screen.Tasks.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("projectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val projectName = backStackEntry.arguments?.getString("projectName") ?: ""
            TasksScreen(
                projectId = projectId,
                projectName = projectName,
                viewModel = tasksViewModel,
                onTaskSelected = { taskId ->
                    navController.navigate(Screen.TaskDetail.create(taskId))
                }
            )
        }
        composable(
            Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            TaskDetailScreen(taskId = taskId, viewModel = taskDetailViewModel)
        }
        composable(
            Screen.Stats.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("projectName") { type = NavType.StringType }
            )
        ) { entry ->
            val projectId = entry.arguments?.getLong("projectId") ?: 0L
            val projectName = entry.arguments?.getString("projectName") ?: ""
            StatsScreen(projectId = projectId, projectName = projectName, viewModel = statsViewModel)
        }
    }
}

class SimpleFactory<T : androidx.lifecycle.ViewModel>(private val creator: () -> T) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return creator.invoke() as T
    }
}
