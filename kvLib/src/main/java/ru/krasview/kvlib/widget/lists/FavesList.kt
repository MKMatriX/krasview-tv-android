package ru.krasview.kvlib.widget.lists

import android.annotation.SuppressLint
import android.content.Context
import ru.krasview.kvlib.indep.consts.AuthRequestConst
import ru.krasview.kvsecret.secret.ApiConst

@SuppressLint("ViewConstructor")
class FavesList(context: Context?, map: Map<String?, Any?>) : AllShowList(context, map ?: emptyMap()) {
    override var type: String? = ""

    init {
        type = map["section"] as String?
    }

    override val authRequest: Int
        get() = AuthRequestConst.AUTH_KRASVIEW
    override val apiAddress: String
        get() = ApiConst.BASE + type + "/faves"

    override fun setConstData() {}
}