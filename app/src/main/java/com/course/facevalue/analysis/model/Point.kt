package com.course.facevalue.analysis.model

import kotlin.math.hypot

/** 归一化图像坐标系中的二维点（0..1，原点在左上角）。 */
data class Pt(val x: Float, val y: Float) {
    fun distanceTo(other: Pt): Float = hypot(x - other.x, y - other.y)
    fun mid(other: Pt): Pt = Pt((x + other.x) / 2f, (y + other.y) / 2f)
    operator fun plus(other: Pt): Pt = Pt(x + other.x, y + other.y)
    operator fun minus(other: Pt): Pt = Pt(x - other.x, y - other.y)
    operator fun times(k: Float): Pt = Pt(x * k, y * k)
}

/** 归一化矩形。 */
data class Box(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val area: Float get() = width * height
}

fun List<Pt>.centroid(): Pt {
    if (isEmpty()) return Pt(0f, 0f)
    var sx = 0f
    var sy = 0f
    for (p in this) { sx += p.x; sy += p.y }
    return Pt(sx / size, sy / size)
}

fun List<Pt>.bounds(): Box {
    if (isEmpty()) return Box(0f, 0f, 0f, 0f)
    var l = Float.MAX_VALUE; var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE; var b = -Float.MAX_VALUE
    for (p in this) {
        if (p.x < l) l = p.x
        if (p.x > r) r = p.x
        if (p.y < t) t = p.y
        if (p.y > b) b = p.y
    }
    return Box(l, t, r, b)
}

/** 以 center 为圆心旋转（角度制，逆时针为正，y 轴向下）。 */
fun Pt.rotated(center: Pt, degrees: Float): Pt {
    val rad = Math.toRadians(degrees.toDouble())
    val cos = kotlin.math.cos(rad).toFloat()
    val sin = kotlin.math.sin(rad).toFloat()
    val dx = x - center.x
    val dy = y - center.y
    return Pt(center.x + dx * cos - dy * sin, center.y + dx * sin + dy * cos)
}

fun List<Pt>.rotated(center: Pt, degrees: Float): List<Pt> = map { it.rotated(center, degrees) }

fun angleDegrees(a: Pt, vertex: Pt, b: Pt): Float {
    val v1x = a.x - vertex.x; val v1y = a.y - vertex.y
    val v2x = b.x - vertex.x; val v2y = b.y - vertex.y
    val n1 = hypot(v1x.toDouble(), v1y.toDouble())
    val n2 = hypot(v2x.toDouble(), v2y.toDouble())
    if (n1 < 1e-6 || n2 < 1e-6) return 0f
    val cos = ((v1x * v2x + v1y * v2y) / (n1 * n2)).coerceIn(-1.0, 1.0)
    return Math.toDegrees(kotlin.math.acos(cos)).toFloat()
}
