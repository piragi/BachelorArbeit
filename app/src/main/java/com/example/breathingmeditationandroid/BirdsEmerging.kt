package com.example.breathingmeditationandroid

import android.graphics.drawable.Drawable
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.annotation.UiThread
import com.plattysoft.leonids.ParticleSystem

class BirdsEmerging(private val activity: ComponentActivity) {

    fun animationStart() {
        val coordinatesTree = arrayOf(Pair(110, 495), Pair(400, 505), Pair(700, 560), Pair(950, 490), Pair(1250, 490), Pair(1500, 440), Pair(1650, 490), Pair(1950, 475))
        val birdsParticleSystem = mutableSetOf<ParticleSystem>()

        for (i in 0..7) {
            birdsParticleSystem.add(setBirdParticleSystem(R.drawable.bird1, 200, 300))
            birdsParticleSystem.add(setBirdParticleSystem(R.drawable.bird2reversed, 190, 240))
            birdsParticleSystem.add(setBirdParticleSystem(R.drawable.bird2, 305, 330))
        }
        activity.runOnUiThread {
            //tree one
            birdsParticleSystem.elementAt(0).emit(coordinatesTree[0].first+10, coordinatesTree[0].second+10,8, 200)
            birdsParticleSystem.elementAt(1).emit(coordinatesTree[0].first, coordinatesTree[0].second,8, 200)
            birdsParticleSystem.elementAt(2).emit(coordinatesTree[0].first+5, coordinatesTree[0].second+5,8, 200)

            birdsParticleSystem.elementAt(3).emit(coordinatesTree[1].first+10, coordinatesTree[1].second+10,8, 200)
            birdsParticleSystem.elementAt(4).emit(coordinatesTree[1].first, coordinatesTree[1].second,8, 200)
            birdsParticleSystem.elementAt(5).emit(coordinatesTree[1].first+5, coordinatesTree[1].second+5,8, 200)

            birdsParticleSystem.elementAt(6).emit(coordinatesTree[2].first+10, coordinatesTree[2].second+10,8, 200)
            birdsParticleSystem.elementAt(7).emit(coordinatesTree[2].first, coordinatesTree[2].second,8, 200)
            birdsParticleSystem.elementAt(8).emit(coordinatesTree[2].first+5, coordinatesTree[2].second+5,8, 200)

            birdsParticleSystem.elementAt(9).emit(coordinatesTree[3].first+10, coordinatesTree[3].second+10,8, 200)
            birdsParticleSystem.elementAt(10).emit(coordinatesTree[3].first, coordinatesTree[3].second,8, 200)
            birdsParticleSystem.elementAt(11).emit(coordinatesTree[3].first+5, coordinatesTree[3].second+5,8, 200)

            birdsParticleSystem.elementAt(12).emit(coordinatesTree[4].first+10, coordinatesTree[4].second+10,8, 200)
            birdsParticleSystem.elementAt(13).emit(coordinatesTree[4].first, coordinatesTree[4].second,8, 200)
            birdsParticleSystem.elementAt(14).emit(coordinatesTree[4].first+5, coordinatesTree[4].second+5,8, 200)

            birdsParticleSystem.elementAt(15).emit(coordinatesTree[5].first+10, coordinatesTree[5].second+10,8, 200)
            birdsParticleSystem.elementAt(16).emit(coordinatesTree[5].first, coordinatesTree[5].second,8, 200)
            birdsParticleSystem.elementAt(17).emit(coordinatesTree[5].first+5, coordinatesTree[5].second+5,8, 200)

            birdsParticleSystem.elementAt(18).emit(coordinatesTree[6].first+10, coordinatesTree[6].second+10,8, 200)
            birdsParticleSystem.elementAt(19).emit(coordinatesTree[6].first, coordinatesTree[6].second,8, 200)
            birdsParticleSystem.elementAt(20).emit(coordinatesTree[6].first+5, coordinatesTree[6].second+5,8, 200)

            birdsParticleSystem.elementAt(21).emit(coordinatesTree[7].first+10, coordinatesTree[7].second+10,8, 200)
            birdsParticleSystem.elementAt(22).emit(coordinatesTree[7].first, coordinatesTree[7].second,8, 200)
            birdsParticleSystem.elementAt(23).emit(coordinatesTree[7].first+5, coordinatesTree[7].second+5,8, 200)

        }
    }


    private fun setBirdParticleSystem(bird: Int, minAngle: Int, maxAngle: Int) : ParticleSystem {
        return ParticleSystem(activity, 30, bird, 700)
            .setScaleRange(0.5f, 0.6f)
            .setSpeedModuleAndAngleRange(0.15f, 0.16f, minAngle, maxAngle)
            .setAcceleration(0.00013f, 90)
            .setFadeOut(150, AccelerateInterpolator())
    }
}