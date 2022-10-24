package com.fingertip.baselib.bean

import com.blankj.utilcode.util.GsonUtils
import java.io.Serializable

open class TopData : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }


    override fun toString(): String {
        return GsonUtils.toJson(this)
    }
}