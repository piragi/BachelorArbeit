package com.example.breathingmeditationandroid.gestures

import kotlinx.coroutines.Deferred

interface IBreathingGesture {
    //TODO add functions like resume, pause and detected to interface
    fun detect(): Deferred<Boolean>
    fun stopDetection()
    fun resumeDetection()
}