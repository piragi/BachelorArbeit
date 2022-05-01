package com.example.breathingmeditationandroid

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.service.controls.ControlsProviderService.TAG
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.bullfrog.particle.IParticleManager
import com.bullfrog.particle.Particles
import com.bullfrog.particle.animation.ParticleAnimation
import com.bullfrog.particle.particle.configuration.Rotation
import com.bullfrog.particle.particle.configuration.Shape
import kotlin.math.*
import kotlin.random.Random
import com.bullfrog.particle.path.IPathGenerator
import com.bullfrog.particle.path.LinearPathGenerator

class HomeScreenActivity : ComponentActivity() {

    private lateinit var container: ViewGroup

    private var particleManager: IParticleManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.home_screen)
        container = findViewById(R.id.home_screen)

        particleManager = Particles.with(this, container) // container is the parent ViewGroup for particles
        particleManager!!.color(0xFFF000)// color sampling from button
            .particleNum(200)// how many particles
            .anchor(850, 200)// use button as the anchor of the animation
            .shape(Shape.CIRCLE)// circle particle
            .radius(2, 6)// random circle radius from 2 to 6
            .anim(ParticleAnimation.EXPLOSION)// using explosion animation
            .start()

        // particleManager!!.start()
    }

    private fun createPathGenerator(): IPathGenerator {
        return object : LinearPathGenerator() {
            val cos = Random.nextDouble(-1.0, 1.0)
            val sin = Random.nextDouble(-1.0, 1.0)

            override fun getCurrentCoord(
                progress: Float,
                duration: Long,
                outCoord: IntArray
            ): Unit {
                val originalX = distance * progress
                val originalY = 100 * sin(originalX / 50)
                val x = originalX * cos - originalY * sin
                val y = originalX * sin + originalY * cos
                outCoord[0] = (0.01 * x * originalY).toInt()
                outCoord[1] = -(0.0001 * y.pow(2) * originalX).toInt()
            }
        }
    }

    private fun createAnimator(): ValueAnimator {
        val animator = ValueAnimator.ofInt(0, 1)
        animator.repeatCount = -1
        animator.repeatMode = ValueAnimator.REVERSE
        animator.duration = 4000L
        return animator
    }
}