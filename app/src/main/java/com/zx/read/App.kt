package com.zx.read

import android.app.Application


/**
 * @description:
 * @author: zhouxiang
 * @created: 2025/03/05 11:52
 * @version: V1.0
 */
class App : Application()  {
    companion object {
        @JvmStatic
        lateinit var INSTANCE: com.zx.read.App
            private set
    }
    override fun onCreate() {
        super.onCreate()
        com.zx.read.App.Companion.INSTANCE = this
    }
}