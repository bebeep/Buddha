package com.fingertip.uilib.fragment.game

import android.annotation.SuppressLint
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentFangshengBinding
import com.fingertip.uilib.viewmodel.FangShengVM

class FangShengFragment : TopPmFragment<FangShengVM>() {

    companion object {
        private const val TAG = "FangShengFragment"
        private const val GAME_URL = "file:///android_asset/game/index.html"
    }

    private val binding get() = mBinding as FragmentFangshengBinding

    override fun initVM(): FangShengVM = provideVM()

    override fun layoutId() = R.layout.fragment_fangsheng

    @SuppressLint("SetJavaScriptEnabled")
    override fun initShiTu() {
        val webView = binding.webView
        setupWebView(webView)
        webView.loadUrl(GAME_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(webView: WebView) {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        // 注入 JSBridge
        webView.addJavascriptInterface(GameBridge(), "AndroidBridge")
        // 禁止长按菜单
        webView.isLongClickable = false
        webView.isHapticFeedbackEnabled = false
    }

    /** JSBridge - 接收 H5 游戏回调 */
    inner class GameBridge {
        @JavascriptInterface
        fun onGameWin(gameType: String, score: Int) {
            log(TAG, "放生成功: $gameType, 分数: $score")
            // TODO: 接入功德系统
            activity?.runOnUiThread {
                // 可在此弹出放生成功动画或提示
            }
        }

        @JavascriptInterface
        fun onGameLose(gameType: String) {
            log(TAG, "游戏失败: $gameType")
        }

        @JavascriptInterface
        fun closeGame() {
            log(TAG, "关闭游戏")
            activity?.runOnUiThread {
                _mActivity?.onBackPressed()
            }
        }

        @JavascriptInterface
        fun onScoreChanged(score: Int) {
            // 可扩展：同步分数到原生UI
        }
    }

    override fun onDestroyView() {
        binding.webView.apply {
            loadUrl("about:blank")
            stopLoading()
            removeAllViews()
            destroy()
        }
        super.onDestroyView()
    }
}
