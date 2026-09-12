package com.rainingyesterday

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** 应用入口：Hilt 依赖装配根节点（docs/01 §3 di/）。 */
@HiltAndroidApp
class RainingYesterdayApp : Application()