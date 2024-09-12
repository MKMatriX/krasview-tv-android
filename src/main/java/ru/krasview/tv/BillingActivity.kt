package ru.krasview.tv

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.FrameLayout
import android.widget.Toast
import com.example.kvlib.R
import ru.krasview.kvlib.adapter.CombineSimpleAdapter
import ru.krasview.kvlib.indep.AuthAccount
import ru.krasview.kvlib.indep.HTTPClient.getXML
import ru.krasview.kvlib.indep.HeaderAccount
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import ru.krasview.kvlib.widget.lists.PackageList
import ru.krasview.tv.billing.util.IabHelper
import ru.krasview.tv.billing.util.IabHelper.OnIabPurchaseFinishedListener
import ru.krasview.tv.billing.util.IabHelper.QueryInventoryFinishedListener
import ru.krasview.tv.billing.util.IabResult
import ru.krasview.tv.billing.util.Inventory
import ru.krasview.tv.billing.util.Purchase

class BillingActivity : Activity(), OnLoadCompleteListener, OnItemClickListener {
    var mHelper: IabHelper? = null
    var mList: PackageList? = null
    private fun setResultAndFinish() {
        HeaderAccount.hideHeader = true
        Toast.makeText(this, "Пакет подключен", Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        onBackPressed()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar!!.setBackgroundDrawable(resources.getDrawable(R.drawable.action_bar_background))
        setContentView(R.layout.activity_billing)
        val layout = findViewById<View>(R.id.layout) as FrameLayout
        mList = PackageList(this, null)
        mList!!.init()
        mList!!.onLoadCompleteListener = this
        mList!!.onItemClickListener = this
        layout.addView(mList)
        val base64EncodedPublicKey = ru.krasview.kvsecret.secret.Billing.base64EncodedPublicKey
        mHelper = IabHelper(this, base64EncodedPublicKey)
        mHelper!!.startSetup { result ->
            if (!result.isSuccess) {
                // Oh noes, there was a problem.
                Log.i("Debug", "Problem setting up In-app Billing: $result")
            }
            mList!!.refresh()
            Log.i("Debug", "Hooray, IAB is fully set up!")
        }
    }

    private var mQueryFinishedListener = QueryInventoryFinishedListener { result, inventory ->
        if (result.isFailure) {
            // Обработка ошибки
            Log.i("Debug", "Произошла ошибка")
            return@QueryInventoryFinishedListener
        }
        go(inventory)
    }

    private fun go(inventory: Inventory) {
        val adapter = mList?.adapter ?: return
        for (i in 0 until adapter.count) {
            val map = adapter.getItem(i) as? MutableMap<String, Any?> ?: emptyMap<String, Any?>().toMutableMap()
            val id = map["sku"] as String?
            if (inventory.hasDetails(id)) {
                var productType = inventory.getSkuDetails(id).type
                productType = if (productType == "subs") {
                    "подписка на месяц"
                } else if (productType == "inapp") {
                    "на месяц"
                } else {
                    ""
                }
                map["inMarket"] = true
                map["productType"] = productType
                map["price"] = inventory.getSkuDetails(id).price
                adapter.notifyDataSetChanged()
            } else {
                var productType = map["product"] as String?
                if (productType == null) {
                } else {
                    productType = if (productType == "subscription") {
                        "подписка на месяц"
                    } else if (productType == "portion") {
                        "на месяц"
                    } else {
                        ""
                    }
                    map["productType"] = productType
                }
                map["inMarket"] = false
                map["price"] = "Недоступно"
                adapter.notifyDataSetChanged()
            }
        }
    }

    internal inner class PurchaseFinishedListener(var m: Map<String?, Any?>) :
        OnIabPurchaseFinishedListener {
        override fun onIabPurchaseFinished(result: IabResult, purchase: Purchase) {
            //обрабатываем прошедшую выплату
            Log.i("Debug", " purchasing: $result")
            if (result.isFailure) {
                //если ошибка
                Log.i("Debug", "Error purchasing: $result")
                return
            }
            SendOnServerTask().execute(m)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mHelper != null) mHelper!!.dispose()
        mHelper = null
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun loadComplete(result: String) {
        val additionalSkuList: MutableList<String?> = ArrayList()
        val adapter = mList!!.adapter
        for (i in 0 until (adapter?.count ?: -2)) {
            val map = adapter?.getItem(i) as? Map<String, Any?> ?: emptyMap()
            //потестить
            additionalSkuList.add(map["sku"] as String?)
        }
        mHelper!!.queryInventoryAsync(
            true, additionalSkuList,
            mQueryFinishedListener
        )
    }

    override fun loadComplete(address: String, result: String) {}
    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val adapter = parent.adapter as CombineSimpleAdapter
        val m = adapter.getItem(position) as Map<String?, Any?>
        if (m["inMarket"] == null || !(m["inMarket"] as Boolean)) {
            return
        } else {
            //Запуск процесса покупки
            val mPurchaseFinishedListener: OnIabPurchaseFinishedListener =
                PurchaseFinishedListener(m)
            mHelper!!.launchPurchaseFlow(
                this@BillingActivity, m["sku"] as String?, 10001,
                mPurchaseFinishedListener, ru.krasview.kvsecret.secret.Billing.LAUNCH_PUNCHASE_FLOW_EXTRA
            )
        }
    }

    private inner class SendOnServerTask : AsyncTask<Map<String?, Any?>, Void?, String>() {
        override fun doInBackground(vararg maps: Map<String?, Any?>): String {
            val packet = maps[0]["id"] as? String ?: ""
            val hash = AuthAccount.getInstance().tvHash
            val address = ru.krasview.kvsecret.secret.ApiConst.SUBSCRIBE
            val secret = ru.krasview.kvsecret.secret.Billing.getSecret(hash, packet)
            val params = "packet=$packet&secret=$secret"
            return getXML(address, params, AuthAccount.AUTH_TYPE_KRASVIEW) ?: ""
        }

        override fun onPostExecute(result: String) {
            if (result == "ok") {
                setResultAndFinish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass on the activity result to the helper for handling
        // NOTE: handleActivityResult() will update the state of the helper,
        // allowing you to make further calls without having it exception on you
        if (mHelper!!.handleActivityResult(requestCode, resultCode, data)) {
            Log.d("Debug", "onActivityResult handled by IABUtil.")
            //handlePurchaseResult(requestCode, resultCode, data);
            return
        }
    }
}