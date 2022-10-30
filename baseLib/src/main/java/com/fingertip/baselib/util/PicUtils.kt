package com.fingertip.baselib.util

import android.content.Intent
import android.net.Uri
import com.fingertip.baseLib.BuildConfig
import com.fingertip.baselib.constant.CodeConstant
import com.fingertip.baselib.log
import com.fingertip.baselib.top.TopApplication
import com.zxy.tiny.Tiny
import com.zxy.tiny.callback.FileCallback
import io.github.devzwy.nsfw.NSFWHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.*


/**
 * 图片压缩、鉴黄、上传工具类
 */
class PicUtils {

    var TAG=this::class.java.simpleName
    val mCropressPath:String = TopApplication.instance.cacheDir.path + "/compress/"
    val mphotoPath:String = TopApplication.instance.cacheDir.path+"/takephoto.jpg"
    val mcopy:String = TopApplication.instance.cacheDir.path+"/copy/"
    var showLoad:()-> Any = {}


    //创建图片文件
    fun createImageFile(): File? {
        val image = isHaveOrCreateFile(mphotoPath)
        log(TAG,"createImageFile :" + image?.path)
        return image
    }

    //压缩
    fun density(path: String?,callback: FileCallback) {
        showLoad.invoke()
        val options = Tiny.FileCompressOptions()
        isHaveOrCreateFolder(mCropressPath)
        options.outfile = mCropressPath + "my_pic"+System.currentTimeMillis()+".jpg"
        options.quality = 50

        options.isKeepSampling = false
        Tiny.getInstance().source(path).asFile().withOptions(options).compress(callback)
    }

    fun getPhonePicPath(requestCode: Int, data: Intent?): String? {
        var originalImagePath: String? = null
        try {
            if (requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_ALBUM && null != data) {
                val picturePath: String = SystemPhotoPathUtil.getPath(TopApplication.instance, data.data as Uri)?:""
                originalImagePath = picturePath
            } else if (requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_CAMERA) {
                originalImagePath = getImagePath()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        log(TAG,"onActivityResult path : $originalImagePath")
        return originalImagePath
    }


    fun copyUriToExternalFilesDir(uri: Uri?, fileName: String) {
        isHaveOrCreateFolder(mcopy)
        if (uri==null){
            return
        }

        val inputStream = TopApplication.instance.contentResolver.openInputStream(uri)
        val tempDir = mcopy
        if (inputStream != null && tempDir != null) {
            val file = File("$tempDir/$fileName")
            if (file.exists()){
                file.delete()
            }

            val fos = FileOutputStream(file)
            val bis = BufferedInputStream(inputStream)
            val bos = BufferedOutputStream(fos)
            val byteArray = ByteArray(1024)
            var bytes = bis.read(byteArray)
            while (bytes > 0) {
                bos.write(byteArray, 0, bytes)
                bos.flush()
                bytes = bis.read(byteArray)
            }
            bos.close()
            fos.close()
        }
    }

    //返回图片地址
    fun getImagePath(): String? {
        return mphotoPath
    }

    fun uploadFile(path: String,needCheckPorn:Boolean = true) {
        upPicCallBack?.start()
        //离线鉴黄  失败就直接返回
        if (needCheckPorn) NSFWHelper.getNSFWScore(path).let {
            if (it.nsfwScore > 0.3){
                upPicCallBack?.errorend()
                if (BuildConfig.DEBUG)  ToastUtil.showMessage("鉴黄不通过-》分数：${it.nsfwScore}")
                return
            }
            if (BuildConfig.DEBUG)  ToastUtil.showMessage("鉴黄通过-》分数：${it.nsfwScore}")
        }
        MainScope().launch (Dispatchers.Default){
            //todo 上传服务器
        }

    }

    //Identify yellow pictures(鉴别是否黄图)
    private fun checkPicIsSelf(avatarPath: String) {
        log(TAG, "开始鉴黄")

    }

    private fun isHaveOrCreateFolder(filePath:String):File{
        val file = File(filePath)
        if (!file.exists()) {
            if (!file.mkdirs())
                log(TAG,"创建压缩文件夹失败 path:" + file.absolutePath)
        }
        return file
    }

    //没有文件就创建文件
    private fun isHaveOrCreateFile(filePath:String):File{
        val file = File(filePath)
        if (!file.exists()) {
            if (!file.createNewFile())
                log(TAG,"创建压缩文件夹失败 path:" + file.absolutePath)
        }
        return file
    }

    var upPicCallBack : UpPicCallBack?=null
    interface UpPicCallBack{
        fun start()
        fun isOktoUp(avatarPath: String)
        fun errorend()
    }
}