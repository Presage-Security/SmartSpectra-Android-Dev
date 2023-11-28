package com.presagetech.smartspectra.common

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

/**
 * This class is a view that use for masking, that means if you want to introduce on view you just
 * use MaskView function it will create mask all over the parent view except that special view you
 * pass to it.
 * @see maskView
 * */
class WalkThroughLayout(context: Context, attr: AttributeSet) : View(context, attr) {

    private var mCanvas: Canvas? = null
    private var mBitmap: Bitmap? = null
    private var maskedView: Rect? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#CC000000")
    }
    private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }


    override fun draw(canvas: Canvas) {
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mCanvas = Canvas(mBitmap!!)
        mCanvas?.drawRect(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            backgroundPaint
        )

        maskedView?.let {
            mCanvas?.drawRect(
                it.left.toFloat(),
                it.top.toFloat(),
                it.right.toFloat(),
                it.bottom.toFloat(),
                holePaint
            )
        }
        canvas.drawBitmap(mBitmap!!, 0f, 0f, null)
        super.draw(canvas)
    }

    fun maskView(view: View){
        maskedView = Rect(
            view.left,
            view.top,
            view.right,
            view.bottom
        )
        invalidate()
    }
    fun maskRect(rect: Rect){
        maskedView = rect
        invalidate()
    }
}
