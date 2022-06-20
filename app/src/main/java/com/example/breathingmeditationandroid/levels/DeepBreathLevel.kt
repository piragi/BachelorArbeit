package com.example.breathingmeditationandroid.levels

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.R
import com.plattysoft.leonids.ParticleSystem

class DeepBreathLevel(private val snow: ImageView, private val activity: ComponentActivity) {

    fun animationStart() {
        val positions =
            arrayOf(Pair(50, 180), Pair(600, 265), Pair(1285, 245), Pair(1745, 395), Pair(1970, 170))
        val snowParticleSystemSet = mutableSetOf<ParticleSystem>()

        for (i in positions.indices) {
            snowParticleSystemSet.add(setSnowParticleSystem())
        }

        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fadeout)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {

                for (i in positions.indices) {
                    //750 good emittingTime
                    snowParticleSystemSet.elementAt(i).emit(positions.elementAt(i).first, positions.elementAt(i).second, 11)
                }
            }

            override fun onAnimationEnd(animation: Animation) {
                snow.visibility = View.INVISIBLE
                fadeOut.cancel()
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        snow.startAnimation(fadeOut)
    }

    private fun setSnowParticleSystem(): ParticleSystem {
        return ParticleSystem(activity, 30, R.drawable.snowflake, 300)
            .setScaleRange(0.2f, 0.3f)
            .setSpeedModuleAndAngleRange(0.007f, 0.016f, 190, 330)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.0025f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }
}