@file:Suppress("UNREACHABLE_CODE")

package com.app.top10downloader

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_main.*
import learnprogramming.academy.R
import java.io.IOException
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.properties.Delegates



class FeedEntry {
    var name: String = ""
    var artist: String = ""
    var releaseDate: String = ""
    var summary: String = ""
    var imageURL: String = ""

    override fun toString(): String {
        return """
            name = $name
            artist = $artist
            releaseDate = $releaseDate
            imageURL = $imageURL
        """.trimIndent()
    }
}

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private var downloadData : DownloadData? = null

    private  var feedUrl : String = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    // %d this is the way of specifying an int value
    private  var feedLimit = 10


    private var feedCachedUrl = "INVALIDATED"
    private val STATE_URL = "feedUrl"
    private val STATE_LIMIT = "feedLimit"



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

          Log.d(TAG , "onCreate : called")

        if(savedInstanceState != null){

            feedUrl = savedInstanceState.getString(STATE_URL).toString()
            feedLimit =  savedInstanceState.getInt(STATE_LIMIT)
        }

         downloadUrl(feedUrl.format(feedLimit))
         Log.d(TAG , "onCreate : done")




    }

    private fun downloadUrl(feedUrl : String){
        // check if the existing url isn't being downloaded again and compare it to the new Url
        if(feedUrl != feedCachedUrl){
            Log.d(TAG, "downloadUrl :  starting AysncTask")
            // Async Task can't execute the programme more than once  , so we have to inti the downloadData = null
            // and tell to exc the programme from the beginning
            downloadData = DownloadData(this , xmlListView)
            downloadData?.execute(feedUrl)
            feedCachedUrl = feedUrl
            // please use the http instead of https
            Log.d(TAG, "downloadUrl: done")

        }
        else{
            Log.d(TAG , "downloadUrl - URL not changed")
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menus, menu)

        if(feedLimit == 10){
            menu?.findItem(R.id.mnu10)?.isChecked = true
        }
        else{
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Store the url in the val

        // when the menu item selected feed url will hold the address of the corresponding RSS feed
        when (item.itemId){
            R.id.mnuApple ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topMovies/xml"

            R.id.mnuFree ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml"
            R.id.mnuSongs ->
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10, R.id.mnu25 ->{
                if (!item.isChecked){
                    item.isChecked = true
                    feedLimit = 35 - feedLimit
                    Log.d(TAG , "onOptionItemSelected ${item.title} settings feedLimit to $feedLimit")

                }else {
                    Log.d(TAG , "onOptionItemSelected ${item.title} settings feedLimit unchanged")
                }
            }
            R.id.mnuRefersh -> feedCachedUrl = "INVALIDATED"
            else ->
                return super.onOptionsItemSelected(item)
        }
        // so we can pass that URL to a download URL fun to download the data
        downloadUrl(feedUrl.format(feedLimit))
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_URL , feedUrl)
        outState.putInt(STATE_LIMIT , feedLimit)
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadData?.cancel(true)

    }

    companion object {
        private class DownloadData(context: Context, listView: ListView) : AsyncTask<String, Void, String>() {
            private val TAG = "DownloadData"

            var propContext : Context by Delegates.notNull()
            var propListView : ListView by Delegates.notNull()

            init {
                propContext = context
                propListView = listView
            }

            override fun onPostExecute(result: String) {
                super.onPostExecute(result)
                val parseApplications = ParseApplications()
                parseApplications.parse(result)

                val feedAdapter = FeedAdapter(propContext , R.layout.list_record, parseApplications.applications)
                propListView.adapter = feedAdapter


            }

            override fun doInBackground(vararg url: String?): String {
                Log.d(TAG, "doInBackground: starts with ${url[0]}")
                val rssFeed = downloadXML(url[0])
                if (rssFeed.isEmpty()) {
                    Log.e(TAG, "doInBackground: Error downloading")
                }
                return rssFeed
            }

            private fun downloadXML(urlPath: String?): String {
  //              return URL(urlPath).readText()

                val xmlResult = StringBuilder()

                try{
                    val url = URL(urlPath)
                    val connection :HttpURLConnection = url.openConnection() as HttpURLConnection
                    val response = connection.responseCode
                    Log.d(TAG, "downloadXML: The response code was $response")

                    connection.inputStream.buffered().reader().use { xmlResult.append(it.readText())}

                    Log.d(TAG , "Received ${xmlResult.length} bytes")
                    return  xmlResult.toString()

                }catch (e : Exception){
                    val errorMessage : String = when (e){
                        is MalformedURLException -> "downloadXML: Invalid URL ${e.message}"
                        is IOException -> "downloadXML: IO Exception reading data: ${e.message}"
                        is SecurityException -> { e.printStackTrace()
                            "downloadXML: Security Exception. Needs permission? ${e.message}"
                        }
                        else -> "Unknown error: ${e.message}"
                    }
                }
                return ""  // If it gets to here, there's been a problem. Return an empty string
            }
        }
    }


}

