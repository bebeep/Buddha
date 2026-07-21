package com.fingertip.uilib.fragment.moment

import androidx.recyclerview.widget.GridLayoutManager
import com.fingertip.baselib.bean.MediaInfo
import com.fingertip.baselib.bean.PhotoEntity
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

    /**
     * 操作逻辑：
     * 0 无照片无视频，能选照片/视频，能拍照片/视频
     * 1 已有照片，只能选择照片/拍照
     * 2 已有视频，只能选视频/拍视频
     */
    private fun getOperateTag(): Int{
        if (videoMedia != null)
        {
            return  2
        }
        if (photoList.isEmpty()) {
            return 0
        }
        return 1
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
                        val mediaInfo = picUtils.getMediaInfo(media.filePath)
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
                        videoMedia = picUtils.getMediaInfo(media.filePath)
                    }
                }
            })
    }

    fun choosePhoto()
    {
        if (photoList.size == 9) return
        GalleryPickerHelper.newInstance()
            .ignoreSize(100) // kb
            .maxItems(9) // Single or Multiple
            .selectedPaths(photoList.map { it.mediaUrl }.toMutableList())
            .maxVideoSize(15) // Video file size limit in MB
            .launchMediaPicker(requireActivity(), MediaType.IMAGE, object : MediaResultCallback {
                override fun onMediaResult(mediaFiles: List<MediaData>) {
                    for (media in mediaFiles)
                    {
                        log("MomentPostFragment","choosePhoto:${media.filePath}", )
                        val mediaInfo = picUtils.getMediaInfo(media.filePath)
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