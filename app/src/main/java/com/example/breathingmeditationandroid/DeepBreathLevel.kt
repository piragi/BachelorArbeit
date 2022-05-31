package com.example.breathingmeditationandroid

import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.plattysoft.leonids.ParticleSystem

class DeepBreathLevel(private val snow: ImageView, private val activity: ComponentActivity) {

    fun animationStart() {
        val snowParticleSystemSet = mutableSetOf<ParticleSystem>()

        for (i in 0..4) {
            snowParticleSystemSet.add(setSnowParticleSystem())
        }

        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fadeout)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                snowParticleSystemSet.elementAt(0).emit(50, 200, 7)
                snowParticleSystemSet.elementAt(1).emit(600, 200, 7)
                snowParticleSystemSet.elementAt(2).emit(1200, 200, 7)
                snowParticleSystemSet.elementAt(3).emit(1500, 380, 7)
                snowParticleSystemSet.elementAt(4).emit(1750, 150, 7)
            }

            override fun onAnimationEnd(animation: Animation) {
                for (i in 0..4) {
                    snowParticleSystemSet.elementAt(i).stopEmitting()
                }
                snow.visibility = View.INVISIBLE
                fadeOut.cancel()
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        snow.startAnimation(fadeOut)
    }

    private fun setSnowParticleSystem(): ParticleSystem {
        return ParticleSystem(activity, 30, R.drawable.snowflake, 200)
            .setScaleRange(0.3f, 0.4f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }
}