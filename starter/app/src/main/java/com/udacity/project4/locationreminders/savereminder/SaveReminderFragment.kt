package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    lateinit var geofencingClient: GeofencingClient

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            val reminder = ReminderDataItem(title, description, location, latitude, longitude)

            Log.i("geoFence", "saving reminder")
            val reminderDTO = _viewModel.validateAndSaveReminder(
                    ReminderDataItem(
                            title,
                            description,
                            location,
                            latitude,
                            longitude
                    )
            )
            if (reminderDTO != null) {
                addGeofenceForReminder(reminder)
            }
            else {
                Log.i("geoFence", "error saving reminder")
            }
//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofences(listOf(geofence))
                .build()
    }

    private fun buildGeofence(reminder: ReminderDataItem): Geofence? {
        val latitude = reminder.latitude
        val longitude = reminder.longitude
        val radius = 150f

        if (latitude != null && longitude != null && radius != null) {
            return Geofence.Builder()
                    // 1
                    .setRequestId(reminder.id)
                    // 2
                    .setCircularRegion(
                            latitude,
                            longitude,
                            radius.toFloat()
                    )
                    // 3
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    // 4
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
        }

        return null
    }


    @SuppressLint("MissingPermission")
    private fun addGeofenceForReminder(reminder: ReminderDataItem) {
        val geofence = buildGeofence(reminder)

        if (geofence != null){
            geofencingClient
                    .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                    // 3
                    .addOnSuccessListener {
                        Log.i("geoFence", "line 125: added geofence")
                    }
                    // 4
                    .addOnFailureListener {
                        Log.e("geoFence", "error adding geofence")
                    }
        }
        else {
            Log.i("geoFence", "null geofence")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
                "ACTION_GEOFENCE_EVENT"
    }
}
