package com.jainhardik120.locationtracker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.jainhardik120.locationtracker.ui.theme.LocationTrackerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocationTrackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    val permissionState = rememberMultiplePermissionsState(
                        permissions = listOf(
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                        )
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
    Button(onClick = {
        locationState.ask({}, {
            if (it is ResolvableApiException) {
                context.getActivity()?.let { it1 -> it.status.startResolutionForResult(it1, 1) }
            }
        })
    }) {
        Text(text = "Ask activation")
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

