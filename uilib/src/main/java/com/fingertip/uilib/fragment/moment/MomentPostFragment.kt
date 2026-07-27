package com.fingertip.uilib.fragment.moment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.GsonUtils
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.MediaInfo
import com.fingertip.baselib.bean.PhotoEntity
import com.fingertip.baselib.constant.GlobalConfig
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.PicUtils
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.view.ChoosePicDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.UploadImageAdapter
import com.fingertip.uilib.adapter.UploadStatus
import com.fingertip.uilib.databinding.FragmentMomentPostBinding
import com.fingertip.uilib.viewmodel.MomentVM
import com.lzlz.toplib.extention.gone
import com.lzlz.toplib.extention.invisible
import com.lzlz.toplib.extention.visible
import com.photo.picker.GalleryPickerHelper
import com.photo.picker.MediaData
import com.photo.picker.MediaType
import com.photo.picker.callback.MediaResultCallback
import java.util.ArrayList

class MomentPostFragment:TopPmFragment<MomentVM>() {
    private val binding get() = mBinding as FragmentMomentPostBinding
    override fun layoutId() = R.layout.fragment_moment_post

    override fun initVM() = MomentVM()

    override fun getClickViews(): List<View> {
        return mutableListOf(binding.ivBack, binding.tvPost,binding.flVideo,binding.ivDelete)
    }

    lateinit var operationDialog: ChoosePicDialog
    override fun initShiTu() {

        operationDialog = ChoosePicDialog(requireContext(), mutableListOf("视频","照片"),onItemClick = {pos->
            when (operationDialog.mylist[pos]) {
                "视频" -> { //录视频
                    choosePhoto(true)
                }
                "照片"->{ //选相册
                    choosePhoto(false)
                }
            }
        })

        initAdapter()
        if (imageList.isEmpty() || (imageList.size < 9 && imageList[imageList.size-1]!=null ))imageList.add(null)
        adapter.initData(imageList)

        mViewModel.postMomentResult.observe(this) {
            if (it.success)
            {
                ToastUtil.showMessage("Post moment success")
                pop()
            }else
            {
                binding.tvPost.isEnabled = true
            }
        }
    }


    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.iv_back -> pop()
            R.id.fl_video -> {//预览视频
                videoMedia?.let {
                    GalleryPickerHelper.toPreViewVideo(requireActivity(), it.mediaUrl)
                }
            }
            R.id.iv_delete -> {//删除视频
                videoMedia = null
                imageList.clear()
                photoList.clear()
                if (imageList.isEmpty() || (imageList.size < 9 && imageList[imageList.size-1]!=null ))imageList.add(null)
                adapter.initData(imageList)
                binding.rcImages.visibility = View.VISIBLE
                binding.flVideo.visibility = View.GONE
            }
            R.id.tv_post -> {
                binding.tvPost.isEnabled = false
                //上传图片和视频
                if (videoMedia != null)
                {
                    if (videoMedia?.thumbUrl?.isEmpty() == true || videoMedia?.thumbObjectKey?.isEmpty() == true) {
                        uploadFiles(mutableListOf(videoMedia?.mediaObjectKey?:""),mutableListOf(videoMedia?.mediaUrl?:""))
                    }else
                    {
                        uploadFiles(mutableListOf(videoMedia?.mediaObjectKey?:"",videoMedia?.thumbObjectKey?:""),
                            mutableListOf(videoMedia?.mediaUrl?:"",videoMedia?.thumbUrl?:""))
                    }

                }
                else if (photoList.isNotEmpty())
                {
                    uploadFiles(photoList.map { it.mediaObjectKey }.toMutableList(),photoList.map { it.mediaUrl }.toMutableList())
                }
                else if (binding.etMomentContent.text.toString().trim().isNotEmpty())//纯文本提交
                {
                    mViewModel.postMoment(textContent = binding.etMomentContent.text.toString().trim())
                }
            }
        }
    }


    private lateinit var adapter: UploadImageAdapter
    private var imageList = ArrayList<String?>()
    private var videoMedia :MediaInfo?=null //视频信息
    private var photoList = ArrayList<MediaInfo>() //图片信息
    private fun initAdapter(){
        adapter = UploadImageAdapter(requireContext()){ position, isDelete ->
            if (isDelete){ //删除
                imageList.removeAt(position)
                photoList.removeAt(position)
                if (imageList.size < 9 && imageList[imageList.size - 1] != null) imageList.add(null)
                adapter.initData(imageList)
            }
            else
            {
                if (imageList[position] == null) {//添加图片
                    if ((imageList.size==9 && imageList[8] !=null) || videoMedia != null){
                        ToastUtil.showMessage("Please delete one picture or video")
                        return@UploadImageAdapter
                    }

                    if (position == 0) //初始状态
                    {
                        operationDialog.mylist = mutableListOf("视频","照片")
                        operationDialog.myAdapter?.initData(operationDialog.mylist)
                        operationDialog.show()
                    }else
                    {
                        choosePhoto(false)
                    }
                } else {//预览大图
                    BigImageDialog(requireContext(), imageList, position).show()
                }
            }
        }
        binding.rcImages.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rcImages.adapter = adapter
        adapter.initData(imageList)
    }



    private fun refreshImages(){
        imageList.clear()
        if (photoList.isEmpty()) {
            return
        }
        for (photo in photoList)  imageList.add(photo.mediaUrl)
        if (imageList.size<9&&imageList[imageList.size - 1] !=null)imageList.add(null)
        binding.rcImages.post {
            adapter.initData(imageList)
        }
    }

    private fun refreshVideoUI()
    {
        videoMedia?.apply {
            binding.rcImages.visibility = View.GONE
            binding.flVideo.visibility = View.VISIBLE
            Glide.with(requireContext()).asBitmap().load(this.thumbUrl).into(binding.ivPhoto)


        }

    }

    fun choosePhoto(isVideo: Boolean)
    {
        if ((isVideo && photoList.size == 1) || (!isVideo && photoList.size == 9)) return
        val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
        GalleryPickerHelper.newInstance()
            .ignoreSize(100)
            .maxItems(9-photoList.size)
            .maxVideoSize(15)
            .launchMediaPicker(requireActivity(), mediaType, object : MediaResultCallback {
                override fun onMediaResult(mediaFiles: List<MediaData>) {
                    if (mediaFiles.isEmpty())
                    {
                         return
                    }
                    val firstMedia = PicUtils.getMediaInfo(mediaFiles.first().filePath)
                    if (firstMedia != null && firstMedia.mediaType == 2)
                    {
                        videoMedia = firstMedia
                        refreshVideoUI()
                        return
                    }

                    for (media in mediaFiles)
                    {
                        log("MomentPostFragment","choosePhoto:${media.filePath}", )
                        val mediaInfo = PicUtils.getMediaInfo(media.filePath)
                        if (mediaInfo != null && photoList.find { it.mediaUrl == mediaInfo.mediaUrl } == null)
                        {
                            photoList.add(mediaInfo)
                        }
                    }
                    refreshImages()
                }
            })
    }



    private fun uploadFiles(objectKeys: MutableList<String>,localFilePaths: MutableList<String>)
    {
        // 标记所有待上传项为 UPLOADING 状态
        if (videoMedia != null)
        {
            binding.vStatusOverlay.visible()
            binding.pbLoading.visible()
            binding.tvSuccess.gone()
            binding.tvFail.gone()
        }
        else
        {
            val uploadingStatuses = HashMap<Int, UploadStatus>()
            for (i in imageList.indices) {
                if (imageList[i] != null && localFilePaths.contains(imageList[i])) {
                    uploadingStatuses[i] = UploadStatus.UPLOADING
                }
            }
            adapter.setUploadStatuses(uploadingStatuses)
        }


        PicUtils.batchUploadToOss(
            GlobalConfig.globalParam?.bucketName?:"",
            objectKeys,
            localFilePaths,
            object : PicUtils.OssBatchUploadCallback {
                override fun onBatchStart(totalCount: Int) {
                    log("MomentPostFragment","onBatchStart")
                }

                override fun onFileProgress(index: Int,objectKey: String,currentSize: Long,totalSize: Long) {
                }

                override fun onFileSuccess(index: Int,objectKey: String,objectUrl: String) {
                    log("MomentPostFragment","onFileSuccess:index:${index},objectKey:${objectKey}")
                    if (videoMedia == null)
                    {
                        val localPath = if (index < localFilePaths.size) localFilePaths[index] else null
                        if (localPath != null) {
                            val pos = imageList.indexOf(localPath)
                            if (pos >= 0) {
                                binding.rcImages.post { adapter.setUploadStatus(pos, UploadStatus.SUCCESS) }
                            }
                        }
                    }
                }

                override fun onFileFailed(index: Int,objectKey: String,errorMsg: String) {
                    log("MomentPostFragment","onFileFailed:index:${index},objectKey:${objectKey},errorMsg:${errorMsg}")
                    binding.tvPost.post { binding.tvPost.isEnabled = true }
                    if (videoMedia != null)
                    {
                        binding.vStatusOverlay.post {
                            binding.vStatusOverlay.gone()
                            binding.pbLoading.gone()
                            binding.tvSuccess.gone()
                            binding.tvFail.visible()
                        }
                    }
                    else
                    {
                        val localPath = if (index < localFilePaths.size) localFilePaths[index] else null
                        if (localPath != null) {
                            val pos = imageList.indexOf(localPath)
                            if (pos >= 0) {
                                binding.rcImages.post { adapter.setUploadStatus(pos, UploadStatus.FAIL)}
                            }
                        }
                    }

                }

                override fun onOverallProgress(completedCount: Int,totalCount: Int) {
                }

                override fun onBatchComplete(results: List<PicUtils.BatchFileResult>) {
                    log("MomentPostFragment","onBatchComplete,results：  ${GsonUtils.toJson(results)}")
                    //全部成功
                    if (videoMedia != null)
                    {
                        binding.vStatusOverlay.post {
                            binding.vStatusOverlay.gone()
                            binding.pbLoading.gone()
                            binding.tvSuccess.visible()
                            binding.tvFail.gone()
                        }

                        val videoUrl = results.find { it.objectKey == videoMedia?.mediaObjectKey }?.objectUrl
                        val videoCover = results.find { it.objectKey == videoMedia?.thumbObjectKey }?.objectUrl

                        if (videoUrl != null && videoCover != null)
                        {
                            mViewModel.postMoment(textContent = binding.etMomentContent.text.toString().trim(),
                                videoUrl = videoUrl,
                                videoCover = videoCover,
                                videoDuration = videoMedia?.duration?:0,
                                videoWidth = videoMedia?.width?:0,
                                videoHeight = videoMedia?.height?:0,
                                isAnonymous = binding.switchHideName.isChecked,
                                location = binding.tvLocate.text.toString().trim(),
                                remindAccountIds = mutableListOf(1,2,3))
                        }
                        else
                        {
                            binding.vStatusOverlay.post {
                                binding.tvPost.isEnabled = true
                                binding.vStatusOverlay.visible()
                                binding.pbLoading.gone()
                                binding.tvSuccess.gone()
                                binding.tvFail.visible()
                            }
                        }
                    }
                    else
                    {
                        if (results.isNotEmpty() && results.size == photoList.size)
                        {
                            val imageUrl = results.mapNotNull { it.objectUrl}.joinToString (",")
                            mViewModel.postMoment(textContent = binding.etMomentContent.text.toString().trim(),
                                imageUrl = imageUrl,
                                isAnonymous = binding.switchHideName.isChecked,
                                location = binding.tvLocate.text.toString().trim(),
                                remindAccountIds = mutableListOf(1,2,3))
                        }
                        else
                        {
                            binding.vStatusOverlay.post {
                                binding.tvPost.isEnabled = true
                                binding.vStatusOverlay.visible()
                                binding.pbLoading.gone()
                                binding.tvSuccess.gone()
                                binding.tvFail.visible()
                            }
                        }
                    }
                }

                override fun onAllFailed(errorMsg: String) {
                    log("MomentPostFragment","onAllFailed：${errorMsg}")
                    // 全部失败
                    binding.tvPost.post { binding.tvPost.isEnabled = true }
                    if (videoMedia != null)
                    {
                        binding.vStatusOverlay.post {
                            binding.vStatusOverlay.visible()
                            binding.pbLoading.gone()
                            binding.tvSuccess.gone()
                            binding.tvFail.visible()
                        }
                    }
                    else
                    {
                        for (i in imageList.indices) {
                            if (localFilePaths.contains(imageList[i])) {
                                binding.rcImages.post { adapter.setUploadStatus(i, UploadStatus.FAIL) }
                            }
                        }
                    }
                }
            }
        )
    }



    override fun onDestroy() {
        super.onDestroy()
        loadEnding()
    }
}