package com.fingertip.uilib.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.loge
import com.blankj.utilcode.util.GsonUtils
import com.fingertip.baselib.net.NetManager
import com.fingertip.baselib.net.RequestBodyFactory
import com.fingertip.baselib.viewmodel.RequestResult
import com.fingertip.baselib.viewmodel.TopVMImp
import com.fingertip.uilib.service.ModuleDownloadService
import com.fingertip.uilib.utils.ModelDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 拜佛页面 ViewModel
 *
 * 负责：
 * - 优先使用 ModuleDownloadService 预下载的3D模型
 * - 本地无3D模型时尝试在线下载
 * - 3D模型加载/下载失败时回退到普通模型(jpg/png)图片
 * - 通过 LiveData 通知 Fragment 加载状态
 */
class WorshipVM : TopVMImp() {

    companion object {
        private const val TAG = "WorshipVM"
        const val MODEL_ID = "worship_bee"

        /**
         * 解析佛像供奉价目 priceField，如 [{"days":2,"price":98},{"days":8,"price":298}]
         */
        fun parsePriceField(priceField: String?): List<PriceItem> {
            if (priceField.isNullOrBlank()) return emptyList()
            return try {
                GsonUtils.fromJson(priceField, GsonUtils.getListType(PriceItem::class.java)) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    /** 下载进度 0.0 ~ 1.0 */
    val downloadProgress = MutableLiveData<Float>()

    /** 3D模型准备就绪（本地文件路径） */
    val modelReady = MutableLiveData<String?>()

    /** 需要显示普通模型图片(jpg/png)的URL */
    val fallbackImageUrl = MutableLiveData<String>()

    /** 加载失败（错误信息） */
    val loadError = MutableLiveData<String>()

    /** 加载状态：true=加载中 */
    val loading = MutableLiveData<Boolean>()

    val buddhaConfigResult = MutableLiveData<RequestResult<List<BuddhaConfig>>>()

    /**
     * 获取或下载模型
     *
     * 优先级：
     * 1. 本地已缓存的3D模型（由 ModuleDownloadService 预下载）
     * 2. 在线下载3D模型（buddha3DModuleUrl）
     * 3. 回退到普通模型图片（buddhaModuleUrl）
     */
    fun getOrDownloadModel(context: Context) {
        launchUI {
            withContext(Dispatchers.Main) {
                loading.value = true
            }

            val buddhaConfig = GlobalConfig.globalParam?.buddhaConfig

            // 1. 优先检查本地已缓存的3D模型（ModuleDownloadService 预下载的）
            val local3DPath = ModuleDownloadService.getLocal3DModelPath(context)
            if (!local3DPath.isNullOrEmpty()) {
                log(TAG, "使用预下载的3D模型: $local3DPath")
                modelReady.postValue(local3DPath)
                loading.postValue(false)
                return@launchUI
            }

            // 2. 本地无缓存，尝试在线下载3D模型
            val threeDUrl = buddhaConfig?.get(0)?.buddha3DModuleUrl ?: ""
            if (threeDUrl.isNotEmpty()) {
                log(TAG, "本地无3D模型缓存，尝试在线下载: $threeDUrl")
                val downloaded = withContext(Dispatchers.IO) {
                    try {
                        var result: String? = null
                        val latch = java.util.concurrent.CountDownLatch(1)
                        ModelDownloadManager.getOrDownloadModel(
                            context = context,
                            modelUrl = threeDUrl,
                            modelId = MODEL_ID,
                            callback = object : ModelDownloadManager.ModelDownloadCallback {
                                override fun onProgress(progress: Float) {
                                    downloadProgress.postValue(progress)
                                }

                                override fun onSuccess(localFilePath: String) {
                                    result = localFilePath
                                    latch.countDown()
                                }

                                override fun onError(errorMsg: String) {
                                    loge(TAG, "在线下载3D模型失败: $errorMsg")
                                    latch.countDown()
                                }
                            }
                        )
                        latch.await()
                        result
                    } catch (e: Exception) {
                        loge(TAG, "在线下载3D模型异常: ${e.message}")
                        null
                    }
                }
                if (!downloaded.isNullOrEmpty()) {
                    log(TAG, "在线下载3D模型成功: $downloaded")
                    modelReady.postValue(downloaded)
                    loading.postValue(false)
                    return@launchUI
                }
            }

            // 3. 3D模型加载/下载均失败，回退到普通模型图片
            val moduleUrl = buddhaConfig?.get(0)?.buddhaModuleUrl ?: ""
            if (moduleUrl.isNotEmpty()) {
                log(TAG, "3D模型不可用，回退到普通模型图片: $moduleUrl")
                fallbackImageUrl.postValue(moduleUrl)
                loading.postValue(false)
                return@launchUI
            }

            // 4. 都没有，报错
            loading.postValue(false)
            loadError.postValue("无可用的模型资源")
        }
    }


    fun getBuddhaConfig() {
        call({
            NetManager.getApi().getBuddhaConfig()
        }, {
            buddhaConfigResult.value = successResult(it)
        }, {
            buddhaConfigResult.value = failResult(it.errorCode)
        }, showLoading = false, toastError = false)
    }

    /** 供佛结果：成功后返回新的供奉剩余秒数（服务端不回，由页面重拉列表） */
    val worshipResult = MutableLiveData<RequestResult<String>>()

    /**
     * 供佛：按价目选择供奉天数，服务端扣功德币；已在供则顺延
     */
    fun worshipBuddha(buddhaId: Int, days: Int) {
        call({
            NetManager.getApi().worshipBuddha(
                body = RequestBodyFactory.worshipBody(buddhaId, days)
            )
        }, {
            worshipResult.value = successResult(it)
        }, {
            worshipResult.value = failResult(it.errorCode)
        })
    }

    /** 供奉价目项（对应服务端 PriceFieldConfig） */
    data class PriceItem(val days: Int = 0, val price: Int = 0)
}
