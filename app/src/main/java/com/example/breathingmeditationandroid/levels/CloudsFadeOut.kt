package com.example.breathingmeditationandroid.levels

import android.media.Image
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.core.graphics.alpha
import com.example.breathingmeditationandroid.R

class CloudsFadeOut(private val activity: ComponentActivity) {
    private lateinit var clouds: ImageView
    fun animationStart() {
        clouds = activity.findViewById(R.id.background_clouds)
        val cloudFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fadeout)
        cloudFadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                clouds.alpha = 0.0f
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

        })
        activity.findViewById<View>(R.id.background_clouds).startAnimation(cloudFadeOut)
    }

    fun resetView() {
        activity.runOnUiThread {
            clouds.animate()
                .setDuration(10000)
                .alpha(1.0f)
                .setListener(null)
            clouds.alpha = 1.0f
        }
    }
}
