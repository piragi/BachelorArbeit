package com.example.breathingmeditationandroid

import android.graphics.drawable.AnimationDrawable
import android.media.Image
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity

class FeedbackTrees(private val backgroundScreen: ImageView, private val activity: ComponentActivity, val mService: BluetoothConnection) : InspirationExpirationInterface {

    private lateinit var treesRightAnimation: AnimationDrawable

    init {
        activity.runOnUiThread {
            backgroundScreen.setBackgroundResource(R.drawable.trees_right)

        }


        mService.addListener(this)

    }
    override fun onInspiration() {
        Log.i("inspiration", "happened")
        activity.runOnUiThread() {
            val treesRightAnimation =
                backgroundScreen.background as AnimationDrawable
            treesRightAnimation.start()
        }

    }

    override fun onExpiration() {
        activity.runOnUiThread() {
            val treesRightAnimation =
                backgroundScreen.background as AnimationDrawable
            treesRightAnimation.stop()

        }
    }
}
