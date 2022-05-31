package com.example.breathingmeditationandroid

import android.graphics.drawable.Drawable
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.annotation.UiThread
import com.plattysoft.leonids.ParticleSystem

class BirdsEmerging(private val activity: ComponentActivity) {

    fun animationStart() {
        val birdsParticleSystem = mutableSetOf<ParticleSystem>()

        for (i in 0..1) {
            birdsParticleSystem.add(setBirdParticleSystem(R.drawable.bird1))
            birdsParticleSystem.add(setBirdParticleSystem(R.drawable.bird2))
        }
        activity.runOnUiThread {
            birdsParticleSystem.elementAt(0).emit(100, 500,8)
            birdsParticleSystem.elementAt(1).emit(100, 500,8)
        }


    }


    private fun setBirdParticleSystem(bird: Int) : ParticleSystem {
        return ParticleSystem(activity, 30, bird, 700)
            .setScaleRange(0.4f, 0.5f)
            .setSpeedModuleAndAngleRange(0.07f, 0.16f, 200, 300)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }
}