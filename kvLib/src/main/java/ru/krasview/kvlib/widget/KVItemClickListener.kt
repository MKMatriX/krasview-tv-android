package ru.krasview.kvlib.widget

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.SimpleAdapter
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.indep.ListAccount
import ru.krasview.kvlib.indep.consts.IntentConst
import ru.krasview.kvlib.indep.consts.RequestConst
import ru.krasview.kvlib.indep.consts.TagConsts
import ru.krasview.kvlib.indep.consts.TypeConsts

class KVItemClickListener internal constructor(private val mList: List) : OnItemClickListener {
    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        itemClick(parent, view, position, id)
    }

    protected fun itemClick(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        val m = parent.getItemAtPosition(position) as Map<String, Any?>
        val type = m[TagConsts.TYPE] as String? ?: return
        if (type != null && type == TypeConsts.BILLING) {
            val intent = Intent()
            intent.action = IntentConst.ACTION_BILLING
            (mList.context as Activity).startActivityForResult(
                intent,
                RequestConst.REQUEST_CODE_BILLING
            )
            return
        } else if (type != null && (type == TypeConsts.VIDEO || type == TypeConsts.CHANNEL) || type == TypeConsts.TV_RECORD_VIDEO) {
            val intent: Intent
            if (playInSystemChoice(type)) {
                intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(m["uri"] as String?), "video/*")
                (mList.context as Activity).startActivity(intent)
            } else {
                intent = Intent()
                intent.action = IntentConst.ACTION_VIDEO_LIST
                intent.putExtra(
                    "index",
                    position - (parent.adapter as CombineSimpleAdapter).constDataCount
                )
                /*boolean rt = false;
				if(m.get("request_time") != null && (Boolean)m.get("request_time") == true){
					rt = true;
				}*/intent.putExtra("request_time", true)
                ListAccount.adapterForActivity =
                    SimpleAdapter(mList.context, mList.adapter?.data, 0, null, null)
                ListAccount.currentList = mList
                (mList.context as Activity).startActivityForResult(
                    intent,
                    RequestConst.REQUEST_CODE_VIDEO
                )
            }
            return
        }
        if (mList.factory != null && mList.viewProposeListener != null) {
            mList.viewProposeListener.onViewProposed(
                parent,
                mList.factory?.getView(m, mList.context)
            )
        }
    }

    private fun playInSystemChoice(t: String): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(mList.context)
        val player: String? = when (t) {
            TypeConsts.CHANNEL, TypeConsts.TV_RECORD_VIDEO -> {
                prefs.getString("video_player_tv", "std")
            }
            TypeConsts.VIDEO -> {
                prefs.getString("video_player_serial", "std")
            }
            else -> {
                return false
            }
        }
        return player == "system"
    }
}