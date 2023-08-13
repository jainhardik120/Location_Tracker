package com.jainhardik120.locationtracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jainhardik120.locationtracker.ui.theme.LocationTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationTrackerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val permissionState = rememberMultiplePermissionsState(
                        permissions = mutableListOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        ).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }

                        }
                    )
                    when {
                        permissionState.allPermissionsGranted -> {
                            App()
                        }

                        permissionState.shouldShowRationale -> {
                            Column {
                                Text(text = "Permission Denied")
                                Button(onClick = {
                                    scope.launch {
                                        permissionState.launchMultiplePermissionRequest()
                                    }
                                }) {
                                    Text(text = "Ask again")
                                }
                            }
                        }

                        else -> {
                            LaunchedEffect(key1 = permissionState, block = {
                                permissionState.launchMultiplePermissionRequest()
                            })
                        }

                    }
                }
            }
        }
    }


}

@Composable
fun App() {
    val context = LocalContext.current
    val locationState = rememberLocationState(context = LocalContext.current)
//    Button(onClick = {
//        locationState.ask({}, {
//            if (it is ResolvableApiException) {
//                context.getActivity()?.let { it1 -> it.status.startResolutionForResult(it1, 1) }
//            }
//        })
//    }) {
//        Text(text = "Ask activation")
//    }
    Column {
        val singapore = LatLng(1.35, 103.87)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(singapore, 10f)
        }
        GoogleMap(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            Marker(
                state = MarkerState(position = singapore),
                title = "Singapore",
                snippet = "Marker in Singapore"
            )
        }
        Row {


        Button(onClick = {
            context.getActivity()
                ?.startService(Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.START_ACTION
                })
        }) {
            Text(text = "Start Service")
        }

        Button(onClick = {
            context.getActivity()
                ?.startService(Intent(context, LocationTrackingService::class.java).apply {
                    action = LocationTrackingService.STOP_ACTION
                })
        }) {
            Text(text = "Stop Service")
        }
        }
    }
}

class LocationState(
    context: Context
) {
    private val locationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).build()
    private val builder =
        LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
    private val client = LocationServices.getSettingsClient(context)
    fun ask(
        onSuccess: (LocationSettingsResponse) -> Unit,
        onError: (Exception) -> Unit
    ) {
        client.checkLocationSettings(builder).addOnSuccessListener {
            it?.let(onSuccess)
        }.addOnFailureListener {
            it.let(onError)
        }
    }
}

@Composable
fun rememberLocationState(
    context: Context
): LocationState = remember {
    LocationState(context)
}

fun Context.getActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}