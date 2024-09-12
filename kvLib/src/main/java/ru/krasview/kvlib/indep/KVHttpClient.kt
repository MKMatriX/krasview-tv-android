package ru.krasview.kvlib.indep

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.krasview.kvlib.interfaces.OnLoadCompleteListener
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

open class KVHttpClient {
    var httpClient = OkHttpClient.Builder().ignoreAllSSLErrors().build()

    @Throws(IOException::class)
    fun run(url: String): String {
        val request: Request = Request.Builder()
            .url(url)
            .header("User-Agent", "krasview 2.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.body!!.string()
        }
    }

    private fun OkHttpClient.Builder.ignoreAllSSLErrors(): OkHttpClient.Builder {
        val naiveTrustManager = @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        }

        val insecureSocketFactory = SSLContext.getInstance("TLSv1.2").apply {
            val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
            init(null, trustAllCerts, SecureRandom())
        }.socketFactory

        sslSocketFactory(insecureSocketFactory, naiveTrustManager)
        hostnameVerifier { _, _ -> true }
        return this
    }

    private class GetXMLAsyncTask : AsyncTask<Any?, Any?, String>() {
        var listener1: OnLoadCompleteListener? = null
        var address: String? = null
        override fun doInBackground(vararg params: Any?): String {
            listener1 = params[2] as OnLoadCompleteListener
            address = params[0] as String
            return getXML(address, params[1] as String)
        }

        override fun onPostExecute(result: String) {
            listener1!!.loadComplete(address, result)
            listener1!!.loadComplete(result)
        }
    }

    companion object {
        fun getXML(address: String?, params: String?): String {
            return getXML(addParams(address, params))
        }

        @JvmStatic
		fun getXML(address: String): String {
            var line = ""
            val client = KVHttpClient()
            line = try {
/*		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpGet httpGet = new HttpGet(address);
		httpGet.setHeader("User-Agent", "krasview 2.0");
		HttpResponse httpResponse = httpClient.execute(httpGet);
		HttpEntity httpEntity = httpResponse.getEntity();
		if(httpEntity != null){
			line = EntityUtils.toString(httpEntity, "UTF-8");
		}*/
                client.run(address)
            } catch (e: UnsupportedEncodingException) {
                "<results status=\"error\"><msg>Can't connect to server</msg></results>"
            } catch (e: MalformedURLException) {
                "<results status=\"error\"><msg>Can't connect to server</msg></results>"
            } catch (e: IOException) {
                "<results status=\"error\"><msg>Can't connect to server</msg></results>"
            }
            return line
        }

        @JvmStatic
		protected fun addParams(address: String?, params: String?): String {
            var newAddress = address
            if (newAddress == null || Uri.parse(newAddress) == null) {
                return ""
            }
            newAddress =
                if (Uri.parse(newAddress) != null && Uri.parse(newAddress).query == null) {
                    "$newAddress?"
                } else {
                    "$newAddress&"
                }
            if (params != null && params.length != 0) {
                newAddress = "$newAddress$params&"
            }
            return newAddress
        }

        @JvmStatic
		fun getXMLAsync(address: String?, params: String?, listener: OnLoadCompleteListener?) {
            val task = GetXMLAsyncTask()
            task.execute(address, params, listener)
        }

        @JvmStatic
		fun getImage(adress: String?): Bitmap {
            var url: URL? = null
            val bmp: Bitmap
            try {
                url = URL(adress)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
            var conn: HttpURLConnection? = null
            try {
                conn = url!!.openConnection() as HttpURLConnection
            } catch (e: IOException) {
                e.printStackTrace()
            }
            conn!!.doInput = true
            try {
                conn.connect()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            var `is`: InputStream? = null
            try {
                `is` = conn.inputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }
            bmp = BitmapFactory.decodeStream(`is`)
            return bmp
        }

        fun getXMLFromFile(addres: String?, context: Context): String? {
            var xmlString: String? = null
            val am = context.assets
            try {
                val `is` = am.open(addres!!)
                val length = `is`.available()
                val data = ByteArray(length)
                `is`.read(data)
                xmlString = String(data)
            } catch (e1: IOException) {
                e1.printStackTrace()
            }
            return xmlString
        }
    }
}