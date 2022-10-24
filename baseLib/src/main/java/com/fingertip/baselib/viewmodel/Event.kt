package com.fingertip.baselib.viewmodel

/**
*用作通过LiveData表示事件的数据公开的包装。
*/
data class Event<out T>(private var content: T) {

    private var hasBeenHandled = false

    // 返回内容并阻止其再次使用。
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    // 返回内容，即使已经处理过。
    fun peekContent(): T = content
}