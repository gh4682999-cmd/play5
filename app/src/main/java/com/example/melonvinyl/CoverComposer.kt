package com.example.melonvinyl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient

/**
 * 안드로이드 오토는 앱 화면을 직접 그릴 수 없고 '앨범 이미지 한 장'만 표시할 수 있습니다.
 * 그래서 앨범 자켓 + 뒤에서 나온 레코드판 + 앨범 색 그라데이션 배경을
 * 한 장의 정사각형 이미지로 합성해서 앨범 이미지 대신 넘깁니다. (정지 이미지, 회전 없음)
 */
object CoverComposer {

    fun compose(src: Bitmap, size: Int = 512): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val s = size.toFloat()
        val aa = Paint.ANTI_ALIAS_FLAG
        val side = minOf(src.width, src.height)
        val crop = Rect((src.width - side) / 2, (src.height - side) / 2, (src.width + side) / 2, (src.height + side) / 2)

        // 배경: 앨범 위쪽/아래쪽 평균색 그라데이션 (차량 화면용으로 어둡게)
        val (top, bottom) = edgeColors(src)
        c.drawRect(0f, 0f, s, s, Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, s, top, bottom, Shader.TileMode.CLAMP)
        })

        // 배치: 자켓은 왼쪽, 레코드는 자켓 오른쪽 가장자리에 중심을 두고 뒤에 놓음
        val sl = 0.62f * s
        val slLeft = 0.04f * s
        val slTop = (s - sl) / 2f
        val r = 0.29f * s
        val cx = slLeft + sl
        val cy = s / 2f

        // 레코드: 앨범 이미지로 꽉 채운 픽처 디스크
        val save = c.save()
        c.clipPath(Path().apply { addCircle(cx, cy, r, Path.Direction.CW) })
        c.drawBitmap(src, crop, RectF(cx - r, cy - r, cx + r, cy + r), Paint(Paint.FILTER_BITMAP_FLAG))
        val groove = Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = 0x2E000000 }
        var rr = r * 0.12f
        while (rr < r) { c.drawCircle(cx, cy, rr, groove); rr += 4f }
        c.drawCircle(cx, cy, r, Paint().apply {
            shader = SweepGradient(
                cx, cy,
                intArrayOf(0, 0x38FFFFFF, 0, 0, 0x2EFFFFFF, 0, 0),
                floatArrayOf(0f, 0.10f, 0.20f, 0.50f, 0.62f, 0.72f, 1f)
            )
        })
        c.restoreToCount(save)
        c.drawCircle(cx, cy, r - 1.5f, Paint(aa).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x99000000.toInt() })
        c.drawCircle(cx, cy, r * 0.065f, Paint(aa).apply { color = 0x99000000.toInt() })
        c.drawCircle(cx, cy, r * 0.045f, Paint(aa).apply { color = bottom })

        // 앨범 자켓 (그림자 포함)
        val slRect = RectF(slLeft, slTop, slLeft + sl, slTop + sl)
        c.drawRect(slRect, Paint(aa).apply {
            color = Color.BLACK
            setShadowLayer(0.03f * s, 0f, 0.012f * s, 0x66000000)
        })
        c.drawBitmap(src, crop, slRect, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** 이미지 위쪽 1/3, 아래쪽 1/3의 평균색을 어둡게 만든 값 */
    private fun edgeColors(src: Bitmap): Pair<Int, Int> {
        val b = Bitmap.createScaledBitmap(src, 16, 16, true)
        fun avg(y0: Int, y1: Int): Int {
            var r = 0; var g = 0; var bl = 0; var n = 0
            for (y in y0 until y1) for (x in 0 until 16) {
                val p = b.getPixel(x, y)
                r += Color.red(p); g += Color.green(p); bl += Color.blue(p); n++
            }
            val k = 0.5f
            return Color.rgb((r / n * k).toInt(), (g / n * k).toInt(), (bl / n * k).toInt())
        }
        return avg(0, 5) to avg(11, 16)
    }
}
