package com.fingertip.uilib.fragment

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.PicUtils
import com.fingertip.uilib.R
import com.fingertip.uilib.viewmodel.MeFragmentVM
import kotlinx.android.synthetic.main.fragment_me.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe

class MeFragment : TopPmFragment<MeFragmentVM>(), PicUtils.UpPicCallBack {
    override fun layoutId(): Int = R.layout.fragment_me

    override fun initShiTu() {
        picUtils.upPicCallBack = this


    }

    override fun onSupportVisible() {
        super.onSupportVisible()
        refreshData()

    }

    override fun getClickViews(): List<View> = mutableListOf(iv_setting, iv_edit,iv_head)

    override fun onSingleClick(v: View?) {
        when(v){
            iv_setting->{

            }

            iv_edit -> {
//                startActRootFragment(EditFragment())
            }

            iv_head -> {
                picChoseDialog.show()
            }
        }
    }

    private fun refreshData() {
    }



    override fun initVM(): MeFragmentVM = provideVM()

    override fun initObserver() {
        super.initObserver()

        mViewModel.setAvatarResult.observe(this) {

        }
    }

    override fun callback(isSuccess: Boolean, outfile: String?, t: Throwable?) {
        super.callback(isSuccess, outfile, t)
        outfile?.let {
            picUtils.uploadFile(it)
        }
    }

    override fun start() {
        lifecycleScope.launch(Dispatchers.Main) {
            startWaiting()
        }
    }

    override fun isOktoUp(avatarPath: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            loadEnding()
            log(fName, "上传头像")
            mViewModel.upAvatar(avatarPath)
        }
    }

    override fun errorend() {
        lifecycleScope.launch(Dispatchers.Main) {
            loadEnding()
        }
    }

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when(event.what) {

        }
    }
}