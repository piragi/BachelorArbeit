package com.example.breathingmeditationandroid.levels

import android.graphics.drawable.AnimationDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import com.example.breathingmeditationandroid.BluetoothConnection
import com.example.breathingmeditationandroid.InspirationExpirationInterface
import com.example.breathingmeditationandroid.R


class FeedbackTrees(
    private val activity: ComponentActivity,
    val mService: BluetoothConnection
) : InspirationExpirationInterface {

    private val treesLeft: ImageView =
        activity.findViewById<View>(R.id.trees_left_background) as ImageView
    private val treesRight: ImageView =
        activity.findViewById<View>(R.id.trees_right_background) as ImageView
    private val treesNeutral: ImageView =
        activity.findViewById<View>(R.id.trees_neutral) as ImageView

    init {
        activity.runOnUiThread {
            treesRight.visibility = View.INVISIBLE
            treesLeft.visibility = View.INVISIBLE
            treesRight.setBackgroundResource(R.drawable.trees_right)
            treesLeft.setBackgroundResource(R.drawable.trees_left)
        }
        mService.addListener(this)
    }

    override fun onExpiration() {
        Log.i("inspiration", "happened")
        activity.runOnUiThread {
            val treesLeftAnimation =
                treesLeft.background as AnimationDrawable?
            val treesRightAnimation =
                treesRight.background as AnimationDrawable?

            treesRight.visibility = View.VISIBLE
            treesLeft.visibility = View.INVISIBLE
            treesNeutral.visibility = View.INVISIBLE

            treesLeftAnimation?.stop()
            treesRightAnimation?.start()

        }
    }

    override fun onInspiration() {
        activity.runOnUiThread {
            val treesLeftAnimation =
                treesLeft.background as AnimationDrawable?
            val treesRightAnimation =
                treesRight.background as AnimationDrawable?

            treesRight.visibility = View.INVISIBLE
            treesLeft.visibility = View.VISIBLE
            treesNeutral.visibility = View.INVISIBLE

            treesRightAnimation?.stop()
            treesLeftAnimation?.start()
        }
    }
}
