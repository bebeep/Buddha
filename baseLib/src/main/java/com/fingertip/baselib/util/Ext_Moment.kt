package com.fingertip.baselib.util

import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.fingertip.baselib.bean.CommentEntity
import com.fingertip.baselib.bean.MomentEntity
import com.fingertip.baselib.top.TopApplication
import java.lang.Exception
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * 移除列表中某个动态
 */
fun ArrayList<MomentEntity>?.removeMoment(momentId:Int):ArrayList<MomentEntity>?{
    this?.let {
        var removeMoment:MomentEntity?=null
        for (moment in it) {
            if (moment.momentId == momentId) removeMoment = moment
        }
        removeMoment?.let { this.remove(removeMoment) }
        return this
    }
    return this
}

/**
 * 修改列表中某个动态的评论数
 */
fun ArrayList<MomentEntity>?.refreshComment( momentId:Int,commentCount:Int):Int{
    this?.let {
        for (moment in it.withIndex()) {
            if (moment.value.momentId == momentId) {
                moment.value.commments = commentCount
                return moment.index
            }
        }
    }
    return -1
}


/**
 * 移除列表中某个评论
 */
fun ArrayList<CommentEntity>?.removeComment(commentId:Int):ArrayList<CommentEntity>?{
    this?.let {
        var removeIndex = -1
        var removeChildIndex = -1
        for (comment in this.withIndex()) {
            if(comment.value.commentId == commentId){
                removeIndex = comment.index
            }
            for (childMoment in comment.value.expandComments.withIndex()){
                if(childMoment.value.commentId == commentId){
                    removeIndex = comment.index
                    removeChildIndex = childMoment.index
                }
            }

        }
        if (removeIndex != -1 && removeChildIndex == -1){
            removeAt(removeIndex)
        }
        if (removeIndex != -1 && removeChildIndex != -1){
            this[removeIndex].expandComments.removeAt(removeChildIndex)
        }
    }

    return this
}

/**
 * 点击事件自动禁用500ms
 * 避免连续快速点击
 */
fun View.autoClickEnable(){
    isEnabled = false
    postDelayed({
        isEnabled = true
    },500)
}


/**
 * 区分单击事件和双击事件
 */
fun View.setOnDoubleClickListener(callback: (isDoubleClick:Boolean) -> Unit){
    var clickTimes = 0
    var isResolving = false
    setOnClickListener {
        clickTimes++
        if (isResolving) return@setOnClickListener
        isResolving = true
        postDelayed({
            clickTimes = if (clickTimes > 1){
                callback(true)
                isResolving = false
                0
            }else{
                callback(false)
                isResolving = false
                0
            }
        },500)
    }
}

fun RecyclerView.setOnDoubleClickInEmptyArea(callback: (isDoubleClick:Boolean) -> Unit){
    var scrollX = 0f
    var scrollY = 0f
    var clickTimes = 0
    var isResolving = false
    setOnTouchListener { view, event ->
        if(event.action == MotionEvent.ACTION_DOWN){
            scrollX = event.x
            scrollY = event.y
        }
        if (event.action == MotionEvent.ACTION_UP) {
            clickTimes++
            if (isResolving) return@setOnTouchListener false
            if (view.id != 0 && abs(scrollX - event.x) <= 5 && abs(scrollY - event.y) <= 5) {
                isResolving = true
                postDelayed({
                    clickTimes = if (clickTimes > 1){
                        callback(true)
                        isResolving = false
                        0
                    }else{
                        callback(false)
                        isResolving = false
                        0
                    }
                },500)
            }
        }

        return@setOnTouchListener false

    }
}


/**
 * 去除文本前面的换行符
 * "\n\n\n\nasdasd\n"
 */
fun String?.clearFirstLine():String{
    if (isNullOrEmpty())return ""
    if (Pattern.compile("\t|\r|\n|\\s*").matcher(this).replaceAll("").isEmpty()) return ""
    val linesText = split("\n")
    var firstText = ""
    var lastText = ""
    linesText.forEach {
        if (it !="\n" && firstText.isEmpty()) firstText = it
        if (it !="\n" && it!="\t" && it!= " " && it.isNotEmpty()) lastText = it
    }

    val firstTextIndex = indexOf(firstText)
    val lastTextIndex = indexOf(lastText)
    try {
        return replaceSensitive(substring(firstTextIndex,lastTextIndex+lastText.length))
    }catch (e:Exception){}
    return replaceSensitive(substring(firstTextIndex))
}

/**
 * 替换敏感词
 */
fun replaceSensitive(content:String):String{
    TopApplication.sensitiveWordsBean?.let {
        val antiSpamRegex = it.getSensitiveWords()
        return Pattern.compile(antiSpamRegex).matcher(content).replaceAll("****")
    }
    return content
}


/**
 * 评论/点赞数超过1000格式化K为单位
 */
fun Int?.toNumString(max:Boolean = false):String{
    if (this == null)return "0"
    if (this>99999 && max) return "99.9 k"
    return if (this < 1000) toString()
    else "${String.format(Locale.ENGLISH,"%.1f",this.toFloat()/1000f)} k"
}