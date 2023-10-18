package com.fingertip.baselib.top

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.ToastUtils
import com.fingertip.baseLib.R
import com.fingertip.baselib.constant.CodeConstant
import com.fingertip.baselib.log
import com.fingertip.baselib.util.PicUtils
import com.fingertip.baselib.view.ChoosePicDialog
import com.fingertip.baselib.viewmodel.TopViewModel
import com.zxy.tiny.callback.FileCallback
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File

/**
 * 带权限申请的fragment
 */
abstract class TopPmFragment<VM : TopViewModel> : TopVMFragment<VM>(),
    EasyPermissions.PermissionCallbacks,FileCallback {

    val picUtils: PicUtils by lazy { PicUtils() }
    var autoGoPhotoAfterPermissionGranted = true //授权后是否自动拍照/打开相册

    protected val picChoseDialog: ChoosePicDialog by lazy {  ChoosePicDialog(_mActivity,onItemClick={
        when(it){
            0->{ //相机
                useCamera()
            }
            1->{ //相册
                useAlbum()
            }
        }
    }) }


    open fun useAlbum():Boolean {

        if (EasyPermissions.hasPermissions(activity, *album_pm)) {
            if (!autoGoPhotoAfterPermissionGranted) return true
            gotoAlbum()
        } else {
            EasyPermissions.requestPermissions(
                this,
                "请求获取读取相册权限",
                R.string.yes,
                R.string.no,
                1,
                *album_pm
            )
        }
        return false
    }


    open fun setUpPicType(): Int = -1




    open fun useCamera():Boolean {
        if (EasyPermissions.hasPermissions(activity, *camera_pm)) {
            if (!autoGoPhotoAfterPermissionGranted) return true
            takeCamera(CodeConstant.IntentSkipCode.REQUESTCODE_CAMERA)
        } else {
            EasyPermissions.requestPermissions(
                this,
                "请求获相机使用权限",
                R.string.yes,
                R.string.no,
                2,
                *camera_pm
            )
        }
        return false
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>?) {
        log("onPermissionsGranted", "sucess+" + requestCode)
        when(requestCode){
            1->{
                useAlbum()
            }
            2->{
                useCamera()
            }
        }
    }

    private fun isAllGranted(requestPerm: Array<String>, grantedPerm: List<String?>?): Boolean {
        grantedPerm?.let {
            var allGranted = true
            for (permission in requestPerm) {
                if (!grantedPerm.contains(permission)) {
                    allGranted = false
                    break
                }
            }
            return allGranted
        }
        return false
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>?) {
        perms?.let {
            if (EasyPermissions.somePermissionPermanentlyDenied(this, it)) {
                AppSettingsDialog.Builder(this).build().show()
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        log("onActivityResult", "-----")

        if (resultCode == RESULT_OK) {
            if (requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_ALBUM || requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_CAMERA) {
                if (Build.VERSION.SDK_INT>=29&&requestCode == CodeConstant.IntentSkipCode.REQUESTCODE_ALBUM){
                    val timestamp = System.currentTimeMillis()
                    data?.data.run {
                        picUtils.copyUriToExternalFilesDir(this,"${timestamp}_copy.png")
                        picUtils.density(picUtils.mcopy+"${timestamp}_copy.png",this@TopPmFragment)
                    }
                }else{
                    picUtils.getPhonePicPath(requestCode, data)?.run {
                        picUtils.density(this,this@TopPmFragment)
                    }
                }


            }
        }
    }


    open fun takeCamera(requestCode: Int) {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        activity?.run {
          /*  if (takePictureIntent.resolveActivity(packageManager) != null) {

            }else{
                log("--------------------","wcn1")
            }*/
            var photoFile: File? = null
            photoFile = picUtils.createImageFile()
            if (photoFile != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val contentUri = FileProvider.getUriForFile(
                        _mActivity,
                        _mActivity.packageName + ".fileProvider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri)
                } else {
                    takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile)
                    )
                }
                this@TopPmFragment. startActivityForResult(takePictureIntent, requestCode)
            }
        }

    }
    fun gotoAlbum() {
        val i = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(i, CodeConstant.IntentSkipCode.REQUESTCODE_ALBUM)
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort("打开相册失败")
        }
    }

    override fun callback(isSuccess: Boolean, outfile: String?, t: Throwable?) {
        log(fName,"压缩回调--isSc==$isSuccess+outfile==$outfile")
    }


    private val album_pm = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    private val camera_pm = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )


}