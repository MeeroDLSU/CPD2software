package CPD2.scworker
// Functions to be used in TimeInPage
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ParseException
import android.os.CountDownTimer
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale




lateinit var countdown: TextView
private var countdownTimer: CountDownTimer? = null
val countdownHandler = Handler()
var isTimerFinished = false
lateinit var timeinButton: Button
lateinit var timerPrompt: TextView
lateinit var requestButton: Button

//fun fetchLocation(
//    context: Context,
//    fusedLocationProviderClient: FusedLocationProviderClient,
//    listener: (latitude: Double, longitude: Double) -> Unit
//) {
//    val task = fusedLocationProviderClient.lastLocation
//
//    if (ActivityCompat.checkSelfPermission(
//            context,
//            android.Manifest.permission.ACCESS_FINE_LOCATION
//        )
//        != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
//        ) != PackageManager.PERMISSION_GRANTED
//    ) {
//        ActivityCompat.requestPermissions(
//            context as Activity,
//            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
//            101
//        )
//        return
//    }
//
//    task.addOnSuccessListener { location ->
//        if (location != null) {
//            val latitude = location.latitude
//            val longitude = location.longitude
//            listener(latitude, longitude)
//        } else {
//            // Handle case where location is null
//            Toast.makeText(context, "Location data not available", Toast.LENGTH_SHORT).show()
//        }
//    }.addOnFailureListener { e ->
//        // Handle any errors that occur while fetching location
//        Toast.makeText(context, "Error fetching location: ${e.message}", Toast.LENGTH_SHORT)
//            .show()
//        Log.e("LocationError", "Error fetching location", e)
//    }
//}

//fun calculateDistance(
//    context: Context,
//    lat1: Double,
//    lon1: Double,
//    lat2: Double,
//    lon2: Double
//): Float {
//    val result = FloatArray(1)
//    try {
//        Location.distanceBetween(lat1, lon1, lat2, lon2, result)
//    } catch (e: Exception) {
//        // Handle any exceptions that occur during distance calculation
//        Toast.makeText(context, "Error calculating distance: ${e.message}", Toast.LENGTH_SHORT)
//            .show()
//        Log.e("DistanceError", "Error calculating distance", e)
//    }
//    return result[0]
//}

fun formatDate(dateTime: String): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateTime)
        val outputDateFormat = SimpleDateFormat("MMM dd HH:mm:ss", Locale.getDefault())
        return outputDateFormat.format(date)
    } catch (e: ParseException) {
        e.printStackTrace()
    }
    return dateTime
}

fun startCountdownTimer(initialTimeMillis: Long = 10 * 1000L, context: Context, isOvertime: Boolean = false) {
    if (!::countdown.isInitialized) {
        Log.e("CountdownError", "Countdown TextView not initialized")
        return
    }

    var remainingTimeMillis = initialTimeMillis

    countdownHandler.post(object : Runnable {
        override fun run() {
            val hours = remainingTimeMillis / (60 * 60 * 1000)
            val minutes = (remainingTimeMillis % (60 * 60 * 1000)) / (60 * 1000)
            val seconds = (remainingTimeMillis % (60 * 1000)) / 1000

            countdown.text = String.format(
                "Time remaining: %02d:%02d:%02d",
                hours, minutes, seconds
            )

            if (remainingTimeMillis > 0) {
                remainingTimeMillis -= 1000 // Decrease by 1 second
                saveTimerState(context, remainingTimeMillis, false)
                countdownHandler.postDelayed(this, 1000) // Update every second
            } else {
                if (!isOvertime) {
                    timerPrompt.visibility = View.VISIBLE
                    timerPrompt.text = "Good Job! Auto Timed Out..."
                    requestButton.text = "Request OT"

                    // Set the timer finished flag to true
                    isTimerFinished = true
                    timeinButton.performClick()

                    Handler(context.mainLooper).postDelayed({
                        timerPrompt.text = "Go Overtime?"
                    }, 5000)
                }else {
                    requestButton.text = "Check OT status"
                    timerPrompt.visibility = View.VISIBLE
                    timerPrompt.text = "Good Job! Auto Timed Out..."

                    // Set the timer finished flag to true
                    isTimerFinished = true
                    timeinButton.performClick()

                    // Hide the "Time's up!" message after 3 seconds
                    Handler(context.mainLooper).postDelayed({
                        timerPrompt.text = "Nicely Done,OT finished!"
                    }, 5000)

                }

            }
        }
    })
}

fun stopCountdownTimer() {
    countdownHandler.removeCallbacksAndMessages(null)
    countdown.visibility = View.GONE
    isTimerFinished = false // Reset the timer finished flag
}
fun saveTimerState(context: Context,remainingTimeMillis: Long, isTimerFinished: Boolean) {
    val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putLong("remainingTimeMillis", remainingTimeMillis)
    editor.putBoolean("isTimerFinished", isTimerFinished)
    editor.apply()
}

fun restoreTimerState(context: Context): Pair<Long, Boolean> {
    val sharedPreferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE)
    val remainingTimeMillis = sharedPreferences.getLong("remainingTimeMillis", 0L)
    val isTimerFinished = sharedPreferences.getBoolean("isTimerFinished", false)
    return Pair(remainingTimeMillis, isTimerFinished)
}

fun isInsideGeoFence(assignedLatitude: Double, assignedLongitude: Double, currentLatitude: Double, currentLongitude : Double, radius : Double): Boolean {

    var distanceInMeters : FloatArray = floatArrayOf(0f)

    Location.distanceBetween(assignedLatitude, assignedLongitude, currentLatitude, currentLongitude, distanceInMeters)
    var placeholder : String = ""

    Log.d("MyCoords", "Distance: ${distanceInMeters[0]} meters")

    return distanceInMeters[0].toDouble() < radius
}


