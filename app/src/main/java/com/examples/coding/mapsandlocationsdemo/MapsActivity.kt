package com.examples.coding.mapsandlocationsdemo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*



const val ZOOM_INC = .5f
const val MY_PERMISSIONS_REQUEST_FINE_LOCALE = 42
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var mapFlag = 0
    private lateinit var locationProvider : FusedLocationProviderClient


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        requestPermissions()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    fun onClick(view: View) {
        when(view.id) {
            R.id.btnGotoLatLng -> {
                val lat = etLat.text.toString().toDouble()
                val lng = etLng.text.toString().toDouble()
                val latlng = LatLng(lat,lng)
                moveFocusToLatLng(latlng, locateByReverseGeocoding(latlng))
            }
            R.id.btnZoomIn -> {
                val currentZoom = mMap.cameraPosition.zoom
                mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom + ZOOM_INC))
            }
            R.id.btnZoomOut -> {
                val currentZoom = mMap.cameraPosition.zoom
                mMap.moveCamera(CameraUpdateFactory.zoomTo(currentZoom - ZOOM_INC))
            }
            R.id.btnGeoCodingSearch ->
                locateByGeocoding(etSearchByLocation.text.toString())
            R.id.btnChangeMapTypes -> {
                when(mapFlag) {
                    0 -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                    1 -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                    2 -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    3 -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                    4 -> mMap.mapType = GoogleMap.MAP_TYPE_NONE
                }
                if(mapFlag == 4) {
                    mapFlag = 0
                } else {
                    mapFlag++
                }
            }
            R.id.btnMoveToLastKnownLocation -> getLastKnownLocation()
        }
    }


    private fun moveFocusToLatLng(latLng : LatLng, title : String) {
        mMap.addMarker(MarkerOptions().position(latLng).title(title))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    fun locateByGeocoding(locationToSearch : String) {
        val geocoder = Geocoder(this)
        val locationResults
                = geocoder.getFromLocationName(locationToSearch, 1)
        val lat = locationResults[0].latitude
        val lng = locationResults[0].longitude
        val title = locationResults[0].countryName
        moveFocusToLatLng(LatLng(lat, lng), title)
    }

    private fun locateByReverseGeocoding(latLng: LatLng) :  String {
        val geocoder = Geocoder(this)
        val results
                = geocoder.getFromLocation(
                                     latLng.latitude,
                                     latLng.longitude,
                                    1)
        return results[0].getAddressLine(0).toString()
    }

    fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCALE)
            }
        } else {
            Toast
                .makeText(this,
                    "FINE PERMISSION WAS GRANTED",
                    Toast.LENGTH_LONG).show()
            initLocationFunctionality()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCALE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast
                        .makeText(this,
                            "FINE PERMISSION WAS GRANTED",
                            Toast.LENGTH_LONG).show()
                    initLocationFunctionality()
                } else {
                    Toast
                        .makeText(this,
                            "FINE PERMISSION WAS NOT GRANTED",
                            Toast.LENGTH_LONG).show()
                    btnMoveToLastKnownLocation.visibility = View.GONE
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    fun initLocationFunctionality() {
        locationProvider = FusedLocationProviderClient(this)
    }

    fun getLastKnownLocation() {
        updateLocation()
        locationProvider.lastLocation.addOnSuccessListener { location ->
            val lat = location.latitude
            val lng = location.longitude
            val latlng = LatLng(lat, lng)
            moveFocusToLatLng(latlng, "Last Known Location")
        }
    }

    fun updateLocation() {
        val locationRequest = LocationRequest()
        locationRequest.setMaxWaitTime(1)
        locationRequest.setInterval(1)
        locationRequest.setNumUpdates(100)

        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingRequestClient = SettingsClient(this)
        settingRequestClient.checkLocationSettings(locationSettingsRequest)
        locationProvider.requestLocationUpdates(locationRequest,
            object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.locations[0]
                val lat = location.getLatitude()
                val lng = location.getLongitude()
                Log.d("TAG_UPDATE", "onLocationResult: $lat $lng")
            }
        }, Looper.myLooper())
    }
}
