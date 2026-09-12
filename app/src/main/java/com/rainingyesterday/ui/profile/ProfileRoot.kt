package com.rainingyesterday.ui.profile

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.rainingyesterday.ui.collection.CollectionScreen
import com.rainingyesterday.ui.diary.DiaryScreen

/** 日记子页。 */
@Composable
fun ProfileDiary() {
    DiaryScreen()
}

/** 图鉴子页。 */
@Composable
fun ProfileCollection() {
    CollectionScreen()
}

/** 设置子页。 */
@Composable
fun ProfileSettings(navController: NavHostController) {
    SettingsScreen(onBack = { navController.navigateUp() })
}