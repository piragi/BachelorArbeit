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
        val snowParticleSystem2 = setSnowParticleSystem()
        val snowParticleSystem3 = setSnowParticleSystem()
        val snowParticleSystem4 = setSnowParticleSystem()
        val snowParticleSystem5 = setSnowParticleSystem()
        val snowParticleSystem = setSnowParticleSystem()

        val fadeOut = AnimationUtils.loadAnimation(activity, R.anim.fadeout)

        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                snowParticleSystem.emit(50, 200, 7)
                snowParticleSystem2.emit(600, 200, 7)
                snowParticleSystem3.emit(1200, 200, 7)
                snowParticleSystem4.emit(1500, 380, 7)
                snowParticleSystem5.emit(1750, 150, 7)
            }

            override fun onAnimationEnd(animation: Animation) {
                snowParticleSystem2.stopEmitting()
                snowParticleSystem3.stopEmitting()
                snowParticleSystem4.stopEmitting()
                snowParticleSystem5.stopEmitting()
                snowParticleSystem.stopEmitting()
                snow.visibility = View.INVISIBLE
                fadeOut.cancel()
            }

            override fun onAnimationRepeat(p0: Animation?) {}
        })

        snow.startAnimation(fadeOut)
    }

    private fun setSnowParticleSystem(): ParticleSystem {
        return ParticleSystem(activity, 30, R.drawable.snowflake, 200)
            .setScaleRange(0.1f, 0.2f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setRotationSpeedRange(90f, 180f)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }
}