package com.example.indiangridnavigation

import android.animation.ValueAnimator
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.lottie.LottieAnimationView
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.maps.android.SphericalUtil

class MainActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var gridInfoText: TextView
    private lateinit var compassView: CompassView
    private lateinit var currentLocationCard: MaterialCardView
    private lateinit var gridInfoCard: MaterialCardView
    private lateinit var refreshButton: MaterialButton
    private lateinit var satelliteButton: MaterialButton
    private lateinit var routeButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var currentLocation: Location? = null
    private var gridOverlay: Polygon? = null
    private var isTracking = true
    private var currentGrid: IndianGridSystem.GridCoordinate? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupSwipeRefresh()
        setupButtons()
        setupSensors()
        setupLocationClient()
        initializeMap(savedInstanceState)
    }

    private fun initializeViews() {
        mapView = findViewById(R.id.mapView)
        gridInfoText = findViewById(R.id.gridInfoText)
        compassView = findViewById(R.id.compassView)
        currentLocationCard = findViewById(R.id.currentLocationCard)
        gridInfoCard = findViewById(R.id.gridInfoCard)
        refreshButton = findViewById(R.id.refreshButton)
        satelliteButton = findViewById(R.id.satelliteButton)
        routeButton = findViewById(R.id.routeButton)
        settingsButton = findViewById(R.id.settingsButton)
        loadingAnimation = findViewById(R.id.loadingAnimation)
        swipeRefresh = findViewById(R.id.swipeRefresh)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.primary),
            ContextCompat.getColor(this, R.color.accent),
            ContextCompat.getColor(this, R.color.success)
        )

        swipeRefresh.setOnRefreshListener {
            refreshLocationData()
            Handler(Looper.getMainLooper()).postDelayed({
                swipeRefresh.isRefreshing = false
            }, 2000)
        }
    }

    private fun setupButtons() {
        refreshButton.setOnClickListener {
            refreshLocationData()
            animateButton(refreshButton)
        }

        satelliteButton.setOnClickListener {
            val intent = Intent(this, SatelliteActivity::class.java)
            startActivity(intent)
            animateButton(satelliteButton)
        }

        routeButton.setOnClickListener {
            showRoutePlanningDialog()
            animateButton(routeButton)
        }

        settingsButton.setOnClickListener {
            showSettingsDialog()
            animateButton(settingsButton)
        }

        currentLocationCard.setOnClickListener {
            currentLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            }
        }
    }

    private fun animateButton(button: MaterialButton) {
        val animator = ValueAnimator.ofFloat(1f, 0.8f, 1f)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener { value ->
            button.scaleX = value.animatedValue as Float
            button.scaleY = value.animatedValue as Float
        }
        animator.start()
    }

    private fun setupSensors() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun initializeMap(savedInstanceState: Bundle?) {
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()
        requestLocationUpdates()
    }

    private fun setupMap() {
        val indiaCenter = LatLng(20.5937, 78.9629)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(indiaCenter, 5f))

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        googleMap.uiSettings.isCompassEnabled = false

        googleMap.setOnMapClickListener { latLng ->
            updateGridOverlay(latLng)
            showGridInformation(latLng)
        }

        googleMap.setOnMapLongClickListener { latLng ->
            addNavigationMarker(latLng)
        }
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateLocationUI(location)
                }
            }
        }

        if (checkLocationPermissions()) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            requestLocationPermissions()
        }
    }

    private fun updateLocationUI(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (isTracking) {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            googleMap.animateCamera(cameraUpdate)
        }

        updateGridInformation(latLng)
        updateCurrentLocationCard(location)
    }

    private fun updateCurrentLocationCard(location: Location) {
        val speed = if (location.hasSpeed()) "${String.format("%.1f", location.speed * 3.6)} km/h" else "0 km/h"
        val accuracy = "${String.format("%.1f", location.accuracy)} m"

        currentLocationCard.findViewById<TextView>(R.id.speedText)?.text = speed
        currentLocationCard.findViewById<TextView>(R.id.accuracyText)?.text = "Accuracy: $accuracy"
        currentLocationCard.findViewById<TextView>(R.id.altitudeText)?.text = "${String.format("%.0f", location.altitude)} m"
    }

    private fun updateGridInformation(latLng: LatLng) {
        val newGrid = IndianGridSystem.latLngToGrid(latLng)

        if (currentGrid?.code != newGrid.code) {
            animateGridChange(newGrid)
            currentGrid = newGrid
        }

        showGridInformation(latLng)
        updateGridOverlay(latLng)
    }

    private fun animateGridChange(newGrid: IndianGridSystem.GridCoordinate) {
        val animator = ValueAnimator.ofArgb(Color.WHITE, Color.YELLOW, Color.WHITE)
        animator.duration = 1000
        animator.addUpdateListener { value ->
            gridInfoText.setTextColor(value.animatedValue as Int)
        }
        animator.start()
    }

    private fun showGridInformation(latLng: LatLng) {
        val gridCoord = IndianGridSystem.latLngToGrid(latLng)
        gridInfoText.text = "Grid Code: ${gridCoord.code}\n" +
                           "Coordinates: (${gridCoord.x}, ${gridCoord.y})\n" +
                           "LatLng: ${String.format("%.4f", latLng.latitude)}, " +
                           "${String.format("%.4f", latLng.longitude)}"

        currentLocation?.let { location ->
            val currentLatLng = LatLng(location.latitude, location.longitude)
            val distance = SphericalUtil.computeDistanceBetween(currentLatLng, latLng)
            gridInfoText.append("\nDistance: ${String.format("%.2f", distance)} meters")
        }
    }

    private fun updateGridOverlay(latLng: LatLng) {
        gridOverlay?.remove()

        val gridCoord = IndianGridSystem.latLngToGrid(latLng)
        val gridBounds = IndianGridSystem.getGridBounds(gridCoord.x, gridCoord.y)

        gridOverlay = googleMap.addPolygon(
            PolygonOptions()
                .addAll(gridBounds)
                .strokeColor(0xFF0000FF.toInt())
                .fillColor(0x220000FF.toInt())
                .strokeWidth(3f)
        )
    }

    private fun addNavigationMarker(latLng: LatLng) {
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Navigation Point")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
    }

    private fun refreshLocationData() {
        loadingAnimation.visibility = View.VISIBLE
        loadingAnimation.playAnimation()

        currentLocation?.let { location ->
            updateLocationUI(location)
        }

        Handler(Looper.getMainLooper()).postDelayed({
            loadingAnimation.visibility = View.GONE
            loadingAnimation.cancelAnimation()
        }, 1500)
    }

    private fun showRoutePlanningDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_route_planning, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Plan Route")
            .setView(dialogView)
            .setPositiveButton("Calculate Route") { dialog, _ ->
                calculateRoute(dialogView)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.show()
    }

    private fun calculateRoute(dialogView: View) {
        val startGridX = dialogView.findViewById<EditText>(R.id.startGridX).text.toString().toIntOrNull()
        val startGridY = dialogView.findViewById<EditText>(R.id.startGridY).text.toString().toIntOrNull()
        val endGridX = dialogView.findViewById<EditText>(R.id.endGridX).text.toString().toIntOrNull()
        val endGridY = dialogView.findViewById<EditText>(R.id.endGridY).text.toString().toIntOrNull()

        if (startGridX != null && startGridY != null && endGridX != null && endGridY != null) {
            val startGrid = IndianGridSystem.GridCoordinate(startGridX, startGridY, "")
            val endGrid = IndianGridSystem.GridCoordinate(endGridX, endGridY, "")

            val route = NavigationUtils.calculateGridRoute(startGrid, endGrid)
            drawRouteOnMap(route)
        } else {
            Toast.makeText(this, "Please enter valid grid coordinates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRouteOnMap(route: List<IndianGridSystem.GridCoordinate>) {
        googleMap.clear()

        val routePoints = mutableListOf<LatLng>()
        route.forEach { grid ->
            val center = IndianGridSystem.getGridCenter(grid.x, grid.y)
            routePoints.add(center)

            if (grid == route.first() || grid == route.last()) {
                val markerOptions = MarkerOptions()
                    .position(center)
                    .title(if (grid == route.first()) "Start" else "End")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                        if (grid == route.first()) BitmapDescriptorFactory.HUE_GREEN
                        else BitmapDescriptorFactory.HUE_RED
                    ))
                googleMap.addMarker(markerOptions)
            }
        }

        googleMap.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(Color.BLUE)
                .width(8f)
                .geodesic(true)
        )

        val bounds = LatLngBounds.builder()
        routePoints.forEach { bounds.include(it) }
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
    }

    private fun showSettingsDialog() {
        val settings = arrayOf("Toggle Grid Overlay", "Toggle Compass", "Change Map Style", "Units: Metric")
        val checkedItems = booleanArrayOf(true, true, false, true)

        AlertDialog.Builder(this)
            .setTitle("Settings")
            .setMultiChoiceItems(settings, checkedItems) { dialog, which, isChecked ->
                when (which) {
                    0 -> toggleGridOverlay(isChecked)
                    1 -> toggleCompass(isChecked)
                    2 -> changeMapStyle(isChecked)
                    3 -> toggleUnits(isChecked)
                }
            }
            .setPositiveButton("Save") { dialog, _ -> dialog.dismiss() }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun toggleGridOverlay(show: Boolean) {
        if (show) {
            currentLocation?.let { location ->
                val latLng = LatLng(location.latitude, location.longitude)
                updateGridOverlay(latLng)
            }
        } else {
            gridOverlay?.remove()
        }
    }

    private fun toggleCompass(show: Boolean) {
        compassView.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun changeMapStyle(satellite: Boolean) {
        val mapStyle = if (satellite) {
            GoogleMap.MAP_TYPE_SATELLITE
        } else {
            GoogleMap.MAP_TYPE_NORMAL
        }
        googleMap.mapType = mapStyle
    }

    private fun toggleUnits(metric: Boolean) {
        // Unit conversion logic can be added here
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
            }
        }
        updateOrientation()
    }

    private fun updateOrientation() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        compassView.updateOrientation(azimuth, pitch, roll)
        rotateMapBasedOnBearing(azimuth)
    }

    private fun rotateMapBasedOnBearing(bearing: Float) {
        val cameraPosition = CameraPosition.Builder(googleMap.cameraPosition)
            .bearing(bearing)
            .build()
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun checkLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        androidx.core.app.ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdates()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        registerSensors()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun registerSensors() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }
}
