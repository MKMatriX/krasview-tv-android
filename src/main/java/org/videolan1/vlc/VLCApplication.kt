/*****************************************************************************
 * VLCApplication.java
 *
 * Copyright © 2010-2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan1.vlc

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.preference.PreferenceManager
import android.util.Log
import androidx.multidex.MultiDexApplication
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator
import com.nostra13.universalimageloader.core.ImageLoader
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration
import com.nostra13.universalimageloader.core.assist.QueueProcessingType
import org.videolan1.vlc.gui.audio.AudioUtil
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class VLCApplication : MultiDexApplication() {
    override fun onCreate() {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().detectAll().penaltyDialog().build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyDeath().build());
		}*/
        super.onCreate()
        initImageLoader(applicationContext)
        trustSSL()

        // Are we using advanced debugging - locale?
        val pref = PreferenceManager.getDefaultSharedPreferences(this)
        var p = pref.getString("set_locale", "")
        if (p != null && p != "") {
            val locale: Locale
            // workaround due to region code
            if (p == "zh-TW") {
                locale = Locale.TRADITIONAL_CHINESE
            } else if (p.startsWith("zh")) {
                locale = Locale.CHINA
            } else if (p == "pt-BR") {
                locale = Locale("pt", "BR")
            } else if (p == "bn-IN" || p.startsWith("bn")) {
                locale = Locale("bn", "IN")
            } else {
                /**
                 * Avoid a crash of
                 * java.lang.AssertionError: couldn't initialize LocaleData for locale
                 * if the user enters nonsensical region codes.
                 */
                if (p.contains("-")) p = p.substring(0, p.indexOf('-'))
                locale = Locale(p)
            }
            Locale.setDefault(locale)
            val config = Configuration()
            config.locale = locale
            baseContext.resources.updateConfiguration(
                config,
                baseContext.resources.displayMetrics
            )
        }
        instance = this

        // Initialize the database soon enough to avoid any race condition and crash
        MediaDatabase.getInstance(this)
        // Prepare cache folder constants
        AudioUtil.prepareCacheFolder(this)
    }

    private fun trustSSL() {
        //Create a trust manager that does not validate certificate chains
        val trustAllCerts: Array<TrustManager> = arrayOf(
            @SuppressLint("CustomX509TrustManager")
            object : X509TrustManager {
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                @SuppressLint("TrustAllX509TrustManager")
                override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
            }
        )

        //Install the all-trusting trust manager
        try {
            val sc = SSLContext.getInstance("TLS")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
        } catch (e: KeyManagementException) {
            e.printStackTrace()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
    }
    /**
     * Called when the overall system is running low on memory
     */
    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "System is running low on memory")
        BitmapCache.getInstance().clear()
    }

    companion object {
        const val TAG = "VLC/VLCApplication"
        private var instance: VLCApplication? = null

        // public static Adapter adapterForActivity;
        const val SLEEP_INTENT = "org.videolan.vlc.SleepIntent"
        fun initImageLoader(context: Context?) {
            // This configuration tuning is custom. You can tune every option, you may tune some of them,
            // or you can create default configuration by
            //  ImageLoaderConfiguration.createDefault(this);
            // method.
            val config = ImageLoaderConfiguration.Builder(context)
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                .discCacheFileNameGenerator(Md5FileNameGenerator())
                .tasksProcessingOrder(QueueProcessingType.LIFO)
                .writeDebugLogs() // Remove for release app
                .build()
            // Initialize ImageLoader with configuration.
            ImageLoader.getInstance().init(config)
        }

        /**
         * @return the main context of the Application
         */
        @JvmStatic
        val appContext: Context?
            get() = instance

        /**
         * @return the main resources from the Application
         */
        @JvmStatic
        val appResources: Resources?
            get() = if (instance == null) null else instance!!.resources
    }
}