package com.fingertip.uilib.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.blankj.utilcode.util.SPUtils
import com.fingertip.baselib.bean.BuddhaConfig
import com.fingertip.baselib.log
import com.fingertip.baselib.loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 佛像模型文件后台下载服务
 *
 * 下载 buddhaConfig 中的 buddhaModuleUrl(普通模型) 和 buddha3DModuleUrl(3D模型) 到本地，
 * 以「文件命名 + 时间戳」作为是否需要重新下载的依据：
 * 当本地文件名/时间戳与服务端一致且文件存在时跳过下载，否则重新下载。
 */
class ModuleDownloadService : Service() {

    companion object {
        private const val TAG = "ModuleDownloadService"

        //下载记录保存的SP名
        private const val SP_NAME = "SP_MODULE_DOWNLOAD"

        //本地保存目录(应用私有目录，无需存储权限)
        private const val DIR_MODULE = "buddha_module"

        //SP记录key前缀
        private const val KEY_MODULE = "module"
        private const val KEY_3D_MODULE = "module_3d"

        private const val EXTRA_MODULE_URL = "extra_module_url"
        private const val EXTRA_3D_URL = "extra_3d_url"
        private const val EXTRA_3D_NAME = "extra_3d_name"
        private const val EXTRA_3D_UPDATE_DATE = "extra_3d_update_date"

        /**
         * 启动下载服务
         */
        fun start(context: Context, config: BuddhaConfig) {
            val intent = Intent(context, ModuleDownloadService::class.java).apply {
                putExtra(EXTRA_MODULE_URL, config.buddhaModuleUrl ?: "")
                putExtra(EXTRA_3D_URL, config.buddha3DModuleUrl ?: "")
                putExtra(EXTRA_3D_NAME, config.buddha3DModuleName ?: "")
                putExtra(EXTRA_3D_UPDATE_DATE, config.buddha3DModuleUpdateDate ?: "")
            }
            context.startService(intent)
        }

        /**
         * 获取本地已缓存的3D模型文件路径（如果存在）
         * 供 WorshipFragment 优先加载使用
         */
        fun getLocal3DModelPath(context: Context): String? {
            val sp = SPUtils.getInstance(SP_NAME)
            val fileName = sp.getString("${KEY_3D_MODULE}_name", "")
            if (fileName.isEmpty()) return null
            val dir = context.getExternalFilesDir(DIR_MODULE) ?: return null
            val file = File(dir, fileName)
            return if (file.exists()) file.absolutePath else null
        }

        /**
         * 获取本地已缓存的普通模型文件路径（如果存在）
         */
        fun getLocalModulePath(context: Context): String? {
            val sp = SPUtils.getInstance(SP_NAME)
            val fileName = sp.getString("${KEY_MODULE}_name", "")
            if (fileName.isEmpty()) return null
            val dir = context.getExternalFilesDir(DIR_MODULE) ?: return null
            val file = File(dir, fileName)
            return if (file.exists()) file.absolutePath else null
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val moduleUrl = intent?.getStringExtra(EXTRA_MODULE_URL) ?: ""
        val threeDUrl = intent?.getStringExtra(EXTRA_3D_URL) ?: ""
        val threeDName = intent?.getStringExtra(EXTRA_3D_NAME) ?: ""
        val threeDUpdateDate = intent?.getStringExtra(EXTRA_3D_UPDATE_DATE) ?: ""

        if (moduleUrl.isEmpty() && threeDUrl.isEmpty()) {
            log(TAG, "无模型下载地址，服务退出")
            stopSelf()
            return START_NOT_STICKY
        }

        //待完成任务计数，全部完成后停止服务
        val pending = AtomicInteger(0)
        val onTaskFinish = {
            if (pending.decrementAndGet() <= 0) {
                stopSelf()
            }
        }

        //普通模型文件：以url中的文件名为准，无服务端时间戳字段时用url本身作为版本依据
        if (moduleUrl.isNotEmpty()) {
            val fileName = moduleUrl.substringAfterLast('/')
            if (fileName.isNotEmpty()) {
                pending.incrementAndGet()
                scope.launch {
                    downloadIfNeeded(moduleUrl, fileName, moduleUrl, KEY_MODULE)
                    onTaskFinish()
                }
            }
        }

        //3D模型文件：统一使用服务端命名 + 更新时间戳
        if (threeDUrl.isNotEmpty()) {
            val fileName = resolve3DFileName(threeDUrl, threeDName)
            if (fileName.isNotEmpty()) {
                pending.incrementAndGet()
                scope.launch {
                    downloadIfNeeded(threeDUrl, fileName, threeDUpdateDate, KEY_3D_MODULE)
                    onTaskFinish()
                }
            }
        }

        if (pending.get() <= 0) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    /**
     * 解析3D模型本地文件名：优先使用服务端统一命名，缺扩展名时从url补齐
     */
    private fun resolve3DFileName(url: String, serverName: String): String {
        val urlName = url.substringAfterLast('/').substringBefore('?')
        if (serverName.isEmpty()) return urlName
        val ext = urlName.substringAfterLast('.', "")
        return if (serverName.contains('.') || ext.isEmpty()) serverName else "$serverName.$ext"
    }

    /**
     * 根据「文件命名 + 时间戳」判断是否需要下载，需要则执行下载
     *
     * @param url       下载地址
     * @param fileName  目标文件名
     * @param timestamp 本次版本依据(3D模型用更新时间戳，普通模型用url)
     * @param spKey     SP记录前缀
     */
    private fun downloadIfNeeded(url: String, fileName: String, timestamp: String, spKey: String) {
        try {
            val dir = getExternalFilesDir(DIR_MODULE) ?: filesDir
            if (!dir.exists()) dir.mkdirs()

            val targetFile = File(dir, fileName)
            val sp = SPUtils.getInstance(SP_NAME)
            val oldName = sp.getString("${spKey}_name", "")
            val oldTime = sp.getString("${spKey}_time", "")

            //文件名、时间戳均一致且文件存在 -> 跳过下载
            if (targetFile.exists() && oldName == fileName && oldTime == timestamp) {
                log(TAG, "文件已是最新，跳过下载: $fileName")
                return
            }

            log(TAG, "开始下载: $url")
            val request = Request.Builder().url(url).build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    loge(TAG, "下载失败 httpCode=${response.code} url=$url")
                    return
                }
                val body = response.body ?: return

                //先写入临时文件，下载完整后再重命名，避免产生损坏文件
                val tmpFile = File(dir, "$fileName.tmp")
                body.byteStream().use { input ->
                    FileOutputStream(tmpFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }

                //清理旧版本文件(文件名变更的情况)
                if (oldName.isNotEmpty() && oldName != fileName) {
                    val oldFile = File(dir, oldName)
                    if (oldFile.exists()) oldFile.delete()
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tmpFile.renameTo(targetFile)) {
                    loge(TAG, "文件重命名失败: $fileName")
                    tmpFile.delete()
                    return
                }
            }

            //记录本次的文件名与时间戳，作为下次判断依据
            sp.put("${spKey}_name", fileName)
            sp.put("${spKey}_time", timestamp)
            log(TAG, "下载完成: ${targetFile.absolutePath}")
        } catch (e: Exception) {
            loge(TAG, "下载异常: ${e.message}")
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
