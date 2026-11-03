package com.fingertip.uilib.login

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.fingertip.baselib.log
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Google 登录封装（Credential Manager）
 *
 * 为什么不用 GoogleSignInClient：老 SDK 已在 2025 年被 Google 废弃，
 * 新项目接入会在 Play 审核上被提示；Credential Manager 是官方替代方案，
 * 而且能顺带支持「已登录过的账号一键登录」，海外的 Google 用户体感差别很大。
 */
class GoogleLoginHelper(private val context: Context) {

    private val tag = "GoogleLogin"
    private val credentialManager = CredentialManager.create(context)

    /**
     * 拉起 Google 账号选择。
     *
     * @param scope 用 Activity 的 lifecycleScope，页面销毁后回调自然不再执行
     * @param onSuccess 返回 idToken
     * @param onCancel  用户主动取消（不算错误，不弹错误提示）
     * @param onError   其它失败
     */
    fun signIn(
        scope: CoroutineScope,
        onSuccess: (idToken: String) -> Unit,
        onCancel: () -> Unit = {},
        onError: (msg: String) -> Unit
    ) {
        if (!GoogleLoginConfig.isConfigured) {
            onError("Google 登录未配置")
            return
        }

        val option = GetGoogleIdOption.Builder()
            .setServerClientId(GoogleLoginConfig.SERVER_CLIENT_ID)
            // true 只列出「之前授权过本应用」的账号；首次使用会拿不到账号，
            // 所以这里用 false，允许用户选任意本机 Google 账号。
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        scope.launch(Dispatchers.Main) {
            try {
                val response = credentialManager.getCredential(context, request)
                val credential = response.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    log(tag, "获取到 idToken，账号=${googleCredential.id}")
                    onSuccess(googleCredential.idToken)
                } else {
                    onError("不支持的凭证类型：${credential.type}")
                }
            } catch (e: GetCredentialCancellationException) {
                // 用户关闭了账号选择弹窗
                onCancel()
            } catch (e: NoCredentialException) {
                // 本机没有可用的 Google 账号（或 Google Play 服务不可用）
                onError("本机没有可用的 Google 账号，请先在系统设置里登录 Google")
            } catch (e: GetCredentialException) {
                Log.w(tag, "Google 登录失败", e)
                onError(e.message ?: "Google 登录失败")
            } catch (e: Exception) {
                Log.w(tag, "Google 登录异常", e)
                onError("Google 登录异常")
            }
        }
    }

    /**
     * 退出登录时清掉 Credential Manager 里的账号状态。
     * 不清的话，下次点 Google 登录会直接复用上次的账号，用户换号会很困惑。
     */
    fun signOut(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(tag, "清理 Google 凭证状态失败", e)
            }
        }
    }
}
