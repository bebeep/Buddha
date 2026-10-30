package com.fingertip.uilib.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopActivity
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.uilib.databinding.ActivityCommonWebBinding
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

/**
 * 通用 H5 展示页：
 * - 任何 H5 页面传 title + url 进来即可展示（修行攻略、活动页、协议等）
 * - 自动把登录 session 拼到 url 上（?session=xxx / &session=xxx），H5 侧按需读取
 * - url 从 globalParam.guideUrl 这类服务端下发的字段取，可热更新不发版
 *
 * 关于主文档拦截（重要）：
 * 阿里云 OSS 默认域名对 2019-09-24 之后创建的桶，返回 html 时会强制加上
 * `Content-Disposition: attachment` + `x-oss-force-download: true`，浏览器会当成附件下载，
 * WebView 里表现为「白屏、无任何回调、无报错」。这里由 App 自己取主文档，去掉该头后再交给
 * WebView 渲染。绑定自定义域名（已备案）后，这段拦截也可以留着，无副作用。
 */
class CommonWebViewActivity : TopActivity() {

    companion object {
        private const val EXTRA_URL = "extra_url"
        private const val EXTRA_TITLE = "extra_title"

        /**
         * 打开 H5 页面，自动附加登录 session。
         * url 为空（服务端未配置）时静默提示，不崩溃
         */
        fun start(context: Context, title: String, url: String?) {
            if (url.isNullOrBlank()) {
                ToastUtil.showMessage("页面暂未开放")
                return
            }
            context.startActivity(
                Intent(context, CommonWebViewActivity::class.java)
                    .putExtra(EXTRA_TITLE, title)
                    .putExtra(EXTRA_URL, url)
            )
        }

        /**
         * 通用 session 拼接：已带 session 参数不重复拼，带 ? 用 &，否则用 ?
         */
        fun appendSession(url: String): String = when {
            url.contains("session=") -> url
            url.contains("?") -> "$url&session=${GlobalConfig.session}"
            else -> "$url?session=${GlobalConfig.session}"
        }
    }

    private lateinit var binding: ActivityCommonWebBinding

    //主文档抓取专用，shouldInterceptRequest 在子线程回调，可以直接阻塞
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override fun isFullTopBar() = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun initShiTu() {
        binding = ActivityCommonWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //TopActivity 里的 FitsSystemWindows 在 setContentView 之前调用，那时还没有子 View，
        //避让会落空；布局根节点已声明 fitsSystemWindows，这里再补一次，确保标题栏不压状态栏
        com.fingertip.baselib.util.FullTopUtil.FitsSystemWindows(this, true)

        binding.tvTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "详情"

        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        }
        binding.webView.setBackgroundColor(Color.WHITE)

        binding.webView.webViewClient = object : WebViewClient() {

            /**
             * 主文档自己抓，绕开 OSS 默认域名的强制下载头；非 html 或抓取失败返回 null，
             * 交回 WebView 原生加载
             */
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!request.isForMainFrame) return null
                val url = request.url.toString()
                if (!url.startsWith("http://") && !url.startsWith("https://")) return null
                return fetchAsHtml(url)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.pbLoading.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    log("CommonWebView", "load fail: ${request.url} ${error?.errorCode} ${error?.description}")
                    binding.pbLoading.visibility = View.GONE
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val scheme = request.url.scheme.orEmpty()
                //http(s) 交给 WebView 自己加载；其余（电话、唤起 App 等）不在本页处理
                return scheme != "http" && scheme != "https"
            }
        }
        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                binding.pbLoading.progress = newProgress
                binding.pbLoading.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }
        }

        binding.pbLoading.visibility = View.VISIBLE
        binding.webView.loadUrl(appendSession(intent.getStringExtra(EXTRA_URL).orEmpty()))
    }

    /**
     * 取主文档：返回 html 才包装成 WebResourceResponse（不带 Content-Disposition），
     * 其余情况返回 null 让 WebView 走原生流程
     */
    private fun fetchAsHtml(url: String): WebResourceResponse? = try {
        val response = httpClient.newCall(Request.Builder().url(url).get().build()).execute()
        response.use { rsp ->
            val contentType = rsp.header("Content-Type").orEmpty()
            val isHtml = contentType.contains("text/html", true) || contentType.contains("application/xhtml", true)
            if (!rsp.isSuccessful || !isHtml) {
                null
            } else {
                val bytes = rsp.body?.bytes()
                if (bytes == null || bytes.isEmpty()) {
                    null
                } else {
                    //去掉 Content-Disposition / x-oss-force-download，避免被当成下载
                    val headers = mutableMapOf<String, String>()
                    rsp.headers.forEach { (name, value) ->
                        val lower = name.lowercase()
                        if (lower != "content-disposition" && lower != "x-oss-force-download") {
                            headers[name] = value
                        }
                    }
                    headers["Content-Type"] = contentType.ifBlank { "text/html; charset=utf-8" }
                    WebResourceResponse(
                        "text/html",
                        "utf-8",
                        rsp.code,
                        rsp.message.ifBlank { "OK" },
                        headers,
                        ByteArrayInputStream(bytes)
                    )
                }
            }
        }
    } catch (e: Throwable) {
        log("CommonWebView", "fetchAsHtml failed: ${e.message}")
        null
    }

    override fun listOfClickView(): List<View> = listOf(binding.ivBack)

    override fun respondOnceClick(v: View?) {
        when (v) {
            binding.ivBack -> {
                if (binding.webView.canGoBack()) binding.webView.goBack() else finish()
            }
        }
    }

    override fun onBackPressedSupport() {
        //H5 内部多级页面时先退 H5 页面，再退 Activity
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressedSupport()
        }
    }

    override fun onDestroy() {
        binding.webView.apply {
            loadUrl("about:blank")
            clearHistory()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }
}
