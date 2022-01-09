package com.example.datcproject

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.graphics.Color
import android.location.*
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.CarrierConfigManager
import android.view.View
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.datcproject.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.example.datcproject.PermissionUtils
import com.example.datcproject.PermissionUtils.PermissionDeniedDialog.Companion.newInstance
import com.example.datcproject.PermissionUtils.isPermissionGranted
import com.example.datcproject.PermissionUtils.requestPermission
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Marker

import com.google.android.gms.maps.model.LatLng
import java.io.IOException


class MapsActivity : AppCompatActivity(), GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, OnMapReadyCallback,
    ActivityCompat.OnRequestPermissionsResultCallback {

    private var permissionDenied = false
    private val buttonDraw: Button? = null
    private lateinit var map: GoogleMap
    var polygon: Polygon? = null
    var polygonsList: List<Polygon> = ArrayList()
    var latLngList: List<LatLng> = ArrayList()
    var markerList: List<Marker?> = ArrayList()
    val boolToDraw = true;
    val color = Color.CYAN

//    val btClear = findViewById<View>(R.id.btClear)


//    val primaryColor = #ffebee;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        Log.d("--PAB--", "onCreate")
//        val buttonDraw = findViewById<Button>(R.id.buttonDraw)
//        buttonDraw!!.setOnClickListener{
//            Log.d("--PAB--","buttonDrawClick")
//                if (polygon != null)
//                    polygon!!.remove()
//                val polygonOptions = PolygonOptions().addAll(latLngList)
//                    .clickable(true)
//                polygon = map.addPolygon(polygonOptions)
//                polygon!!.strokeColor = Color.alpha(0Xffebee)
//                if (boolToDraw)
//                    run { polygon!!.fillColor = Color.alpha(0Xffebee) }
//
//        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show()
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        // [START maps_check_location_permission]
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        } else {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(
                this, LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )
        }
        // [END maps_check_location_permission]
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (isPermissionGranted(
                permissions,
                grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
            // [END_EXCLUDE]
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError()
            permissionDenied = false
        }
    }

    private fun showMissingPermissionError() {
        newInstance(true).show(supportFragmentManager, "dialog")
    }

    fun searchLocation(view: View) {
        val locationSearch: EditText = findViewById<EditText>(R.id.editText)
        lateinit var location: String
        location = locationSearch.text.toString()
        var addressList: List<Address>? = null

        if (location == "") {
            Toast.makeText(applicationContext, "provide location", Toast.LENGTH_SHORT).show()
        } else {
            val geoCoder = Geocoder(this)
            try {
                addressList = geoCoder.getFromLocationName(location, 1)

            } catch (e: IOException) {
                e.printStackTrace()
            }
            val address = addressList!![0]
            val latLng = LatLng(address.latitude, address.longitude)
            map.addMarker(MarkerOptions().position(latLng).title(location))
            map.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            Toast.makeText(
                applicationContext,
                address.latitude.toString() + " " + address.longitude,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMapClickListener { latLng ->
            val markerOptions = MarkerOptions().position(latLng)
            val marker = map.addMarker(markerOptions)
            latLngList = latLngList + (latLng)
            markerList = markerList + (marker)
        }

        val buttonDraw = findViewById<Button>(R.id.buttonDraw)
        buttonDraw.setOnClickListener {
            Log.d("--PAB--", "buttonDrawClick")
            addHardCodedAreas()
            if (polygon != null) {
                Log.d("--PAB--", "first IF")
            }
            val polygonOptions = PolygonOptions().addAll(latLngList)
            for (latLng in latLngList) {
                Log.d("debug", latLng.toString())
            }
            polygon = map.addPolygon(polygonOptions)
            polygonsList = polygonsList + (polygon!!)
            for(polygon1 in polygonsList)
                polygon1.apply {
                    Log.d("debug", "polygon1 interior")
                    strokeColor = Color.argb(50, 0, 0, 0)
                    Log.d("debug", strokeColor.toString())
                    Log.d("debug", fillColor.toString())
                    fillColor = Color.argb(30, 50, 255, 0)
                }
        }

        val buttonClear = findViewById<Button>(R.id.buttonClear)
        buttonClear.setOnClickListener {
            Log.d("--PAB--", "buttonDrawClick")
            for (marker in markerList) marker!!.remove()
            latLngList = ArrayList()
            markerList = ArrayList()
        }

        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)
        enableMyLocation()
        addHardCodedAreas()
    }

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

 fun addHardCodedAreas()
{
    //Parcul Civic
    Log.d("--PAB--", "HardCoded")
    val polygonHC = map.addPolygon(PolygonOptions()
        .add(
            LatLng(21.2451553, 45.7809329),
            LatLng(21.2631798, 45.8055294),
            LatLng(21.2886715, 45.7960152),
            LatLng(21.2755394, 45.7638107),
            LatLng(21.2451553, 45.7809329),
            LatLng(21.2447691, 45.7809479))
        .fillColor(Color.argb(30, 50, 255, 0))
        .strokeColor(Color.argb(30, 50, 255, 0)))
//        LatLng(21.2288958, 45.7535708),
//        LatLng(21.2291586, 45.7542296),
//        LatLng(21.2293303, 45.7547311),
//        LatLng(21.2294644, 45.7548883),
//        LatLng(21.2298506, 45.7550118),
//        LatLng(21.2310845, 45.7550006),
//        LatLng(21.2329245, 45.7546039),
//        LatLng(21.2327206, 45.7541248),
//        LatLng(21.2323612, 45.7536681),
//        LatLng(21.2318730, 45.7534361),
//        LatLng(21.2313098, 45.7533387),
//        LatLng(21.2300009, 45.7533799),
//        LatLng(21.2288958, 45.7535708)
    polygonsList = polygonsList + (polygonHC)
//    polygonHC.apply {
//        strokeColor = Color.BLACK
//        Log.d("debug", strokeColor.toString())
//        fillColor = Color.GREEN
//        Log.d("debug", fillColor.toString())
//    }

    //Botanic
    map.addPolygon(PolygonOptions().add(
        LatLng(21.2262350, 45.7622165),
        LatLng(21.2236279, 45.7613557),
        LatLng(21.2219167, 45.7602067),
        LatLng(21.2222975, 45.7601169),
        LatLng(21.2227482, 45.7598699),
        LatLng(21.2237513, 45.7593422),
        LatLng(21.2246096, 45.7588557),
        LatLng(21.2253714, 45.7583504),
        LatLng(21.2262994, 45.7588856),
        LatLng(21.2257898, 45.7593534),
        LatLng(21.2262297, 45.7596154),
        LatLng(21.2261599, 45.7597052),
        LatLng(21.2267715, 45.7600308),
        LatLng(21.2273347, 45.7595181),
        LatLng(21.2275815, 45.7598138),
        LatLng(21.2270665, 45.7606409),
        LatLng(21.2262297, 45.7622239)
    ))

    //Padurea Verde
    map.addPolygon(PolygonOptions().add(
        LatLng(21.2633300, 45.8056341),
        LatLng(21.2891865, 45.7961948),
        LatLng(21.2761188, 45.7777004),
        LatLng(21.2806463, 45.7762937),
        LatLng(21.2841868, 45.7744379),
        LatLng(21.2799168, 45.7733752),
        LatLng(21.2808180, 45.7720881),
        LatLng(21.2749600, 45.7710554),
        LatLng(21.2755609, 45.7640502),
        LatLng(21.2747884, 45.7637209),
        LatLng(21.2708616, 45.7638556),
        LatLng(21.2696600, 45.7644095),
        LatLng(21.2692094, 45.7648735),
        LatLng(21.2694669, 45.7653825),
        LatLng(21.2699819, 45.7655172),
        LatLng(21.2705827, 45.7655321),
        LatLng(21.2713766, 45.7653076),
        LatLng(21.2720847, 45.7656968),
        LatLng(21.2723637, 45.7661159),
        LatLng(21.2724710, 45.7664901),
        LatLng(21.2721705, 45.7668793),
        LatLng(21.2719131, 45.7674631),
        LatLng(21.2715483, 45.7677175),
        LatLng(21.2711406, 45.7678972),
        LatLng(21.2702823, 45.7679421),
        LatLng(21.2704754, 45.7686306),
        LatLng(21.2698960, 45.7689300),
        LatLng(21.2689090, 45.7687503),
        LatLng(21.2688017, 45.7697382),
        LatLng(21.2665915, 45.7691994),
        LatLng(21.2662268, 45.7694089),
        LatLng(21.2654543, 45.7694538),
        LatLng(21.2645102, 45.7694688),
        LatLng(21.2639093, 45.7693191),
        LatLng(21.2632227, 45.7686007),
        LatLng(21.2628150, 45.7684211),
        LatLng(21.2617636, 45.7683761),
        LatLng(21.2614202, 45.7684809),
        LatLng(21.2606907, 45.7683761),
        LatLng(21.2604761, 45.7720432),
        LatLng(21.2652826, 45.7722976),
        LatLng(21.2659907, 45.7727017),
        LatLng(21.2661409, 45.7738242),
        LatLng(21.2655401, 45.7758148),
        LatLng(21.2604976, 45.7808880),
        LatLng(21.2595534, 45.7804690),
        LatLng(21.2579656, 45.7815913),
        LatLng(21.2535238, 45.7795112),
        LatLng(21.2523866, 45.7780596),
        LatLng(21.2447906, 45.7808730),
        LatLng(21.2629008, 45.8055294)
    ))

    //Lac Dumbravita
    map.addPolygon(PolygonOptions().add(
        LatLng(21.2689090, 45.8078030),
        LatLng(21.2744451, 45.8056042),
        LatLng(21.2761402, 45.8082966),
        LatLng(21.2802601, 45.8081619),
        LatLng(21.2822771, 45.8108542),
        LatLng(21.2820411, 45.8122152),
        LatLng(21.2805176, 45.8120208),
        LatLng(21.2756038, 45.8098371),
        LatLng(21.2740159, 45.8091342),
        LatLng(21.2734151, 45.8081021),
        LatLng(21.2704968, 45.8084611),
        LatLng(21.2689304, 45.8079376)
    ))

    //Zona Ronat
    map.addPolygon(PolygonOptions().add(
        LatLng(21.1715233, 45.7828783),
        LatLng(21.1767483, 45.7805139),
        LatLng(21.1809969, 45.7846739),
        LatLng(21.1860180, 45.7818906),
        LatLng(21.1978197, 45.7877264),
        LatLng(21.2018108, 45.7844645),
        LatLng(21.1944723, 45.7804540),
        LatLng(21.1944723, 45.7798854),
        LatLng(21.1901379, 45.7766828),
        LatLng(21.1882925, 45.7763535),
        LatLng(21.1941290, 45.7726718),
        LatLng(21.1925411, 45.7703369),
        LatLng(21.1947298, 45.7696484),
        LatLng(21.1933994, 45.7680918),
        LatLng(21.1922407, 45.7678822),
        LatLng(21.1913824, 45.7668943),
        LatLng(21.1906099, 45.7658465),
        LatLng(21.1825418, 45.7643197),
        LatLng(21.1814260, 45.7667446),
        LatLng(21.1781216, 45.7657866),
        LatLng(21.1715233, 45.7828764),
        LatLng(21.1794090, 45.7775807)
    ))

    //Timisoara Vest
    map.addPolygon(PolygonOptions().add(
        LatLng(21.1656332, 45.7226600),
        LatLng(21.1706114, 45.7259331),
        LatLng(21.1731219, 45.7275509),
        LatLng(21.1758417, 45.7291199),
        LatLng(21.1755922, 45.7297265),
        LatLng(21.1768878, 45.7307282),
        LatLng(21.1764854, 45.7310334),
        LatLng(21.1767134, 45.7318328),
        LatLng(21.1746454, 45.7342199),
        LatLng(21.1764640, 45.7350942),
        LatLng(21.1780545, 45.7338249),
        LatLng(21.1808118, 45.7355453),
        LatLng(21.1793607, 45.7367266),
        LatLng(21.1785534, 45.7365844),
        LatLng(21.1586165, 45.7296704),
        LatLng(21.1590672, 45.7287717),
        LatLng(21.1628544, 45.7301122),
        LatLng(21.1639005, 45.7287567),
        LatLng(21.1655420, 45.7293109),
        LatLng(21.1661053, 45.7285620),
        LatLng(21.1690986, 45.7261953),
        LatLng(21.1661267, 45.7244988),
        LatLng(21.1656278, 45.7226638)
    ))
}
}