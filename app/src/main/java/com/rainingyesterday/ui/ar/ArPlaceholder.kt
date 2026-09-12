package com.rainingyesterday.ui.ar

/**
 * AR 视图占位（GDD 7.31）。
 *
 * MVP（M0–M9）阶段只保留本包，不引入任何 AR SDK。
 * P2 后期以「AR 观察窗」单点验证起步，ARCore 与 CameraX/IMU 双轨验证（03 §4.4）。
 * AR 仅是表现层可选视图：输入来自触发引擎算出的 DerivedState（01 §4.3），
 * 领域层不依赖本包。
 */