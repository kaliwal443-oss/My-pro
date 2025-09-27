package com.example.indiangridnavigation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.*

class CompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bearing: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f
    
    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    
    private val textPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
    }
    
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 2f
    }
    
    private val directions = arrayOf("N", "E", "S", "W")
    private val secondaryDirections = arrayOf("NE", "SE", "SW", "NW")

    fun updateOrientation(bearing: Float, pitch: Float = 0f, roll: Float = 0f) {
        this.bearing = bearing
        this.pitch = pitch
        this.roll = roll
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val centerY = height / 2f
        val radius = min(centerX, centerY) * 0.8f
        
        drawCompassBackground(canvas, centerX, centerY, radius)
        drawDirectionMarkers(canvas, centerX, centerY, radius)
        drawNorthIndicator(canvas, centerX, centerY, radius)
        drawBearingText(canvas, centerX, centerY)
        drawOrientationIndicators(canvas, centerX, centerY, radius)
    }
    
    private fun drawCompassBackground(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // Draw outer circle
        paint.color = Color.argb(200, 40, 40, 40)
        canvas.drawCircle(centerX, centerY, radius, paint)
        
        // Draw inner circle
        paint.color = Color.argb(150, 60, 60, 60)
        canvas.drawCircle(centerX, centerY, radius * 0.8f, paint)
        
        // Draw border
        strokePaint.color = Color.argb(200, 100, 100, 100)
        strokePaint.strokeWidth = 3f
        canvas.drawCircle(centerX, centerY, radius, strokePaint)
    }
    
    private fun drawDirectionMarkers(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 1f
        textPaint.color = Color.WHITE
        
        for (i in 0 until 36) {
            val angle = i * 10f - bearing
            val isMainDirection = i % 9 == 0
            val isSecondaryDirection = i % 9 == 4
            
            val startRadius = when {
                isMainDirection -> radius - 40
                isSecondaryDirection -> radius - 30
                else -> radius - 20
            }
            val endRadius = radius - 10
            
            val startX = centerX + startRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val startY = centerY - startRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val endX = centerX + endRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            val endY = centerY - endRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            
            // Draw tick mark
            strokePaint.strokeWidth = if (isMainDirection) 3f else 1f
            canvas.drawLine(startX, startY, endX, endY, strokePaint)
            
            // Draw direction letters for main directions
            if (isMainDirection) {
                val directionIndex = (i / 9) % 4
                val textRadius = radius - 60
                val textX = centerX + textRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
                val textY = centerY - textRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
                
                textPaint.color = if (directions[directionIndex] == "N") Color.RED else Color.WHITE
                textPaint.textSize = 24f
                textPaint.typeface = Typeface.DEFAULT_BOLD
                canvas.drawText(directions[directionIndex], textX, textY + 8, textPaint)
            }
            
            // Draw secondary directions
            if (isSecondaryDirection) {
                val directionIndex = (i / 9) % 4
                val textRadius = radius - 50
                val textX = centerX + textRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
                val textY = centerY - textRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
                
                textPaint.color = Color.LTGRAY
                textPaint.textSize = 12f
                textPaint.typeface = Typeface.DEFAULT
                canvas.drawText(secondaryDirections[directionIndex], textX, textY + 4, textPaint)
            }
        }
    }
    
    private fun drawNorthIndicator(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        paint.color = Color.RED
        
        // Draw north indicator triangle
        val trianglePath = Path()
        val tipX = centerX
        val tipY = centerY - radius + 25
        val baseY = centerY - radius + 60
        
        trianglePath.moveTo(tipX, tipY)
        trianglePath.lineTo(tipX - 15, baseY)
        trianglePath.lineTo(tipX + 15, baseY)
        trianglePath.close()
        
        canvas.drawPath(trianglePath, paint)
        
        // Draw north indicator line
        strokePaint.color = Color.RED
        strokePaint.strokeWidth = 2f
        canvas.drawLine(centerX, centerY - radius + 60, centerX, centerY - radius + 80, strokePaint)
    }
    
    private fun drawBearingText(canvas: Canvas, centerX: Float, centerY: Float) {
        textPaint.color = Color.WHITE
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("${bearing.toInt()}°", centerX, centerY + 10, textPaint)
        
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText("Pitch: ${pitch.toInt()}°", centerX - 50, centerY + 40, textPaint)
        canvas.drawText("Roll: ${roll.toInt()}°", centerX + 50, centerY + 40, textPaint)
    }
    
    private fun drawOrientationIndicators(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // Pitch indicator (horizontal line)
        strokePaint.color = Color.GREEN
        strokePaint.strokeWidth = 2f
        val pitchOffset = pitch.coerceIn(-30f, 30f) * 2
        canvas.drawLine(centerX - 40, centerY + pitchOffset, centerX + 40, centerY + pitchOffset, strokePaint)
        
        // Roll indicator (inclined line)
        strokePaint.color = Color.BLUE
        val rollRad = Math.toRadians(roll.toDouble())
        val rollLength = 50f
        val rollX = rollLength * sin(rollRad).toFloat()
        val rollY = rollLength * cos(rollRad).toFloat()
        canvas.drawLine(centerX, centerY, centerX + rollX, centerY - rollY, strokePaint)
        
        // Center dot
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, 5f, paint)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val size = min(width, height)
        setMeasuredDimension(size, size)
    }
}
