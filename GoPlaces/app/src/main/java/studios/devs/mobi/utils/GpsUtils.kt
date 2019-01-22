package studios.devs.mobi.utils

import android.widget.Toast
import android.app.Activity
import com.google.android.gms.location.LocationSettingsStatusCodes
import android.content.IntentSender
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.location.LocationSettingsResponse
import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.tasks.OnSuccessListener
import android.location.LocationManager
import io.fabric.sdk.android.services.settings.IconRequest.build
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import android.content.Context.LOCATION_SERVICE
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.location.SettingsClient


open class GpsUtils(private val context: Context) {

    companion object {
        private val GPS_REQUEST = 1001
    }

    private val mSettingsClient: SettingsClient
    private val mLocationSettingsRequest: LocationSettingsRequest
    private val locationManager: LocationManager
    private val locationRequest: LocationRequest

    init {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        mSettingsClient = LocationServices.getSettingsClient(context)
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (10 * 1000).toLong()
        locationRequest.fastestInterval = (2 * 1000).toLong()
        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)
        mLocationSettingsRequest = builder.build()
        //**************************
        builder.setAlwaysShow(true) //this is the key ingredient
        //**************************
    }

    // method for turn on GPS
    fun turnGPSOn(onGpsListener: onGpsListener?) {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            onGpsListener?.gpsStatus(true)
        } else {
            mSettingsClient
                    .checkLocationSettings(mLocationSettingsRequest)
                    .addOnSuccessListener(context as Activity) {
                        //  GPS is already enable, callback GPS status through listener
                        onGpsListener?.gpsStatus(true)
                    }
                    .addOnFailureListener(context as Activity) { e ->
                        val statusCode = (e as ApiException).statusCode
                        when (statusCode) {
                            LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                                // Show the dialog by calling startResolutionForResult(), and check the
                                // result in onActivityResult().
                                val rae = e as ResolvableApiException
                                rae.startResolutionForResult(context as Activity, GPS_REQUEST)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i("MOBI", "PendingIntent unable to execute request.")
                            }

                            LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                                val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                                Log.e("MOBI", errorMessage)
                                Toast.makeText(context as Activity, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
        }
    }

    interface onGpsListener {
        fun gpsStatus(isGPSEnable: Boolean)
    }
}