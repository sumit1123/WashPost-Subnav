package com.wapo.flagship.sliding

interface PullDownActivity {
    fun onSlidingStarted()
    fun onSlidingFinished()
    fun canSlideDown() : Boolean
}