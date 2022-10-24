package com.fingertip.baselib.top

import android.app.Application
import com.blankj.utilcode.util.Utils

open class TopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        Utils.init(instance)
    }

    companion object {
        @JvmStatic
        lateinit var instance: Application
    }
}