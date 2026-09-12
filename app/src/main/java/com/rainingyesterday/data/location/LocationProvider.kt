package com.rainingyesterday.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 玩家定位结果（领域层不依赖 Android Location，此处为 data 层对外出口）。 */
data class UserLocation(
    val lat: Double,
    val lng: Double,
    val accuracyM: Float? = null,
    val fromProvider: String = "unknown",
)

/**
 * 定位源：抽象"当前位置"，让领域层只认这个值，不碰 Android LocationManager（docs/01 数据层、03 §4.3）。
 * 三档定位策略由上层通过 [LocationMode] 决定启用哪种实现，见 MountedLocationProvider。
 */
interface LocationProvider {
    /** 定位是否可用（权限已授予） */
    fun isAvailable(): Boolean
    /** 当前的位置流 */
    val location: StateFlow<UserLocation?>
    /** 主动请求一次定位（Fused-style；响应写入 location） */
    fun requestSingle()
    /** 启动持续定位（根据实现是否需持续监听） */
    fun start()
    /** 停止持续定位/释放 */
    fun stop()
}

/**
 * 真实定位：用 Android LocationManager（免 GMS，华为/国内设备可跑），前台低耗。
 * 权限缺失时优雅降级（location 置空，由 UI 落到手点模拟模式，不崩）。
 */
@Singleton
class RealLocationProvider @Inject constructor(
    private val context: Context,
) : LocationProvider {

    private val _location = MutableStateFlow<UserLocation?>(null)
    override val location: StateFlow<UserLocation?> = _location.asStateFlow()

    private val manager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    override fun isAvailable(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    override fun requestSingle() {
        if (!isAvailable()) return
        // 优先 GPS，其次网络；取最后一个可用修复
        val best = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { try { manager.getLastKnownLocation(it) } catch (_: Exception) { null } }
            .maxByOrNull { it.time }
        best?.let { emit(it.latitude, it.longitude, it.accuracy, best.provider ?: "gps") }
    }

    @SuppressLint("MissingPermission")
    override fun start() {
        if (!isAvailable()) return
        val listener = LocationListener { loc ->
            emit(loc.latitude, loc.longitude, loc.accuracy, loc.provider ?: "net")
        }
        activeListener = listener
        try {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 15_000L, 5f, listener, Looper.getMainLooper(),
            )
        } catch (_: Exception) { /* GPS 不可用则忽略 */ }
        try {
            manager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 15_000L, 5f, listener, Looper.getMainLooper(),
            )
        } catch (_: Exception) { /* ignore */ }
    }

    private var activeListener: android.location.LocationListener? = null

    override fun stop() {
        activeListener?.let { try { manager.removeUpdates(it) } catch (_: Exception) {} }
        activeListener = null
    }

    private fun emit(lat: Double, lng: Double, accuracy: Float?, provider: String) {
        _location.value = UserLocation(lat, lng, accuracy, provider)
    }
}

/** 手点模拟定位：不依赖真实传感器，用于演示/开发/权限被拒兜底（docs/03 §4.3 disable 模式）。 */
@Singleton
class MockLocationProvider @Inject constructor() : LocationProvider {

    private val _location = MutableStateFlow<UserLocation?>(null)
    override val location: StateFlow<UserLocation?> = _location.asStateFlow()

    override fun isAvailable(): Boolean = true // 模拟无需权限

    /** 外部（地图点击）把目标经纬度写入模拟位置。 */
    fun setMock(lat: Double, lng: Double) {
        _location.value = UserLocation(lat, lng, fromProvider = "mock")
    }

    override fun requestSingle() = unit()
    override fun start() = unit()
    override fun stop() = unit()

    private fun unit(): Unit {
        // 模拟定位无需传感器动作，静默
    }
}