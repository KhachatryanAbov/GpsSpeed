package com.example.gpsspeed

import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity


class LocationJobScheduler : JobService() {

    companion object {
        const val ACTION_UPDATE_LOCATION = "action_update_location"
        const val MILLIS_TO_SECONDS_DIVISIBLE = 1000L
        const val DEF_ELAPSED_TIME_VALE = 1L
        const val PARAMS_UPDATE_FREQUENCY = 1000L
    }

    private var jobCancelled = false
    private var distance: Float = 0f
    private var totalDistance: Float = 0f
    private var speed: Float = 0f
    private var elapsedTime: Long = DEF_ELAPSED_TIME_VALE
    private var oldTime = System.currentTimeMillis()

    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var locationManager: LocationManager
    private var oldLocation: Location? = null
    private var newLocation: Location? = null
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            doCalculations(location)
            publishResults()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        jobCancelled = true
        locationManager.removeUpdates(locationListener)
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        initBroadcastReceiver()
        initLocationManager()
        listenLocationUpdates()
        doBackgroundWork()
        return true
    }

    private fun initBroadcastReceiver() {
        localBroadcastManager = LocalBroadcastManager.getInstance(this)
    }

    private fun initLocationManager() {
        locationManager = getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager
    }

    private fun listenLocationUpdates() {
        try {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener)
        } catch (ex: SecurityException) {
        }
    }

    private fun doBackgroundWork() {
        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!jobCancelled) {
                    doCalculations(newLocation)
                    publishResults()
                    handler.postDelayed(this, PARAMS_UPDATE_FREQUENCY)
                }
            }
        }, PARAMS_UPDATE_FREQUENCY)
    }

    private fun publishResults() {
        val intent = Intent(ACTION_UPDATE_LOCATION)
        intent.putExtra(MainActivity.EXTRA_LOCATION, newLocation)
        intent.putExtra(MainActivity.EXTRA_SPEED, speed)
        intent.putExtra(MainActivity.EXTRA_TIME, elapsedTime)
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun doCalculations(location: Location?) {
        newLocation = location
        if (oldLocation == null && location != null) {
            oldLocation = Location(location)
        }
        if (location != null) {
            distance = location.distanceTo(oldLocation)
            totalDistance += distance
            oldLocation = Location(location)
        }
        elapsedTime = (System.currentTimeMillis() - oldTime)
        if (elapsedTime == 0L) let {
            elapsedTime = DEF_ELAPSED_TIME_VALE
        }
        speed = totalDistance / (elapsedTime / MILLIS_TO_SECONDS_DIVISIBLE)
    }

}