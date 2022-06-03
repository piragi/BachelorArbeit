package com.example.breathingmeditationandroid

import android.graphics.drawable.AnimationDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import kotlin.concurrent.thread


class FeedbackTrees(
    private val treesLeft: ImageView,
    private val treesRight: ImageView,
    private val treesNeutral: ImageView,
    private val activity: ComponentActivity,
    val mService: BluetoothConnection
) : InspirationExpirationInterface {

    init {
        activity.runOnUiThread {
            treesRight.setBackgroundResource(R.drawable.trees_right)
            treesLeft.setBackgroundResource(R.drawable.trees_left)
        }
        mService.addListener(this)
        checkForAnimations()

    }

    private fun checkForAnimations() {
        thread(start = true, isDaemon = true) {
            val treesLeftAnimation =
                treesLeft.background as AnimationDrawable
            while(true) {
                if(!treesLeftAnimation.isRunning) {
                    treesNeutral.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onExpiration() {
        Log.i("inspiration", "happened")
        activity.runOnUiThread {
            treesRight.visibility = View.VISIBLE
            treesLeft.visibility = View.INVISIBLE
            treesNeutral.visibility = View.INVISIBLE

            val treesLeftAnimation =
                treesLeft.background as AnimationDrawable
            treesLeftAnimation.stop()

            val treesRightAnimation =
                treesRight.background as AnimationDrawable
            treesRightAnimation.start()

        }
    }

    override fun onInspiration() {
        activity.runOnUiThread {
            treesRight.visibility = View.INVISIBLE
            treesLeft.visibility = View.VISIBLE
            treesNeutral.visibility = View.INVISIBLE


            val treesRightAnimation =
                treesRight.background as AnimationDrawable
            treesRightAnimation.stop()
            val treesLeftAnimation =
                treesLeft.background as AnimationDrawable
            treesLeftAnimation.start()

        }
    }
}
