package ru.krasview.kvlib.indep

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvlib.interfaces.FatalErrorExitListener
import ru.krasview.kvsecret.secret.ApiConst
import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("StaticFieldLeak")
object HTTPClient : KVHttpClient() {
    @SuppressLint("StaticFieldLeak")
    var mContext: Context? = null
    var exitListener: FatalErrorExitListener? = null
    @JvmStatic
	fun setContext(context: Context?) {
        mContext = context
    }

    @JvmStatic
	fun getXML(address: String, params: String?, request_auth_type: Int): String? {
        var address = address
        address = addParams(address, params)
        Log.d("http", address)
        var auth_address: String
        val account = AuthAccount.getInstance()
        auth_address = when (request_auth_type) {
            AuthRequestConst.AUTH_NONE -> address
            AuthRequestConst.AUTH_KRASVIEW -> {
                if (!account.isKrasviewAccount) {
                    return ""
                }
                address + "hash=" + account.hash
            }
            AuthRequestConst.AUTH_TV -> if (!account.isTVAccount) {
                return null
            } else if (account.isSocialNetworkAccount) {
                address + "hash=" + account.tvHash
            } else {
                try {
                    (address
                            + "login=" + URLEncoder.encode(
                        account.login,
                        StandardCharsets.UTF_8.toString()
                    )
                            + "&password=" + URLEncoder.encode(
                        account.password,
                        StandardCharsets.UTF_8.toString()
                    ))
                } catch (e: UnsupportedEncodingException) {
                    return null
                }
            }
            else -> throw IllegalStateException("Unexpected value: $request_auth_type")
        }
        var result = getXML(auth_address)
        if (result == "wrong hash") {
            if (account.isSocialNetworkAccount) {
                exitFromApplication()
                return ""
            }
            try {
                val hash = getXML(
                    ApiConst.KRASVIEW_AUTH, "login="
                            + URLEncoder.encode(account.login, StandardCharsets.UTF_8.toString())
                            + "&password=" + URLEncoder.encode(
                        account.password,
                        StandardCharsets.UTF_8.toString()
                    )
                )
                account.hash = hash
                if (account.hash == "error") {
                    exitFromApplication()
                    return ""
                }
                val prefs = PreferenceManager
                    .getDefaultSharedPreferences(mContext)
                prefs.edit().putString("pref_hash", account.hash).commit()
                auth_address = address + "hash=" + account.hash
                result = getXML(auth_address)
            } catch (e: UnsupportedEncodingException) {
                return null
            }
        }
        return result
    }

    private fun exitFromApplication() {
        if (exitListener != null) {
            exitListener!!.onError()
        }
    }
}