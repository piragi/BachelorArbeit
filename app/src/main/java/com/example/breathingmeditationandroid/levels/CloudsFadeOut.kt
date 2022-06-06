package com.example.breathingmeditationandroid.levels

import android.media.Image
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.R

class CloudsFadeOut(private val activity: ComponentActivity) {
    fun animationStart() {
        val clouds = activity.findViewById<View>(R.id.background_clouds)
        val cloudFadeOut = AnimationUtils.loadAnimation(activity, R.anim.fadeout)
        cloudFadeOut.setAnimationListener(object: Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                clouds.visibility = View.INVISIBLE
            }

            override fun onAnimationRepeat(animation: Animation?) {
            }

        })
        activity.findViewById<View>(R.id.background_clouds).startAnimation(cloudFadeOut)
    }
}