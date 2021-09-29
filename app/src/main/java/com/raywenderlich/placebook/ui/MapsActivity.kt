package com.raywenderlich.placebook.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.raywenderlich.placebook.databinding.ActivityMapsBinding

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.adapter.BookmarkInfoWindowAdapter
import com.raywenderlich.placebook.viewmodel.MapsViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupLocationClient()
        setupPlacesClient()
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
        map = googleMap
        setupMapListeners()
        createBookmarkMarkerObserver()
        getCurrentLocation()
    }

    // PlacesClient - contains API key
    private fun setupPlacesClient() {
        Places.initialize(applicationContext,
            getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    // Location services client
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Add run-time permissions  - Pass current activity as context, array of requested
    // permissions, and requestCode to identify this request
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }

    // Get the current location
    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        } else {
            // Single call to display blue dot, controls to move to keep up, and allow to recenter map
            map.isMyLocationEnabled = true

            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    val latLng = LatLng(location.latitude,
                        location.longitude)

                    val update = CameraUpdateFactory.newLatLngZoom(latLng,
                        16.0f)

                    map.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    // Callback to handle user response to permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] ==
                PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
    }

    // Fetch request to get details about a place
    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
    }

    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        val placeId = pointOfInterest.placeId

        // keeps data & network usage under control to only retrieve certain attributes
        val placeFields = listOf(Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        val request = FetchPlaceRequest
            .builder(placeId, placeFields)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                displayPoiGetPhotoStep(place)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                }
            }
    }

    // Fetching a place photo - restrict height and width to save memory and bandwidth
    private fun displayPoiGetPhotoStep(place: Place) {
        val photoMetadata = place
            .getPhotoMetadatas()?.get(0)
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }
        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxWidth(resources.getDimensionPixelSize(R.dimen.default_image_width))
            .setMaxHeight(resources.getDimensionPixelSize(R.dimen.default_image_height))
            .build()
        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap = fetchPhotoResponse.bitmap
                displayPoiDisplayStep(place, bitmap)
            }.addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG,
                        "Place not found: " +
                                exception.message + ", " +
                                "statusCode: " + statusCode)
                }
            }
    }

    // Add a place marker
    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        val marker = map.addMarker(MarkerOptions()
            .position(place.latLng as LatLng)
            .title(place.name)
            .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)
    }

    // Map listeners
    private fun setupMapListeners() {
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        map.setOnPoiClickListener {
            displayPoi(it)
        }

        // Listener to call handleInfoWindow() whenever the user taps an info window,
        // which calls MapsViewModel.addBookmarkFromPlace() and adds a bookmark to the database
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
    }

    // Handle taps on a place info window
    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(
                    placeInfo.place,
                    placeInfo.image
                )
            }
        }
        marker.remove()
    }

    // Add a blue marker to the map based on a BookmarkMarkerView
    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkMarkerView): Marker? {
        val marker = map.addMarker(MarkerOptions()
            .position(bookmark.location)
            .icon(
                BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE))
            .alpha(0.8f))
        marker.tag = bookmark
        return marker
    }

    // Display all bookmark markers - walk through list of objects and add place marker for each
    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookmarkMarkerView>) {
        bookmarks.forEach { addPlaceMarker(it) }
    }

    // Observe changes to BookmarkMarkerView objects from MapsViewModel and update View on change
    private fun createBookmarkMarkerObserver() {
        mapsViewModel.getBookmarkMarkerViews()?.observe(
            this, {
                map.clear()
                it?.let {
                    displayAllBookmarks(it)
                }
            })
    }

    // Set the Place object and the Bitmap image as the Marker tag
    class PlaceInfo(val place: Place? = null,
                    val image: Bitmap? = null)
}