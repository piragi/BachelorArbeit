package com.example.breathingmeditationandroid

import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import com.plattysoft.leonids.ParticleSystem
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class GamePausing(activity: ComponentActivity, breathingUtils: BreathingUtils) {

    private var activity: ComponentActivity
    private var breathingUtils: BreathingUtils
    private var gamePaused = true

    init {
        this.activity = activity
        this.breathingUtils = breathingUtils
    }

    fun startAnimation() {
        animateLeaves()
    }

    private fun animateLeaves() {

    }

    private fun initializeParticles(): ParticleSystem? {
        return null
    }
}