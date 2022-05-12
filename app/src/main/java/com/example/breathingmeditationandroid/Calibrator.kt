package com.example.breathingmeditationandroid
import android.util.Log
import java.lang.System.currentTimeMillis
import kotlin.math.abs

object Calibrator {

    var calibratedAbdo: Pair<Double, Double> = Pair(0.0, 0.0)
    var calibratedThor: Pair<Double, Double> = Pair(0.0, 0.0)

    // Buffers for how much difference there can be for holding breath
    var holdBreathBufferInAbdo = 0.0
    var holdBreathBufferInThor = 0.0

    //TODO check if this can be calculated from in and out buffer
    var holdBreathBufferMiddleAbdo = 0.0
    var holdBreathBufferMiddleThor = 0.0

    var holdBreathBufferOutAbdo = 0.0
    var holdBreathBufferOutThor = 0.0


    private lateinit var breathingUtils: BreathingUtils
    private lateinit var mService: BluetoothConnection

    fun calibrate(mService: BluetoothConnection) {
        this.mService = mService
        breathingUtils = BreathingUtils(Calibrator.mService)
        calibrateMinAndMax()
        var calibratedInNum = 0
        var calibratedOutNum = 0
        var prevIn = false
        var prevOut = false
        val bufferAbdoOut = arrayListOf<Double>()
        val bufferAbdoIn = arrayListOf<Double>()
        val bufferThorOut = arrayListOf<Double>()
        val bufferThorIn = arrayListOf<Double>()

        repeat(2) {
            while (!(calibratedInNum == 2 && calibratedOutNum == 2)) {
                //lowest abdo
                Log.i("Calibration", "BreathHold")
                if (detectMaxRespiration() && calibratedOutNum < 2 && !prevOut) {
                    Log.i("Calibration", "Respiration: Hold breath for 5 sec")
                    val calibratedBreathHold = calibrateBreathHold()
                    bufferAbdoOut.add(calibratedBreathHold.first)
                    bufferThorOut.add(calibratedBreathHold.second)
                    calibratedOutNum++
                    prevOut = true
                    prevIn = false
                } else if (detectMaxInspiration() && calibratedInNum < 2 && !prevIn) {
                    Log.i("Calibration", "Inspiration: Hold breath for 5 sec")
                    val calibratedBreathHold = calibrateBreathHold()
                    bufferAbdoIn.add(calibratedBreathHold.first)
                    bufferThorIn.add(calibratedBreathHold.second)
                    calibratedInNum++
                    prevOut = false
                    prevIn = true
                }
            }
        }
        Log.i("Calibration:", "abdoOut: $holdBreathBufferOutAbdo, abdoIn: $holdBreathBufferInAbdo")
        Log.i("Calibration:", "thorOut: $holdBreathBufferOutThor, thorIn: $holdBreathBufferInThor")

    }

    private fun detectMaxInspiration(): Boolean {
        return breathingUtils.smoothValue().first >= calibratedAbdo.first.times(0.95)
                && breathingUtils.smoothValue().second >= calibratedThor.first.times(0.95)
    }

    private fun detectMaxRespiration(): Boolean {
        return breathingUtils.smoothValue().first <= calibratedAbdo.second.times(0.95)
                && breathingUtils.smoothValue().second <= calibratedThor.second.times(0.95)

    }

    //TODO: muss doch smarter gehen
    //TODO: als Coroutine dann kann sich der screen schön bewegen dazwischen

    private fun calibrateMinAndMax() {

        val minimaAbdo: ArrayList<Double> = ArrayList()
        val maximaAbdo: ArrayList<Double> = ArrayList()
        val minimaThor: ArrayList<Double> = ArrayList()
        val maximaThor: ArrayList<Double> = ArrayList()
        //TODO: lokal mit k?!?!
        var lokalMinima = 0.0
        var lokalMaxima = 0.0

        Log.i("Calibration:", "Abdo")
        //first abdo
        repeat(2) {
            while (mService.mExpiration == 0) {
                Log.i("Calibration", "inAbdoEx")
                if (!lokalMinima.equals(0.0)) {
                    minimaAbdo.add(lokalMinima)
                    lokalMinima = 0.0
                }
                if (mService.mAbdoCorrected > lokalMinima) {
                    lokalMaxima = mService.mAbdoCorrected
                }
            }

            while (mService.mInspiration == 0) {
                Log.i("Calibration", "inAbdoIn")
                if (!lokalMaxima.equals(0.0)) {
                    maximaAbdo.add(lokalMaxima)
                    lokalMaxima = 0.0
                }
                if (mService.mAbdoCorrected < lokalMinima) {
                    lokalMinima = mService.mAbdoCorrected
                }
            }
        }
        Log.i("Calibration:", "thor")
        //then thor
        repeat(2) {
            while (mService.mExpiration == 0) {
                Log.i("Calibration", "inThorIn")
                if (!lokalMinima.equals(0.0)) {
                    minimaThor.add(lokalMinima)
                    lokalMinima = 0.0
                }
                if (mService.mThorCorrected > lokalMaxima) {
                    lokalMaxima = mService.mThorCorrected

                }
            }

            while (mService.mInspiration == 0) {
                Log.i("Calibration", "inThorOut")
                if (!lokalMaxima.equals(0.0)) {
                    maximaThor.add(lokalMaxima)
                    lokalMaxima = 0.0
                }
                if (mService.mThorCorrected < lokalMinima) {
                    lokalMinima = mService.mThorCorrected
                }
            }
        }

        calibratedAbdo = Pair(mService.calculateMedian(maximaAbdo) * 1.2, mService.calculateMedian(minimaAbdo) * 1.2)
        calibratedThor = Pair(mService.calculateMedian(maximaThor) * 1.2, mService.calculateMedian(minimaThor) * 1.2)
    }

    private fun calibrateBreathHold(): Pair<Double, Double> {
        val diffValuesThor = arrayListOf<Double>()
        val diffValuesAbdo = arrayListOf<Double>()
        val startTime = currentTimeMillis()
        var prevValueAbdo = breathingUtils.smoothValue().first
        var prevValueThor = breathingUtils.smoothValue().second
        while (currentTimeMillis().minus(startTime) < 5000) {
                diffValuesAbdo.add(abs(breathingUtils.smoothValue().first.minus(prevValueAbdo)))
                diffValuesThor.add(abs(breathingUtils.smoothValue().second.minus(prevValueThor)))
                prevValueAbdo = breathingUtils.smoothValue().first
                prevValueThor = breathingUtils.smoothValue().second
                Log.i("Calibration", "${abs(breathingUtils.smoothValue().first.minus(prevValueAbdo))}")
                Log.i("Calibration", "${abs(breathingUtils.smoothValue().second.minus(prevValueThor))}")
        }
        return Pair(diffValuesAbdo.average(), diffValuesThor.average())
    }
}