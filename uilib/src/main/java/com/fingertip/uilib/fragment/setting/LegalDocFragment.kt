package com.fingertip.uilib.fragment.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentLegalDocBinding
import com.fingertip.uilib.viewmodel.MainVM

/**
 * 法律文本页：隐私政策 / 用户协议 / 开源许可
 *
 * 内容打包在 assets/legal/ 里，**不走服务端下发**。
 * 理由：这三份文档是提审必查项，审核员点进来必须能读到完整条款；
 * 走网络一旦没配置 URL 或网络不通，就会变成"页面暂未开放"直接判违规。
 * 条款要更新时随版本发包，反正改条款本身也需要用户重新同意。
 */
class LegalDocFragment : TopPmFragment<MainVM>() {

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_ASSET = "arg_asset"

        /** assets/legal 下的文件名，不带路径 */
        const val ASSET_PRIVACY = "privacy.html"
        const val ASSET_TERMS = "terms.html"
        const val ASSET_LICENSE = "licenses.html"

        fun newInstance(title: String, asset: String): LegalDocFragment {
            return LegalDocFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_ASSET, asset)
                }
            }
        }
    }

    override fun layoutId() = R.layout.fragment_legal_doc

    override fun initVM(): MainVM = provideVM()

    private val binding get() = mBinding as FragmentLegalDocBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun initShiTu() {
        binding.tvTitle.text = arguments?.getString(ARG_TITLE).orEmpty()

        //条款是纯静态文本，不需要 JS；关掉能少一大块攻击面
        binding.webView.settings.apply {
            javaScriptEnabled = false
            domStorageEnabled = false
            //按屏宽自适应，避免手机上左右滚动
            useWideViewPort = true
            loadWithOverviewMode = true
        }

        val asset = arguments?.getString(ARG_ASSET) ?: ASSET_PRIVACY
        binding.webView.loadUrl("file:///android_asset/legal/$asset")
    }

    override fun getClickViews(): List<View> = listOf(binding.ivBack)

    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when (v) {
            binding.ivBack -> pop()
        }
    }

    override fun onDestroyView() {
        //WebView 不销毁会漏 Activity 引用
        binding.webView.destroy()
        super.onDestroyView()
    }
}
