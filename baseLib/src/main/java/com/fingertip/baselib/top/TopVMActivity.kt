package com.fingertip.baselib.top

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fingertip.baselib.viewmodel.TopViewModel

abstract class TopVMActivity<VM : TopViewModel> : TopActivity() {

    lateinit var mViewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        mViewModel = initVM()
        mViewModel.run {
            lifecycle.addObserver(this)
        }
        initObserver()

        super.onCreate(savedInstanceState)
    }

    abstract fun initVM(): VM

    open fun initObserver() {
        mViewModel.run {
            waiting.observe(this@TopVMActivity, Observer {
                it?.run {
                    if (getContentIfNotHandled() != null && peekContent()) {
                        loading()
                    } else {
                        loadEnding()
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(mViewModel)
        super.onDestroy()
    }

    inline fun <reified T: TopViewModel> provideVM(): T {
        return ViewModelProvider(this).get(T::class.java)
    }
}