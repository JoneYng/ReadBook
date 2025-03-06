package com.zx.read


import com.zx.read.config.ReadBookConfig
import com.zx.read.extensions.GSON
import com.zx.read.extensions.fromJsonArray
import java.io.File

object DefaultData {
    val defaultReadConfigs by lazy {
        val json = String(
            App.INSTANCE.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ReadBookConfig.Config>(json)!!
    }

}