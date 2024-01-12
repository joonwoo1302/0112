package com.example.key2

import android.content.ContentValues
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.key2.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted = false
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //construck a PlaceClient
        Places.initializeWithNewPlacesApiEnabled(
            applicationContext,"key"
        )

        //construct a FusedLocationProviderClient.
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //주변 음식점 표시
        val findRestrtButton = findViewById<Button>(R.id.findRestrtButton)
        findRestrtButton.setOnClickListener {
            showCurrentPlace(restaurantTypes)
        }
        //주변호텔표시버튼
        val findHotelButton = findViewById<Button>(R.id.findHotelButton)
        findHotelButton.setOnClickListener {
            showCurrentPlace(hotelTypes)
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getLocationPermission()
        getDeviceLocation()
        updateLocationUI()
    }


    //Location허가요청
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this.applicationContext,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }


    //현재 위치 마커표시
    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        //현재위치를 찾은경우 위치정보를 사용합니다.
                        val lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation.latitude,
                                        lastKnownLocation.longitude
                                    ), 18f
                                )
                            )
                        }
                    } else {
                        //현재위치를 찾지못한경우 기본위치를 사용하고 예외 로그를 출력합니다.
                        Log.e(ContentValues.TAG, "Exception: %s", task.exception)
                        mMap.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(LatLng(33.362361, 126.533277), 18f)
                        )
                        mMap.uiSettings.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
            } else {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }


    //주변 검색 대상 건정
    private val restaurantTypes =
        listOf(Place.Type.CAFE, Place.Type.BAKERY, Place.Type.RESTAURANT, Place.Type.MEAL_TAKEAWAY)
    private val hotelTypes = listOf(Place.Type.LODGING)
    private val atmType = listOf(Place.Type.ATM, Place.Type.BANK)

    private fun showCurrentPlace(types: List<Place.Type>) {
        if (mMap == null || !locationPermissionGranted) {
            return
        }

        //기존 마커 삭제
        mMap.clear()

        val placesClient = Places.createClient(this)
        val request = FindCurrentPlaceRequest.newInstance(
            listOf(
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
            )
        )

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                for (placeLikelihood in response?.placeLikelihoods ?: emptyList()) {
                    val place = placeLikelihood.place
                    if (place.types.any { it in types }) {
                        //원하는 유형의 장소에 대한 처리
                        mMap.addMarker(
                            MarkerOptions()
                                .title(place.name)
                                .position(place.latLng)
                        )
                    }
                }
            } else {
                Log.e(ContentValues.TAG, "Exception: %s", task.exception)
            }
        }
    }

}



