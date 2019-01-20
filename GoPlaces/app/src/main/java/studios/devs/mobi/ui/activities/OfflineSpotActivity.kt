package studios.devs.mobi.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.jakewharton.rxbinding2.widget.text
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.addTo
import studios.devs.mobi.MainApplication
import studios.devs.mobi.R
import studios.devs.mobi.databinding.ActivityOfflineSpotBinding
import studios.devs.mobi.extension.addTo
import studios.devs.mobi.extension.rxClick
import studios.devs.mobi.extension.rxTextChanges
import studios.devs.mobi.extension.toVisibility
import studios.devs.mobi.model.SpotEntity
import studios.devs.mobi.ui.dialogs.AllSpotsDialog
import studios.devs.mobi.ui.dialogs.SPOTS_LIST
import studios.devs.mobi.viewmodels.OfflineSpotViewModel
import studios.devs.mobi.viewmodels.OfflineSpotViewModelInput
import studios.devs.mobi.viewmodels.OfflineSpotViewModelInputOutput
import studios.devs.mobi.viewmodels.OfflineSpotViewModelOutput
import java.util.ArrayList
import javax.inject.Inject
import kotlin.jvm.java

class OfflineSpotActivity : BaseActivity(), AllSpotsDialog.SelectedSpotListener {

    lateinit var mapFragment: SupportMapFragment
    lateinit var googleMap: GoogleMap
    lateinit var binding: ActivityOfflineSpotBinding
    private lateinit var latitude: String
    private lateinit var longitude: String
    private var addPlaceIsShown: Boolean = false

    companion object {
        const val PLACE_PICKER_INTENT = 1
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            latitude = location.latitude.toString()
            longitude = location.longitude.toString()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: OfflineSpotViewModelInputOutput by lazy {
        ViewModelProviders.of(
                this,
                viewModelFactory
        ).get(OfflineSpotViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainApplication.appComponent.inject(this)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_offline_spot)
        setUpLocationManager()

    }

    override fun onStart() {
        super.onStart()
        viewModel
                .bind(this)
                .addTo(compositeDisposable)
        viewModel.input.loadAllSpots()
    }

    override fun onBackPressed() {
        if (addPlaceIsShown) {
            closeAddPlace()
        } else {
            super.onBackPressed()
        }
    }

    fun askForLocation() {
        val builder = PlacePicker.IntentBuilder()
        startActivityForResult(builder.build(this), PLACE_PICKER_INTENT)
    }

    fun askForSpotName() {
        showToast(getString(R.string.empty_title))
    }

    fun spotAlreadyIncluded() {
        showToast(getString(R.string.already_in_list))
    }

    fun navigate(spotEntity: SpotEntity) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${spotEntity.latitude},${spotEntity.longitude}"))
        startActivity(intent)
    }

    fun showAllSpots(allSpots: List<SpotEntity>) {
        if (allSpots.isEmpty()) {
            showToast(getString(R.string.nothing_in_list))
        } else {
            val allSpotssFragment = AllSpotsDialog()
            val bundle = Bundle()
            val spotTitles = arrayListOf<String>()
            for (i in allSpots.indices) {
                spotTitles.add(allSpots[i].spotTitle)
            }
            bundle.putParcelableArrayList(SPOTS_LIST, allSpots as ArrayList<out Parcelable>)
            allSpotssFragment.arguments = bundle
            allSpotssFragment.show(supportFragmentManager, "spots")
        }
    }

    override fun onSpotSelected(spotTitle: String) {
        viewModel.input.navigateToConcreteSpot(spotTitle)
    }

    @SuppressLint("MissingPermission")
    private fun setUpLocationManager() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, locationListener)

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showToast(getString(R.string.error_loc_unavailable))
        } else {
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            if (location != null) {
                val lati = location.latitude
                val longi = location.longitude
                latitude = lati.toString()
                longitude = longi.toString()
                viewModel.input.locationSet(latitude, longitude)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PLACE_PICKER_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlacePicker.getPlace(this, data)
                latitude = place.latLng.latitude.toString()
                longitude = place.latLng.longitude.toString()
                viewModel.input.locationSet(latitude, longitude)
            }
        }
    }

    fun switchToAddPlace() {
        addPlaceIsShown = true
        binding.optionsMenu.root.visibility = View.GONE
        binding.addPlaceMenu.root.visibility = View.VISIBLE
    }

    fun closeAddPlace() {
        addPlaceIsShown = false
        viewModel.input.loadAllSpots()
        binding.addPlaceMenu.addSpotTxt.text?.clear()
        binding.optionsMenu.root.visibility = View.VISIBLE
        binding.addPlaceMenu.root.visibility = View.GONE
    }


}

private fun OfflineSpotViewModelInputOutput.bind(activity: OfflineSpotActivity): List<Disposable> {
    return listOf(
            output.bind(activity.binding),
            output.bind(activity),
            input.bind(activity.binding),
            activity.binding.bind(activity)
    ).flatten()
}

private fun ActivityOfflineSpotBinding.bind(activity: OfflineSpotActivity): List<Disposable> {
    return listOf(
            optionsMenu.buttonAddPlace.rxClick.subscribe { activity.switchToAddPlace() },
            addPlaceMenu.closeButton.rxClick.subscribe { activity.closeAddPlace() }
    )
}

private fun OfflineSpotViewModelInput.bind(binding: ActivityOfflineSpotBinding): List<Disposable> {
    return listOf(
            binding.mainHolder.spotIcon.rxClick.subscribe { navigate() },
            binding.optionsMenu.buttonPlaceToGo.rxClick.subscribe { showRandomSpot() },
            binding.optionsMenu.buttonAllPlaces.rxClick.subscribe { showAllSpots() },
            binding.addPlaceMenu.addSpotTxt.rxTextChanges.subscribe { newSpotText(it) },
            binding.addPlaceMenu.addSpotBtn.rxClick.subscribe { addNewSpot() },
            binding.addPlaceMenu.currentLocationBox.rxClick.subscribe { useCurrentLocationIsChecked() }
    )
}


private fun OfflineSpotViewModelOutput.bind(activity: OfflineSpotActivity): List<Disposable> {
    return listOf(
            newSpotAddedStream.subscribe { activity.showToast(it.spotTitle + ", added!") },
            askForLocationStream.subscribe { activity.askForLocation() },
            askForSpotNameStream.subscribe { activity.askForSpotName() },
            spotIsAlreadyIncluded.subscribe { activity.spotAlreadyIncluded() },
            showAllSpotsStream.subscribe { activity.showAllSpots(it) },
            emptySpotListStream.subscribe { activity.showToast(activity.getString(R.string.nothing_in_list)) },
            errorStream.subscribe { activity.renderError(it.description) },
            loadingViewModelOutput.isLoading.subscribe { activity.renderLoading(it) },
            mapNavigationStream.subscribe { activity.navigate(it) }
    )
}

private fun OfflineSpotViewModelOutput.bind(binding: ActivityOfflineSpotBinding): List<Disposable> {
    return listOf(
            randomSpotStream.subscribe { binding.mainHolder.placeResultText.text = it }
    )
}




