package com.example.gpsspeed

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        const val CODE_REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION = 101
        const val JOB_ID: Int = 201

        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_SPEED = "extra_speed"
        const val EXTRA_TIME = "extra_time"
        const val DATE_FORMAT_ELAPSED_TIME = "%d min, %d sec"
    }

    private lateinit var broadCastReceiver : BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViewClicks()
        initBroadcastReceiver()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CODE_REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                scheduleJob()
            }
        }
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadCastReceiver)
        super.onDestroy()
    }

    private fun initViewClicks() {
        tvStartCounting.setOnClickListener {
            tvStopCounting.visibility = View.VISIBLE
            tvStartCounting.visibility = View.GONE
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    CODE_REQUEST_PERMISSIONS_ACCESS_FINE_LOCATION
                )
            } else {
                scheduleJob()
            }
        }

        tvStopCounting.setOnClickListener {
            tvStopCounting.visibility = View.GONE
            tvStartCounting.visibility = View.VISIBLE
            clearTextViewValues()
            cancelJob()
        }
    }

    private fun initBroadcastReceiver() {
        broadCastReceiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, intent: Intent?) {
                if (intent?.action == LocationJobScheduler.ACTION_UPDATE_LOCATION) {
                    updateParams(intent)
                }
            }
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(broadCastReceiver, IntentFilter(LocationJobScheduler.ACTION_UPDATE_LOCATION))
    }

    private fun updateParams(intent: Intent) {
        val location = intent.getParcelableExtra<Location>(EXTRA_LOCATION)
        val speed = intent.getFloatExtra(EXTRA_SPEED, 0f)
        val elapsedTime = intent.getLongExtra(EXTRA_TIME, 0L)

        val formattedElapsedTime = String.format(
            DATE_FORMAT_ELAPSED_TIME,
            TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
            TimeUnit.MILLISECONDS.toSeconds(elapsedTime) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime)))

        if(location != null) {
            tvCurrentLocation.text = getString(
                R.string.current_location,
                "${location.latitude} : ${location.longitude}"
            )
        }
        tvElapsedTime.text = String.format(getString(R.string.elapsed_time,  formattedElapsedTime))
        tvCurrentSpeed.text = String.format(getString(R.string.current_speed, speed))
    }


    private fun clearTextViewValues() {
        tvCurrentSpeed.text = ""
        tvElapsedTime.text=""
        tvCurrentLocation.text=""
    }

    private fun scheduleJob() {
        val info = JobInfo.Builder(JOB_ID, ComponentName(this, LocationJobScheduler::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()
        val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        scheduler.schedule(info)
    }

    private fun cancelJob() {
        (getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).run {
            cancel(JOB_ID)
        }
    }
}
