package com.fingertip.uilib.fragment.worshiping

import android.animation.ValueAnimator
import android.net.Uri
import android.view.View
import android.view.animation.LinearInterpolator
import com.bumptech.glide.Glide
import com.fingertip.baselib.log
import com.fingertip.baselib.loge
import com.fingertip.baselib.event_bus.MessageEvent
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.uilib.R
import com.fingertip.uilib.databinding.FragmentWorshipBinding
import com.fingertip.uilib.viewmodel.WorshipVM
import com.eqgis.eqr.utils.ScaleTool
import com.google.sceneform.Node
import com.google.sceneform.math.Quaternion
import com.google.sceneform.math.Vector3
import com.google.sceneform.rendering.ModelRenderable
import org.greenrobot.eventbus.Subscribe

/**
 * 拜佛页面
 *
 * 功能：
 * - 优先加载预下载的3D模型(.glb)
 * - 3D模型加载/下载失败时回退到普通模型(jpg/png)图片
 * - 全屏显示3D模型或图片，支持触摸旋转
 * - 基于 Sceneform-EQR (EQ-Renderer) 渲染引擎
 */
class WorshipFragment : TopPmFragment<WorshipVM>() {

    companion object {
        private const val TAG = "WorshipFragment"
        /** 模型距离相机的距离 */
        private const val MODEL_DISTANCE = 3.6f
        /** 自动旋转一圈的时长(ms) */
        private const val ROTATE_DURATION = 8000L
    }

    private val binding get() = mBinding as FragmentWorshipBinding

    private var modelNode: Node? = null
    private var rotateAnimator: ValueAnimator? = null

    override fun layoutId(): Int = R.layout.fragment_worship

    override fun initVM(): WorshipVM = provideVM()

    override fun initShiTu() {
        initSceneLayout()
        showLoading(true)
        mViewModel.getOrDownloadModel(requireContext())
    }

    override fun initObserver() {
        super.initObserver()

        mViewModel.downloadProgress.observe(viewLifecycleOwner) { progress ->
            val percent = (progress * 100).toInt()
            binding.tvLoadingStatus.text = "下载模型中... $percent%"
        }

        mViewModel.modelReady.observe(viewLifecycleOwner) { localFilePath ->
            log(TAG, "3D模型就绪: $localFilePath")
            showLoading(false)
            // 隐藏兜底图片，显示3D场景
            binding.ivFallbackImage.visibility = View.GONE
            binding.sceneLayout.visibility = View.VISIBLE
            loadModel(localFilePath)
        }

        mViewModel.fallbackImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            log(TAG, "回退到普通模型图片: $imageUrl")
            showLoading(false)
            // 隐藏3D场景，显示兜底图片
            binding.sceneLayout.visibility = View.GONE
            binding.ivFallbackImage.visibility = View.VISIBLE
            loadFallbackImage(imageUrl)
        }

        mViewModel.loadError.observe(viewLifecycleOwner) { errorMsg ->
            loge(TAG, "模型加载失败: $errorMsg")
            showLoading(false)
            binding.tvLoadingStatus.visibility = View.VISIBLE
            binding.tvLoadingStatus.text = "加载失败，请重试"
        }

        mViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            showLoading(isLoading)
        }
    }

    /**
     * 初始化3D场景
     */
    private fun initSceneLayout() {
        val ctx = context ?: return

        // 初始化场景，添加环境光照
        binding.sceneLayout.init(ctx)
            .addIndirectLight("enviroments/light/lightroom_ibl.ktx", 50)

        // 设置相机参数
        binding.sceneLayout.camera.setVerticalFovDegrees(45f)
        binding.sceneLayout.camera.setFarClipPlane(100f)

        // 设置背景透明（使用布局自身的黑色背景）
        binding.sceneLayout.setTransparent(true)
    }

    /**
     * 加载3D模型到场景
     */
    private fun loadModel(localFilePath: String?) {
        val ctx = context ?: return
        val layout = binding.sceneLayout
        val rootNode = layout.rootNode ?: return

        modelNode = Node().apply {
            isEnabled = false
        }

        val node = modelNode!!

        ModelRenderable
            .builder()
            .setSource(ctx, Uri.parse(localFilePath))
            .setIsFilamentGltf(true)
            .build()
            .thenAccept { modelRenderable ->
                // 设置渲染器
                node.setRenderable(modelRenderable)

                // 缩放成单位尺寸
                node.setLocalScale(
                    Vector3.one().scaled(ScaleTool.calculateUnitsScale(modelRenderable))
                )

                // 设置位置：在相机前方 MODEL_DISTANCE 处
                node.setLocalPosition(Vector3(0f, 0f, -MODEL_DISTANCE))

                // 关联到场景根节点
                node.setParent(rootNode)

                // 显示模型
                node.isEnabled = true

                // 启用触摸旋转（由SceneLayout统一处理）
                layout.enableTouchRotation(node)

                log(TAG, "3D模型加载完成")
            }
            .exceptionally { throwable ->
                // 3D模型加载失败，回退到普通模型图片
                loge(TAG, "3D模型加载异常: ${throwable.message}")
                val moduleUrl = com.fingertip.baselib.constant.GlobalConfig.globalParam?.buddhaConfig?.get(0)?.buddhaModuleUrl?:""
                if (moduleUrl.isNotEmpty()) {
                    log(TAG, "3D模型加载失败，回退到普通模型图片: $moduleUrl")
                    activity?.runOnUiThread {
                        binding.sceneLayout.visibility = View.GONE
                        binding.ivFallbackImage.visibility = View.VISIBLE
                        loadFallbackImage(moduleUrl)
                    }
                } else {
                    activity?.runOnUiThread {
                        binding.tvLoadingStatus.visibility = View.VISIBLE
                        binding.tvLoadingStatus.text = "加载失败，请重试"
                    }
                }
                null
            }
    }

    /**
     * 加载兜底普通模型图片(jpg/png)
     */
    private fun loadFallbackImage(imageUrl: String) {
        val ctx = context ?: return
        Glide.with(ctx)
            .load(imageUrl)
            .into(binding.ivFallbackImage)
        log(TAG, "兜底图片加载完成: $imageUrl")
    }

    /**
     * 显示/隐藏加载指示器
     */
    private fun showLoading(show: Boolean) {
        binding.progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvLoadingStatus.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.tvLoadingStatus.text = "加载模型中..."
        }
    }

    // ==================== 生命周期管理 ====================

    override fun onResume() {
        super.onResume()
        binding.sceneLayout.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.sceneLayout.pause()
    }

    override fun onDestroyView() {
        // 禁用触摸旋转
        binding.sceneLayout.disableTouchRotation()
        // 销毁模型渲染资源
        modelNode?.let { node ->
            node.renderableInstance?.destroy()
            node.setParent(null)
        }
        modelNode = null
        binding.sceneLayout.destroy()
        super.onDestroyView()
    }

    // ==================== EventBus ====================

    @Subscribe
    fun onMessageEvent(event: MessageEvent) {
        when (event.what) {

        }
    }
}
