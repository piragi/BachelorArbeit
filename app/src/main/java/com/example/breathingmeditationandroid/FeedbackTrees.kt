package com.example.breathingmeditationandroid

import android.graphics.drawable.AnimationDrawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.activity.ComponentActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.concurrent.thread


class FeedbackTrees(
    private val treesLeft: ImageView?,
    private val treesRight: ImageView?,
    private val treesNeutral: ImageView?,
    private val activity: ComponentActivity,
    val mService: BluetoothConnection
) : InspirationExpirationInterface {

    init {
        activity.runOnUiThread {
            treesRight?.visibility  = View.INVISIBLE
            treesLeft?.visibility  = View.INVISIBLE
            treesRight?.setBackgroundResource(R.drawable.trees_right)
            treesLeft?.setBackgroundResource(R.drawable.trees_left)
        }
        mService.addListener(this)
    }

    override fun onExpiration() {
        Log.i("inspiration", "happened")
        activity.runOnUiThread {
            val treesLeftAnimation =
                treesLeft?.background as AnimationDrawable?
            val treesRightAnimation =
                treesRight?.background as AnimationDrawable?

            treesRight?.visibility = View.VISIBLE
            treesLeft?.visibility = View.INVISIBLE
            treesNeutral?.visibility = View.INVISIBLE

            treesLeftAnimation?.stop()
            treesRightAnimation?.start()

        }
    }

    override fun onInspiration() {
        activity.runOnUiThread {
            val treesLeftAnimation =
                treesLeft?.background as AnimationDrawable?
            val treesRightAnimation =
                treesRight?.background as AnimationDrawable?

            treesRight?.visibility = View.INVISIBLE
            treesLeft?.visibility = View.VISIBLE
            treesNeutral?.visibility = View.INVISIBLE

            treesRightAnimation?.stop()
            treesLeftAnimation?.start()
        }
    }
}
