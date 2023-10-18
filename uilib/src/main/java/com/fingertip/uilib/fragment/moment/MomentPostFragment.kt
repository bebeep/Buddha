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
import com.fingertip.uilib.utils.MomentUtils
import com.fingertip.uilib.viewmodel.MomentVM
import com.huantansheng.easyphotos.EasyPhotos
import com.huantansheng.easyphotos.setting.Setting
import com.huantansheng.easyphotos.utils.GlideEngine
import com.huantansheng.easyphotos.utils.file.FileUtils
import com.huantansheng.easyphotos.utils.media.DurationUtils
import com.huantansheng.easyphotos.utils.media.MediaScannerConnectionUtils
import com.huantansheng.easyphotos.utils.uri.UriUtils
import kotlinx.android.synthetic.main.fragment_moment_post.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class MomentPostFragment:TopPmFragment<MomentVM>() {
    override fun layoutId() = R.layout.fragment_moment_post

    override fun initVM() = MomentVM()



    lateinit var operationDialog: ChoosePicDialog
    override fun initShiTu() {
        autoGoPhotoAfterPermissionGranted = false

        operationDialog = ChoosePicDialog(requireContext(), mutableListOf("录视频","拍照","相册"),onItemClick = {pos->
            when (pos) {
                0 -> { //录视频
                    if (useCamera()) takeVideo()
                }
                1 -> { //拍照
                    if (useCamera()) takePhoto()
                }
                2->{ //选相册
                    if (useAlbum()) selectPhoto()
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
                    if ((imageList.size==9 && imageList[8] !=null) || videoPath.isNotEmpty()){
                        ToastUtil.showMessage("Please delete one picture or video")
                        return@UploadImageAdapter
                    }
                    when(operateTag){
                        0->operationDialog?.show()
                        1->takePhoto()
                        2->takeVideo()
                    }
                } else {//预览大图
                    BigImageDialog(requireContext(), imageList, position).show()
                }
            }
        }
        rc_images.layoutManager = GridLayoutManager(requireContext(), 3)
        rc_images.adapter = adapter
        adapter.initData(imageList)
    }



    private fun selectPhoto(){
        val builder = EasyPhotos.createAlbum(this, true, true, GlideEngine.getInstance())
            .setSelectedPhotos(photoList, true)
            .setVideoMinSecond(3)
            .setPuzzleMenu(false)
            .setCleanMenu(false)
            .setCameraLocation(Setting.LIST_FIRST)
            .setGif(false)
        when (operateTag) {
            //可选照片也可选视频
            0 -> builder.complexSelector(true, 1, 9)
            //只能选照片
            1 -> builder.setCount(9)
            //只能选视频
            else -> builder.onlyVideo().setCount(1)
        }

        builder.start(999)
    }


    private fun refreshImages(){
        imageList.clear()
        setOperateTag()
        if (photoList.isEmpty()) {
            return
        }
        for (photo in photoList)  imageList.add(photo.path)
        if (imageList.size<9&&imageList[imageList.size - 1] !=null)imageList.add(null)
        rc_images?.post {
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


    /**
     * 拍照
     */
    private fun takePhoto(){
        createCameraTempImageFile()
        val imageUri = UriUtils.getUri(requireContext(), mTempImageFile)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //对目标应用临时授权该Uri所代表的文件
        cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) //对目标应用临时授权该Uri所代表的文件
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri) //将拍取的照片保存到指定URI
        startActivityForResult(cameraIntent, 666)
    }

    /**
     * 录视频
     */
    private fun takeVideo(){
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        startActivityForResult(videoIntent, 777)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>?) {
        log("onPermissionsGranted", "operateTag   $operateTag")
        when(operateTag){
            0->operationDialog.show()
            1->takePhoto()
            2->takeVideo()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        loadEnding()
    }

    private var videoPath = ""
    private var videoThumb = ""
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(resultCode){
            Activity.RESULT_OK -> {
                if (requestCode == 666) {//直接拍照
                    onCameraResult()//处理照片
                }
                if (data == null) return
                if (requestCode == 777) {//直接录视频 跳转到视频编辑页面
                    MomentUtils.goHandleVideo(_mActivity, FileUtils.getPath(requireContext(), data.data))
                }
                if (requestCode == 999) {//拍视频/选择视频返回
                    val operate = data.getIntExtra("operate", 0)
                    if (operate == 1) {//选择视频返回
                        photoList.clear()
                        val path = data.getStringExtra("path")?:""
                        MomentUtils.goHandleVideo(_mActivity,path)
                    } else { //选择照片返回
                        photoList = data.getParcelableArrayListExtra(EasyPhotos.RESULT_PHOTOS)!!
                        refreshImages()
                        rc_images.visibility = View.VISIBLE
                        fl_video.visibility = View.GONE
                    }

                }
            }
        }
    }



    override fun onFragmentResult(requestCode: Int, resultCode: Int, data: Bundle?) {
        super.onFragmentResult(requestCode, resultCode, data)
        if (requestCode == MomentUtils.REQUEST_CODE_HANDLE_VIDEO) {//处理视频回调
            videoPath = data?.getString("video-file-path")?:return
            val file = MomentUtils.getVideoThumb(videoPath)
            photoList.clear()
            imageList.clear()
            rc_images.visibility = View.GONE
            fl_video.visibility = View.VISIBLE
            videoThumb = file?.path?:""
            Glide.with(this).asBitmap().load(videoThumb).into(iv_photo)
            operateTag = 2
        }
    }


    //创建临时文件
    private var mTempImageFile: File?=null
    private fun createCameraTempImageFile() {
        var dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (null == dir) {
            dir = File(Environment.getExternalStorageDirectory(), File.separator + "DCIM" + File.separator + "Camera" + File.separator)
        }
        if (!dir.isDirectory) {
            if (!dir.mkdirs()) {
                dir = requireActivity().getExternalFilesDir(null)
                if (null == dir || !dir.exists()) {
                    dir = requireActivity().filesDir
                    if (null == dir || !dir.exists()) {
                        dir = requireActivity().filesDir
                        if (null == dir || !dir.exists()) {
                            val cacheDirPath = File.separator + "data" + File.separator + "data" + File.separator + requireActivity().packageName + File.separator + "cache" + File.separator
                            dir = File(cacheDirPath)
                            if (!dir.exists())  dir.mkdirs()
                        }
                    }
                }
            }
        }
        mTempImageFile = try {
            File.createTempFile("IMG", ".jpg", dir)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    //保存照片并刷新相册
    private fun onCameraResult() {
        Thread {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HH_mm_ss", Locale.getDefault())
            val imageName = "IMG_%s.jpg"
            val filename = String.format(imageName, dateFormat.format(Date()))
            val reNameFile = File(mTempImageFile!!.parentFile, filename)
            if (!reNameFile.exists()) {
                if (mTempImageFile!!.renameTo(reNameFile)) {
                    mTempImageFile = reNameFile
                }
            }
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(mTempImageFile!!.absolutePath, options)
            MediaScannerConnectionUtils.refresh(requireContext(), mTempImageFile)
            // 更新媒体库
            val uri = UriUtils.getUri(requireContext(), mTempImageFile)
            var width = 0
            var height = 0
            var orientation = 0
            if (Setting.useWidth) {
                width = options.outWidth
                height = options.outHeight
                var exif: ExifInterface? = null
                try {
                    exif = ExifInterface(mTempImageFile!!)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                if (null != exif) {
                    orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
                    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        width = options.outHeight
                        height = options.outWidth
                    }
                }
            }
            val photo = PhotoEntity(
                mTempImageFile!!.name, uri,
                mTempImageFile!!.absolutePath,
                mTempImageFile!!.lastModified() / 1000, width, height, orientation,
                mTempImageFile!!.length(),
                DurationUtils.getDuration(mTempImageFile!!.absolutePath),
                options.outMimeType
            )
            photoList.add(photo)
            refreshImages()
            rc_images.visibility = View.VISIBLE
            fl_video.visibility = View.GONE
        }.start()
    }


}