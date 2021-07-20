package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.synthetic.main.fragment_select_location.*
import org.koin.android.ext.android.inject
import android.provider.Settings
import com.udacity.project4.BuildConfig
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {
    private val TAG = "SelectLocationFragment"

    companion object {
        private const val DEFAULT_ZOOM = 15
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()

    private var lastKnownLocation: Location? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentSelectLocationBinding

    private var reminderPoi:PointOfInterest? = null

    private val REQUEST_LOCATION_PERMISSION = 1

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    getContext()!!,
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
            else {
                Log.e(TAG, "map style set successfully")
            }

        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun checkPermissionsAndStartGeofencing() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            Log.i("hunt", "permissions active")
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(getContext()!!,
                            android.Manifest.permission.ACCESS_FINE_LOCATION))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            getContext()!!, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @TargetApi(29 )
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(AuthenticationActivity.TAG, "Request foreground only location permission")
        requestPermissions(
            permissionsArray,
            resultCode
        )
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.i("signInFlow", "inside the map fragment")
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

        checkPermissionsAndStartGeofencing()

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
//        TODO: call this function after the user confirms on the selected location

        binding.savelocationBtn.setOnClickListener {
            onLocationSelected()
        }

        return binding.root
    }

    private fun setMapLongClick(map:GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )
            Log.i("mMap", "clicked on map")

            binding.savelocationBtn.setOnClickListener {
                Log.i("mMap", "clicked on button")
                _viewModel.latitude.value = latLng.latitude
                _viewModel.longitude.value = latLng.longitude
                _viewModel.reminderSelectedLocationStr.value = getString(R.string.dropped_pin)
                // _viewModel.navigationCommand.value = NavigationCommand.Back
                findNavController().navigateUp()
            }


            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(snippet)
                    .snippet(snippet)
            )

            Log.i("mMap", "marker added")
        }
    }

    private fun setPoiClick(map: GoogleMap) {
        map.setOnPoiClickListener { poi ->
            val poiMarker = map.addMarker(
                    MarkerOptions()
                            .position(poi.latLng)
                            .title(poi.name)
            )

            reminderPoi = poi
        }
    }

    private fun saveLocation() {
        Log.i("map", "Saving a POI")
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        setMapStyle(mMap)
        // Add a marker in Addis Ababa and move the camera

        val addis = LatLng(8.9973, 38.7868)
        val zoomLevel = 20f
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(addis, zoomLevel))
        mMap.addMarker(MarkerOptions()
            .title("Marker in Addis")
            .position(addis))

        setMapLongClick(mMap)
        setPoiClick(mMap)
        enableMyLocation()
    }

    private fun updateUI() {
        try {
            if (isPermissionGranted()) {
                mMap.uiSettings?.isMyLocationButtonEnabled = true
                mMap.uiSettings?.isMapToolbarEnabled = false
                mMap.isMyLocationEnabled = true
            } else {
                mMap.uiSettings?.isMyLocationButtonEnabled = false
                mMap.uiSettings?.isMapToolbarEnabled = false
                mMap.isMyLocationEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message, e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (isPermissionGranted()) {
            mMap.setMyLocationEnabled(true)
            updateUI()
            getDeviceLocation()
        }
        else {
            Snackbar.make(
                binding.selectLocation,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
            ActivityCompat.requestPermissions(
                    getActivity()!!,
                    arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun getDeviceLocation() {
        try {
            if (isPermissionGranted()) {
                val lastLocation = fusedLocationProviderClient.lastLocation

                lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {

                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            Log.i("last","last  known location $lastKnownLocation")

                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastKnownLocation!!.latitude,
                                        lastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        } else {
                            Log.i("last","Exception: %s", task.exception)
                            val addis = LatLng(8.9973, 38.7868)
                            mMap.moveCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(addis, DEFAULT_ZOOM.toFloat())
                            )
                            _viewModel.showErrorMessage.postValue(getString(R.string.err_select_location))
                            ActivityCompat.requestPermissions(
                                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                REQUEST_TURN_LOCATION_ON
                            )
                            mMap.uiSettings?.isMyLocationButtonEnabled = false
                        }
                    }
                }
                    .addOnFailureListener {
                        Log.i("failure", "failure")
                        getDeviceLocation()
                    }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, e.message, e)
        }

    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        Log.i("permisssion", "onRequestPermissionsResult")
        Log.i("permisssion", "requestCode $requestCode")
        if (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.i("permisssion", "location permission granted")
                enableMyLocation()
            }
            else {
                Log.i("permisssion", "location permission not granted")
            }
        }
        else {
            Log.i("permisssion", "location permission not granteds")
        }
    }

    private fun isPermissionGranted() : Boolean {
        return ContextCompat.checkSelfPermission(
                getContext()!!,
                Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence

        if (reminderPoi != null) {
            _viewModel.selectedPOI.value = reminderPoi
            _viewModel.reminderSelectedLocationStr.value = reminderPoi?.name
            _viewModel.latitude.value = reminderPoi?.latLng?.latitude
            _viewModel.longitude.value = reminderPoi?.latLng?.longitude
            findNavController().navigateUp()
        } else {
            Log.i("poi", "You need to select a POI")
            Snackbar.make(
                binding.selectLocation,
                R.string.err_select_location, Snackbar.LENGTH_INDEFINITE
            ).show()
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val TAG = "HuntMainActivity"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val REQUEST_TURN_LOCATION_ON = 29
