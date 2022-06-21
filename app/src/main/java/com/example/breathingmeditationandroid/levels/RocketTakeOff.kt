package com.example.breathingmeditationandroid.levels

import android.animation.ValueAnimator
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.R

class RocketTakeOff(private val height: Int, private val activity: ComponentActivity) {

    fun startAnimation() {
        val rocketWithFire = activity.findViewById<View>(R.id.rocket) as ImageView
        val rocketWithoutFire = activity.findViewById<View>(R.id.rocket_without_fire) as ImageView
        activity.runOnUiThread {
            rocketWithoutFire.visibility = View.INVISIBLE
            rocketWithFire.visibility = View.VISIBLE

            ValueAnimator.ofFloat(0.0f, 1.2f).apply {
                interpolator = LinearInterpolator()
                duration = 2200L
                start()
                addUpdateListener {
                    val progressY = animatedValue as Float * (-height)
                    val progressX = animatedValue as Float * -190
                    rocketWithFire.translationY = progressY
                    rocketWithFire.translationX = progressX
                }
            }
            ValueAnimator.ofFloat(0.0f, 1.2f).apply {
                interpolator = LinearInterpolator()
                duration = 1000L
                start()
                addUpdateListener {
                    val progressY = animatedValue as Float * (height)
                    val progressX = animatedValue as Float * 190
                    rocketWithFire.translationY = progressY
                    rocketWithFire.translationX = progressX
                }
            }
        }
    }
}