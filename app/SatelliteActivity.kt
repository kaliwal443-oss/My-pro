package com.example.indiangridnavigation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.util.*
import kotlin.math.*

class SatelliteActivity : AppCompatActivity() {

    private lateinit var radarChart: BarChart
    private lateinit var signalChart: LineChart
    private lateinit var satelliteCountText: android.widget.TextView
    private lateinit var signalStrengthText: android.widget.TextView
    private lateinit var accuracyText: android.widget.TextView
    private lateinit var gnssStatusText: android.widget.TextView
    private lateinit var coordinateText: android.widget.TextView
    private lateinit var gridCoordinateText: android.widget.TextView
    private lateinit var locationText: android.widget.TextView
    private lateinit var coordinateCard: MaterialCardView
    private lateinit var signalCard: MaterialCardView
    private lateinit var exportButton: MaterialButton
    private lateinit var shareButton: MaterialButton

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLocation: Location? = null
    private var satelliteCount = 0
    private var strongSignals = 0

    private val signalData = ArrayList<Entry>()
    private val satelliteSystems = arrayOf("GPS", "GLONASS", "Galileo", "BeiDou")
    private var chartUpdateHandler = Handler(Looper.getMainLooper())
    private var chartUpdateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_satellite)

        initializeViews()
        setupCharts()
        setupButtons()
        startLocationUpdates()
        startChartUpdates()
    }

    private fun initializeViews() {
        radarChart = findViewById(R.id.radarChart)
        signalChart = findViewById(R.id.signalChart)
        satelliteCountText = findViewById(R.id.satelliteCountText)
        signalStrengthText = findViewById(R.id.signalStrengthText)
        accuracyText = findViewById(R.id.accuracyText)
        gnssStatusText = findViewById(R.id.gnssStatusText)
        coordinateText = findViewById(R.id.coordinateText)
        gridCoordinateText = findViewById(R.id.gridCoordinateText)
        locationText = findViewById(R.id.locationText)
        coordinateCard = findViewById(R.id.coordinateCard)
        signalCard = findViewById(R.id.signalCard)
        exportButton = findViewById(R.id.exportButton)
        shareButton = findViewById(R.id.shareButton)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setupCharts() {
        setupRadarChart()
        setupSignalChart()
    }

    private fun setupRadarChart() {
        radarChart.description.isEnabled = false
        radarChart.setTouchEnabled(true)
        radarChart.isDragEnabled = true
        radarChart.setScaleEnabled(true)
        radarChart.setPinchZoom(true)

        val xAxis = radarChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(satelliteSystems)
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 10f
        xAxis.granularity = 1f

        val yAxis = radarChart.axisLeft
        yAxis.textColor = Color.WHITE
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 10f
        yAxis.granularity = 1f

        radarChart.axisRight.isEnabled = false
        radarChart.legend.isEnabled = false
        radarChart.setDrawBarShadow(false)
        radarChart.setDrawValueAboveBar(true)
    }

    private fun setupSignalChart() {
        signalChart.description.isEnabled = false
        signalChart.setTouchEnabled(true)
        signalChart.isDragEnabled = true
        signalChart.setScaleEnabled(true)
        signalChart.setPinchZoom(true)

        val xAxis = signalChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textColor = Color.WHITE
        xAxis.granularity = 1f

        val yAxis = signalChart.axisLeft
        yAxis.textColor = Color.WHITE
        yAxis.axisMinimum = 0f
        yAxis.axisMaximum = 50f
        yAxis.setLabelCount(6, true)

        signalChart.axisRight.isEnabled = false
        signalChart.legend.textColor = Color.WHITE
    }

    private fun setupButtons() {
        exportButton.setOnClickListener { exportData() }
        shareButton.setOnClickListener { shareData() }

        coordinateCard.setOnLongClickListener {
            copyCoordinatesToClipboard()
            true
        }

        signalCard.setOnLongClickListener {
            copySignalDataToClipboard()
            true
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 2000
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    updateCoordinateInformation(location)
                    accuracyText.text = "Accuracy: ${String.format("%.2f", location.accuracy)} meters"
                    updateSatelliteData() // Simulate satellite data based on accuracy
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                gnssStatusText.text = if (availability.isLocationAvailable) {
                    "GNSS Status: Available"
                } else {
                    "GNSS Status: Unavailable"
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
            == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    private fun updateCoordinateInformation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        updateCoordinateText(latLng)
        updateGridCoordinateText(latLng)
        updateLocationDetails(location)
    }

    private fun updateCoordinateText(latLng: LatLng) {
        val decimalDegrees = String.format("Lat: %.6f°\nLng: %.6f°", 
            latLng.latitude, latLng.longitude)
        
        val dmsLat = convertToDMS(latLng.latitude, true)
        val dmsLng = convertToDMS(latLng.longitude, false)
        
        val utm = convertToUTM(latLng)
        
        coordinateText.text = "Decimal Degrees:\n$decimalDegrees\n\n" +
                             "DMS:\nLat: $dmsLat\nLng: $dmsLng\n\n" +
                             "UTM:\n$utm"
    }

    private fun updateGridCoordinateText(latLng: LatLng) {
        val gridCoord = IndianGridSystem.latLngToGrid(latLng)
        val gridBounds = IndianGridSystem.getGridBounds(gridCoord.x, gridCoord.y)
        
        gridCoordinateText.text = "Indian Grid System:\n" +
                                 "Grid Code: ${gridCoord.code}\n" +
                                 "Grid Coordinates: (${gridCoord.x}, ${gridCoord.y})\n" +
                                 "Grid Size: 0.1° × 0.1° (~11km)\n" +
                                 "Bounds:\n" +
                                 "NW: ${String.format("%.4f", gridBounds[0].latitude)}, ${String.format("%.4f", gridBounds[0].longitude)}\n" +
                                 "NE: ${String.format("%.4f", gridBounds[1].latitude)}, ${String.format("%.4f", gridBounds[1].longitude)}\n" +
                                 "SE: ${String.format("%.4f", gridBounds[2].latitude)}, ${String.format("%.4f", gridBounds[2].longitude)}\n" +
                                 "SW: ${String.format("%.4f", gridBounds[3].latitude)}, ${String.format("%.4f", gridBounds[3].longitude)}"
    }

    private fun updateLocationDetails(location: Location) {
        val speed = if (location.hasSpeed()) "${String.format("%.2f", location.speed * 3.6)} km/h" else "Unknown"
        val bearing = if (location.hasBearing()) "${location.bearing.toInt()}°" else "Unknown"
        val altitude = if (location.hasAltitude()) "${String.format("%.2f", location.altitude)} m" else "Unknown"
        
        locationText.text = "Location Details:\n" +
                           "Speed: $speed\n" +
                           "Bearing: $bearing\n" +
                           "Altitude: $altitude\n" +
                           "Provider: ${location.provider}\n" +
                           "Time: ${Date(location.time).toString()}"
    }

    private fun convertToDMS(coord: Double, isLatitude: Boolean): String {
        val degrees = coord.toInt()
        val minutesDouble = (coord - degrees) * 60
        val minutes = minutesDouble.toInt()
        val seconds = (minutesDouble - minutes) * 60
        
        val direction = when {
            isLatitude && coord >= 0 -> "N"
            isLatitude -> "S"
            coord >= 0 -> "E"
            else -> "W"
        }
        
        return String.format("%d°%d'%.2f'' %s", 
            Math.abs(degrees), Math.abs(minutes), Math.abs(seconds), direction)
    }

    private fun convertToUTM(latLng: LatLng): String {
        val lat = latLng.latitude
        val lng = latLng.longitude
        
        val zone = ((lng + 180) / 6).toInt() + 1
        val band = getUTMBand(lat)
        
        val easting = ((lng - (zone * 6 - 183)) * 100000).toInt()
        val northing = (lat * 100000).toInt()
        
        return "Zone: ${zone}${band}\nEasting: $easting\nNorthing: $northing"
    }

    private fun getUTMBand(lat: Double): Char {
        return when {
            lat >= 72 -> 'X'
            lat >= 64 -> 'W'
            lat >= 56 -> 'V'
            lat >= 48 -> 'U'
            lat >= 40 -> 'T'
            lat >= 32 -> 'S'
            lat >= 24 -> 'R'
            lat >= 16 -> 'Q'
            lat >= 8 -> 'P'
            else -> 'N'
        }
    }

    private fun updateSatelliteData() {
        // Simulate satellite data based on location accuracy
        val accuracy = currentLocation?.accuracy ?: 100.0
        satelliteCount = when {
            accuracy < 10 -> (8 + (Math.random() * 4)).toInt()
            accuracy < 20 -> (6 + (Math.random() * 4)).toInt()
            accuracy < 50 -> (4 + (Math.random() * 4)).toInt()
            else -> (2 + (Math.random() * 4)).toInt()
        }
        
        strongSignals = when {
            accuracy < 10 -> (6 + (Math.random() * 2)).toInt()
            accuracy < 20 -> (4 + (Math.random() * 2)).toInt()
            accuracy < 50 -> (2 + (Math.random() * 2)).toInt()
            else -> (0 + (Math.random() * 2)).toInt()
        }

        updateSatelliteUI()
    }

    private fun updateSatelliteUI() {
        satelliteCountText.text = "Satellites: $satelliteCount"
        signalStrengthText.text = "Strong Signals: $strongSignals"
        
        val quality = when {
            strongSignals >= 6 -> "Excellent"
            strongSignals >= 4 -> "Good"
            strongSignals >= 2 -> "Fair"
            else -> "Poor"
        }
        
        signalStrengthText.append("\nQuality: $quality")
        
        when {
            satelliteCount >= 8 -> gnssStatusText.text = "GNSS Status: Excellent Fix"
            satelliteCount >= 6 -> gnssStatusText.text = "GNSS Status: Good Fix"
            satelliteCount >= 4 -> gnssStatusText.text = "GNSS Status: Fair Fix"
            satelliteCount > 0 -> gnssStatusText.text = "GNSS Status: Weak Fix"
            else -> gnssStatusText.text = "GNSS Status: No Fix"
        }
    }

    private fun startChartUpdates() {
        chartUpdateRunnable = object : Runnable {
            override fun run() {
                updateCharts()
                chartUpdateHandler.postDelayed(this, 2000)
            }
        }
        chartUpdateHandler.post(chartUpdateRunnable!!)
    }

    private fun updateCharts() {
        updateRadarChart()
        updateSignalChart()
    }

    private fun updateRadarChart() {
        val entries = ArrayList<BarEntry>()
        
        // Simulate satellite counts for different systems
        entries.add(BarEntry(0f, (satelliteCount * 0.4f + (Math.random() * 2)).toFloat())) // GPS
        entries.add(BarEntry(1f, (satelliteCount * 0.3f + (Math.random() * 2)).toFloat())) // GLONASS
        entries.add(BarEntry(2f, (satelliteCount * 0.2f + (Math.random() * 2)).toFloat())) // Galileo
        entries.add(BarEntry(3f, (satelliteCount * 0.1f + (Math.random() * 2)).toFloat())) // BeiDou
        
        val dataSet = BarDataSet(entries, "Satellites by System")
        dataSet.colors = listOf(Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW)
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 10f
        
        val barData = BarData(dataSet)
        radarChart.data = barData
        radarChart.invalidate()
    }

    private fun updateSignalChart() {
        if (signalData.size > 20) {
            signalData.removeAt(0)
            // Adjust x values after removal
            signalData.forEachIndexed { index, entry -> entry.x = index.toFloat() }
        }
        
        val baseStrength = when {
            strongSignals >= 6 -> 35f
            strongSignals >= 4 -> 25f
            strongSignals >= 2 -> 15f
            else -> 10f
        }
        
        val newValue = baseStrength + (Math.random() * 10).toFloat()
        signalData.add(Entry(signalData.size.toFloat(), newValue))
        
        val dataSet = LineDataSet(signalData, "Signal Strength (dB-Hz)")
        dataSet.color = Color.GREEN
        dataSet.valueTextColor = Color.WHITE
        dataSet.lineWidth = 2f
        dataSet.setDrawCircles(false)
        dataSet.setDrawValues(false)
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        
        val lineData = LineData(dataSet)
        signalChart.data = lineData
        signalChart.invalidate()
    }

    private fun exportData() {
        val exportText = generateExportText()
        // In a real app, you would save this to a file
        Toast.makeText(this, "Data ready for export", Toast.LENGTH_SHORT).show()
        
        // Simulate export process
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(this, "Data exported successfully", Toast.LENGTH_SHORT).show()
        }, 1000)
    }

    private fun shareData() {
        val shareText = generateShareText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share Satellite Data"))
    }

    private fun generateShareText(): String {
        return """🌍 Satellite Navigation Data
                
📡 Satellite Information:
- Total Satellites: $satelliteCount
- Strong Signals: $strongSignals
- Signal Quality: ${when {
    strongSignals >= 6 -> "Excellent"
    strongSignals >= 4 -> "Good" 
    strongSignals >= 2 -> "Fair"
    else -> "Poor"
}}

📍 Location Data:
${coordinateText.text}

🛰️ GNSS Status: ${gnssStatusText.text.toString().replace("GNSS Status: ", "")}

Generated by Indian Grid Navigation App"""
    }

    private fun generateExportText(): String {
        return "Timestamp,Latitude,Longitude,Satellites,StrongSignals,Accuracy\n" +
               "${Date()},${currentLocation?.latitude},${currentLocation?.longitude},$satelliteCount,$strongSignals,${currentLocation?.accuracy}"
    }

    private fun copyCoordinatesToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Coordinates", coordinateText.text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun copySignalDataToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val signalText = "Satellites: $satelliteCount\nStrong Signals: $strongSignals"
        val clip = ClipData.newPlainText("Signal Data", signalText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Signal data copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        startChartUpdates()
    }

    override fun onPause() {
        super.onPause()
        chartUpdateRunnable?.let { chartUpdateHandler.removeCallbacks(it) }
        fusedLocationClient.flushLocations()
    }

    override fun onDestroy() {
        super.onDestroy()
        chartUpdateRunnable?.let { chartUpdateHandler.removeCallbacks(it) }
    }
}
