package com.example.mapwithroute

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.AsyncTask
import android.util.Log
import android.widget.Button
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.android.gms.maps.model.Marker
import java.net.URL


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var originLatitude: Double = 41.07284
    private var originLongitude: Double = 28.67701
    private var destinationLatitude: Double = 41.019808889344866
    private var destinationLongitude: Double = 28.88945525757646
    private lateinit var originLocation: LatLng
    private lateinit var destinationLocation: LatLng
    var id = "m"
    var idNum1 = 1
    var idNum2 = 2
    private lateinit var apiKey: String
    private lateinit var url: String
    private lateinit var mapFragment: SupportMapFragment
    var mText = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val ai: ApplicationInfo = applicationContext.packageManager
            .getApplicationInfo(applicationContext.packageName, PackageManager.GET_META_DATA)
        val value = ai.metaData["com.google.android.geo.API_KEY"]
        apiKey = value.toString()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        originLocation = LatLng(originLatitude, originLongitude)
        destinationLocation = LatLng(destinationLatitude, destinationLongitude)

        setUpUI(originLocation, destinationLocation)

    }

    fun setUpUI(origin: LatLng, dest: LatLng) {
        mapFragment.getMapAsync {
            googleMap = it
            googleMap!!.addMarker(MarkerOptions().position(origin).draggable(true))
            googleMap!!.addMarker(MarkerOptions().position(dest).draggable(true))
        }
        val gd = findViewById<Button>(R.id.directions)
        gd.setOnClickListener {
            mapFragment.getMapAsync {
                url = getDirectionURL(origin, dest, apiKey)
                GetDirection(url).execute()
                googleMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, 14F))
            }
        }
        mapFragment.getMapAsync {
            googleMap!!.setOnMarkerDragListener(object : OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {
                    if (marker.id == id + idNum1.toString()) {
                    mText = true
                        idNum1 += 2
                        idNum2 += 2
                }
                else if (marker.id == id + idNum2.toString()) {
                    mText = false
                        idNum1 += 2
                        idNum2 += 2
                    }
            }
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                googleMap!!.clear()
                if (mText) {
                    setUpUI(marker.position, destinationLocation)
                } else {
                    setUpUI(originLocation, marker.position)
                }

            }
        })
    }
    }

    override fun onMapReady(p0: GoogleMap) {
        googleMap = p0
        googleMap!!.clear()
        googleMap!!.addMarker(MarkerOptions().position(LatLng(originLatitude, originLongitude)))
        googleMap!!.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    originLatitude,
                    originLongitude
                ), 18F
            )
        )

    }

    private fun getDirectionURL(origin: LatLng, dest: LatLng, secret: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url: String) :
        AsyncTask<Void, Void, List<List<LatLng>>>() {
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, MapData::class.java)
                val path = ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size - 1) {
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices) {
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.GREEN)
                lineoption.geodesic(true)
            }
            googleMap!!.addPolyline(lineoption)
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }
}
