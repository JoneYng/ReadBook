package com.zx.read.utils

import android.content.Context
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.AnimRes
import com.zx.read.config.AppConfig

object AnimationUtilsSupport {
    fun loadAnimation(context: Context, @AnimRes id: Int): Animation {
        val animation = AnimationUtils.loadAnimation(context, id)
        return animation
    }
}