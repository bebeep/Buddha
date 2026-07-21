package com.fingertip.uilib.fragment.moment

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.fingertip.baselib.bean.PhotoEntity
import com.fingertip.baselib.dialog.BigImageDialog
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopPmFragment
import com.fingertip.baselib.util.ToastUtil
import com.fingertip.baselib.view.ChoosePicDialog
import com.fingertip.uilib.R
import com.fingertip.uilib.adapter.UploadImageAdapter
import com.fingertip.uilib.databinding.FragmentMomentPostBinding
import com.fingertip.uilib.utils.MomentUtils
import com.fingertip.uilib.viewmodel.MomentVM
import com.photo.picker.GalleryPickerHelper
import com.photo.picker.MediaData
import com.photo.picker.MediaType
import com.photo.picker.MimeType
import com.photo.picker.callback.MediaResultCallback
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class MomentPostFragment:TopPmFragment<MomentVM>() {
    private val binding get() = mBinding as FragmentMomentPostBinding
    override fun layoutId() = R.layout.fragment_moment_post

    override fun initVM() = MomentVM()


    lateinit var operationDialog: ChoosePicDialog
    override fun initShiTu() {
        autoGoPhotoAfterPermissionGranted = false

        operationDialog = ChoosePicDialog(requireContext(), mutableListOf("录视频","拍照","相册"),onItemClick = {pos->
            when (pos) {
                0 -> { //录视频
//                    if (useCamera()) takeVideo()
                    GalleryPickerHelper.newInstance()
                        .isCompress(true)
                        .isCrop(true)
                        .ignoreSize(100) // kb
                        .quality(75) // 75%
                        .maxItems(1) // Single or Multiple
                        .maxVideoSize(15) // Video file size limit in MB
                        .launchMediaPicker(requireActivity(), MediaType.CAPTURE_VIDEO, object : MediaResultCallback {
                            override fun onMediaResult(mediaFiles: List<MediaData>) {
                                // Handle result
                                mediaFiles.firstOrNull()?.let {
                                    val paths = arrayListOf<String>()
                                    paths.add(it.filePath)
                                    GalleryPickerHelper.toPreViewImage(requireActivity(), 0, paths)
                                }
                            }
                        })
                }
                1 -> { //拍照
//                    if (useCamera()) takePhoto()
                    GalleryPickerHelper.newInstance()
                        .isCompress(true)
                        .isCrop(true)
                        .ignoreSize(100) // kb
                        .quality(75) // 75%
                        .maxItems(1) // Single or Multiple
                        .maxVideoSize(15) // Video file size limit in MB
                        .launchMediaPicker(requireActivity(), MediaType.CAMERA, object : MediaResultCallback {
                            override fun onMediaResult(mediaFiles: List<MediaData>) {
                                // Handle result
                            }
                        })
                }
                2->{ //选相册
//                    if (useAlbum()) selectPhoto()
                    GalleryPickerHelper.newInstance()
                        .isCompress(true)
                        .isCrop(false)
                        .ignoreSize(100) // kb
                        .quality(75) // 75%
                        .maxItems(9) // Single or Multiple
                        .maxVideoSize(15) // Video file size limit in MB
                        .launchMediaPicker(requireActivity(), MediaType.IMAGE_OR_VIDEO, object : MediaResultCallback {
                            override fun onMediaResult(mediaFiles: List<MediaData>) {
                                // Handle result
                                mediaFiles.firstOrNull()?.let {
                                    if (it.mimeType == MimeType.VIDEO) {
                                        GalleryPickerHelper.toPreViewVideo(requireActivity(), it.filePath)
                                    } else if (it.mimeType == MimeType.IMAGE) {
                                        val paths = arrayListOf<String>()
                                        paths.add(it.filePath)
                                        GalleryPickerHelper.toPreViewImage(requireActivity(), 0, paths)
                                    }
                                }
                            }
                        })
                }
            }
        })




        setOperateTag()
        initAdapter()
        if (imageList.isEmpty() || (imageList.size < 9 && imageList[imageList.size-1]!=null ))imageList.add(null)
        adapter.initData(imageList)

    }




    private lateinit var adapter: UploadImageAdapter
    private var imageList = ArrayList<String?>()
    private var photoList = ArrayList<PhotoEntity>()
    private fun initAdapter(){
        adapter = UploadImageAdapter(requireContext()){ position, tag ->
            if (tag == 0){ //删除
                imageList.removeAt(position)
                photoList.removeAt(position)
                if (imageList.size < 9 && imageList[imageList.size - 1] != null) imageList.add(null)
                adapter.initData(imageList)
                setOperateTag()
            }else{
                if (imageList[position] == null) {//添加图片
//                    if ((imageList.size==9 && imageList[8] !=null) || videoPath.isNotEmpty()){
//                        ToastUtil.showMessage("Please delete one picture or video")
//                        return@UploadImageAdapter
//                    }
                    when(operateTag){
                        0->operationDialog?.show()
//                        1->takePhoto()
//                        2->takeVideo()
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
        setOperateTag()
        if (photoList.isEmpty()) {
            return
        }
        for (photo in photoList)  imageList.add(photo.path)
        if (imageList.size<9&&imageList[imageList.size - 1] !=null)imageList.add(null)
        binding.rcImages?.post {
            adapter.initData(imageList)

        }
    }

    /**
     * 操作逻辑：
     * 0 无照片无视频，能选照片/视频，能拍照片/视频
     * 1 已有照片，只能选择照片，只能拍照
     * 2 已有视频，只能选视频，只能拍视频
     */
    private var operateTag = 0
    private fun setOperateTag(){
        if (photoList.isEmpty()) {
            operateTag = 0
            return
        }
        operateTag = 1
        for (photo in photoList) if (photo.type.contains("video")) operateTag = 2
    }







    override fun onDestroy() {
        super.onDestroy()
        loadEnding()
    }

//    private var videoPath = ""
//    private var videoThumb = ""
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        when(resultCode){
//            Activity.RESULT_OK -> {
//                if (requestCode == 666) {//直接拍照
//                    onCameraResult()//处理照片
//                }
//                if (data == null) return
//                if (requestCode == 777) {//直接录视频 跳转到视频编辑页面
//                    MomentUtils.goHandleVideo(_mActivity, FileUtils.getPath(requireContext(), data.data))
//                }
//                if (requestCode == 999) {//拍视频/选择视频返回
//                    val operate = data.getIntExtra("operate", 0)
//                    if (operate == 1) {//选择视频返回
//                        photoList.clear()
//                        val path = data.getStringExtra("path")?:""
//                        MomentUtils.goHandleVideo(_mActivity,path)
//                    } else { //选择照片返回
//                        photoList = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS)!!
//                        refreshImages()
//                        binding.rcImages.visibility = View.VISIBLE
//                        binding.flVideo.visibility = View.GONE
//                    }
//
//                }
//            }
//        }
//    }



    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle?) {
        super.onFragmentResult(requestCode, resultCode, data)

    }


}