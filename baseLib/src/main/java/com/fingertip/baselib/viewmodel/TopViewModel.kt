package com.fingertip.baselib.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


open class TopViewModel : ViewModel(), LifecycleObserver {

    var className: String = javaClass.simpleName


    val waiting = MutableLiveData<Event<Boolean>>()


    protected fun goLoading() {
        waiting.value = Event(true)
    }

    protected fun stopLoading() {
        waiting.value = Event(false)
    }


    fun<T> successResult(data: T?) = RequestResult(true, data)
    fun failResult(code: Int = -1) = RequestResult(false, null, code)

    fun launchUI(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            block()
        }
    }
}