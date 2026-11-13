package io.github.taehunkim.dorangori.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.taehunkim.dorangori.AppContainer
import io.github.taehunkim.dorangori.DorangoriApp
import io.github.taehunkim.dorangori.ui.detail.RecordingDetailScreen
import io.github.taehunkim.dorangori.ui.list.RecordingListScreen
import io.github.taehunkim.dorangori.ui.settings.SettingsScreen

val Context.container: AppContainer get() = (applicationContext as DorangoriApp).container

@Composable
fun AppScaffold(openId: Long?, onOpened: () -> Unit) {
    val nav = rememberNavController()
    LaunchedEffect(openId) {
        if (openId != null) { nav.navigate("detail/$openId") { launchSingleTop = true }; onOpened() }
    }
    NavHost(nav, startDestination = "list") {
        composable("list") { RecordingListScreen(onOpen = { nav.navigate("detail/$it") }, onSettings = { nav.navigate("settings") }) }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            RecordingDetailScreen(id = entry.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
