package com.example.breathingmeditationandroid.levels

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.R

class RocketTakeOff (private val height: Int, private val activity: ComponentActivity) {

    fun startAnimation() {
        val rocket = activity.findViewById<View>(R.id.rocket) as ImageView
        activity.runOnUiThread {
            rocket.visibility = View.VISIBLE

            ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                interpolator = LinearInterpolator()
                duration = 1500L
                start()
                addUpdateListener {
                    val progressY = animatedValue as Float * (-height)
                    val progressX = animatedValue as Float * -90
                    rocket.translationY = progressY
                    rocket.translationX = progressX
                }
            }
        }
    }
}