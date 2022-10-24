package com.lzlz.toplib.top

import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fingertip.baselib.top.TopFragment
import com.fingertip.baselib.viewmodel.TopViewModel

abstract class TopVMFragment<VM : TopViewModel> : TopFragment() {

    lateinit var mViewModel: VM

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        mViewModel = initVM()
        mViewModel.run {
            lifecycle.addObserver(this)
        }
        initObserver()
        super.onActivityCreated(savedInstanceState)
    }

    abstract fun initVM(): VM

    open fun initObserver() {
        mViewModel.run {
            waiting.observe(viewLifecycleOwner, Observer {
                it?.run {
                    if (getContentIfNotHandled() != null && peekContent()) {
                        startWaiting()
                    } else {
                        endWaiting()
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