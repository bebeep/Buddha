package com.fingertip.uilib.fragment.moment

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.blankj.utilcode.util.GsonUtils
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
import com.fingertip.uilib.databinding.FragmentMomentPostBinding
import com.fingertip.uilib.viewmodel.MomentVM
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
        return mutableListOf(binding.ivBack, binding.tvPost)
    }

    lateinit var operationDialog: ChoosePicDialog
    override fun initShiTu() {

        operationDialog = ChoosePicDialog(requireContext(), mutableListOf("录视频","拍照","相册"),onItemClick = {pos->
            when (operationDialog.mylist[pos]) {
                "录视频" -> { //录视频
                    takeVideo()
                }
                "拍照" -> { //拍照
                    takePhoto()
                }
                "相册"->{ //选相册
                    choosePhoto()
                }
            }
        })

        initAdapter()
        if (imageList.isEmpty() || (imageList.size < 9 && imageList[imageList.size-1]!=null ))imageList.add(null)
        adapter.initData(imageList)

    }


    override fun onSingleClick(v: View?) {
        super.onSingleClick(v)
        when(v?.id){
            R.id.iv_back -> pop()
            R.id.tv_post -> {
                //1.上传图片和视频
                if (videoMedia != null)
                {

                }
                else if (photoList.isNotEmpty())
                {
                    PicUtils.batchUploadToOss(
                        GlobalConfig.globalParam?.bucketName?:"",
                        photoList.map { it.objectKey }.toMutableList(),
                        photoList.map { it.mediaUrl }.toMutableList(),
                        object : PicUtils.OssBatchUploadCallback {
                            override fun onBatchStart(totalCount: Int) {
                                log("MomentPostFragment","onBatchStart")
                            }

                            override fun onFileProgress(index: Int,objectKey: String,currentSize: Long,totalSize: Long) {
                                log("MomentPostFragment","onOverallProgress:index:${index},objectKey:${objectKey},  ${currentSize}/${totalSize}")
                            }

                            override fun onFileSuccess(index: Int,objectKey: String,objectUrl: String) {
                                log("MomentPostFragment","onFileSuccess:index:${index},objectKey:${objectKey},objectUrl:${objectUrl}")
                            }

                            override fun onFileFailed(index: Int,objectKey: String,errorMsg: String) {
                                log("MomentPostFragment","onFileFailed:index:${index},objectKey:${objectKey},errorMsg:${errorMsg}")
                            }

                            override fun onOverallProgress(completedCount: Int,totalCount: Int) {
                                log("MomentPostFragment","onOverallProgress  ${completedCount}/${totalCount}")
                            }

                            override fun onBatchComplete(results: List<PicUtils.BatchFileResult>) {
                                log("MomentPostFragment","onBatchComplete,results：  ${GsonUtils.toJson(results)}")
                            }

                            override fun onAllFailed(errorMsg: String) {
                                log("MomentPostFragment","onAllFailed：${errorMsg}")
                            }
                        }
                    )
                }
            }
        }
    }


    private lateinit var adapter: UploadImageAdapter
    private var imageList = ArrayList<String?>()
    private var videoMedia :MediaInfo?=null
    private var photoList = ArrayList<MediaInfo>()
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
                        operationDialog.mylist = mutableListOf("录视频","拍照","相册")
                    }else
                    {
                        operationDialog.mylist = mutableListOf("拍照","相册")

                    }
                    operationDialog.myAdapter?.initData(operationDialog.mylist)
                    operationDialog.show()
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



    fun takePhoto()
    {
        if (photoList.size == 9) return
        GalleryPickerHelper.newInstance()
            .launchMediaPicker(requireActivity(), MediaType.CAMERA, object : MediaResultCallback {
                override fun onMediaResult(mediaFiles: List<MediaData>) {
                    for (media in mediaFiles)
                    {
                        log("MomentPostFragment","takePhoto:${media.filePath}", )
                        val mediaInfo = PicUtils.getMediaInfo(media.filePath)
                        if (mediaInfo != null)
                        {
                            photoList.add(mediaInfo)
                        }
                    }
                    refreshImages()
                }

                override fun onCancel() {
                    super.onCancel()
                    log("MomentPostFragment","cancel", )
                }
            })
    }

    fun takeVideo()
    {
        GalleryPickerHelper.newInstance()
            .ignoreSize(100) // kb
            .quality(75) // 75%
            .launchMediaPicker(requireActivity(), MediaType.CAPTURE_VIDEO, object : MediaResultCallback {
                override fun onMediaResult(mediaFiles: List<MediaData>) {
                    for (media in mediaFiles)
                    {
                        log("MomentPostFragment","takeVideo:${media.filePath}", )
                        videoMedia = PicUtils.getMediaInfo(media.filePath)
                    }
                }
            })
    }

    fun choosePhoto()
    {
        if (photoList.size == 9) return
        GalleryPickerHelper.newInstance()
            .ignoreSize(100) // kb
            .maxItems(9-photoList.size) // Single or Multiple
            .maxVideoSize(15) // Video file size limit in MB
            .launchMediaPicker(requireActivity(), MediaType.IMAGE_OR_VIDEO, object : MediaResultCallback {
                override fun onMediaResult(mediaFiles: List<MediaData>) {
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






    override fun onDestroy() {
        super.onDestroy()
        loadEnding()
    }
}