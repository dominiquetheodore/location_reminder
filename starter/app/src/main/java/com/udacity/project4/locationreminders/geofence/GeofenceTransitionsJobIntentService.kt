package com.udacity.project4.locationreminders.geofence

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.JobIntentService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class GeofenceTransitionsJobIntentService : JobIntentService(), CoroutineScope {

    private var coroutineJob: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + coroutineJob

    companion object {
        private const val JOB_ID = 573

        //  TODO: call this to start the JobIntentService to handle the geofencing transition events
        fun enqueueWork(context: Context, intent: Intent) {
            enqueueWork(
                context,
                GeofenceTransitionsJobIntentService::class.java, JOB_ID,
                intent
            )
        }
    }

    override fun onHandleWork(intent: Intent) {
        //TODO: handle the geofencing transition events and
        // send a notification to the user when he enters the geofence area
        //TODO call @sendNotification
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        Log.e("geoFence", "geofence event")
        if (geofencingEvent.hasError()) {
            val errorMessage = "Error encountered " + geofencingEvent.errorCode
            Log.e("geoFence", errorMessage)
            return
        }
        // 3
        handleEvent(geofencingEvent)
    }

    //TODO: get the request id of the current geofence
    private lateinit var requestId : String

    private fun sendNotification(triggeringGeofences: List<Geofence>) {
        for (i in triggeringGeofences.indices) {
            if (triggeringGeofences.isNotEmpty()) {
                requestId = triggeringGeofences[i].requestId

            } else {

                Log.i("NO_GEOFENCE","No Triggering Geofence Found")
                return
            }

            //Get the local repository instance
            val remindersLocalRepository: ReminderDataSource by inject()
//        Interaction to the repository has to be through a coroutine scope
            CoroutineScope(coroutineContext).launch(SupervisorJob()) {
                //get the reminder with the request id

                val result = remindersLocalRepository.getReminder(requestId)
                if (result is Result.Success<ReminderDTO>) {
                    val reminderDTO = result.data
                    //send a notification to the user with the reminder details
                    Log.i("geoFence", "line 68: send notification for " + reminderDTO.title)
                    sendNotification(
                        this@GeofenceTransitionsJobIntentService, ReminderDataItem(
                            reminderDTO.title,
                            reminderDTO.description,
                            reminderDTO.location,
                            reminderDTO.latitude,
                            reminderDTO.longitude,
                            reminderDTO.id
                        )
                    )
                }
                else {
                    Log.i("geoFence", "requestId $requestId not found")
                }
            }

        }

    }

    private fun handleEvent(event: GeofencingEvent) {
        // 1

        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                sendNotification(event.triggeringGeofences)
                Log.i("geoFence", "some notifications were triggered")
        }
    }
}