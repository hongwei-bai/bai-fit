package com.hongwei.bai_fit.domain

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.internal.zzg.getSignInResultFromIntent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener


class GoogleFitConnector {
    companion object {
        private const val TAG = "bai-fit.domain"

        private const val GOOGLE_SIGN_IN = 123
        private const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 234

        private const val PERMISSIONS_REQUEST_CODE_FINE_LOCATION = 456
        private const val PERMISSIONS_REQUEST_CODE_ACTIVITY_RECOGNITION = 678
    }

    private var account: GoogleSignInAccount? = null

    private val fitnessOptions = FitnessOptions.builder() //步数
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE) //距离
        .addDataType(DataType.TYPE_DISTANCE_DELTA, FitnessOptions.ACCESS_WRITE)
        .build()

    fun connect(activity: Activity) {
        connectGoogle(activity)
    }

    private fun connectGoogle(activity: Activity) {
        try {
            //如果已经登录过google账号。则可以拿到账号
            account = GoogleSignIn.getLastSignedInAccount(activity)
            if (account == null) {
                //没有。则要登录
                signIn(activity)
            } else {
                //有，则要订阅
                subscribe(activity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun signIn(activity: Activity) {
        val gso =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        val mGoogleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = mGoogleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, GOOGLE_SIGN_IN)
    }

    private fun handleSignInResult(activity: Activity, requestCode: Int, data: Intent?) {
        if (requestCode != GOOGLE_SIGN_IN) {
            return
        }
        try {
            val result =
                getSignInResultFromIntent(data)
            if (result!!.isSuccess) {
                account = getSignedInAccountFromIntent(data).getResult(
                    ApiException::class.java
                )
                subscribe(activity)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    private fun subscribe(activity: Activity) {
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            Log.w(TAG, "subscribe failed, getLastSignedInAccount: $account")
            return
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.w(TAG, "No permission for ${Manifest.permission.ACCESS_FINE_LOCATION}")

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_CODE_FINE_LOCATION
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.w(TAG, "No permission for ${Manifest.permission.ACTIVITY_RECOGNITION}")

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACTIVITY_RECOGNITION)
            ) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), PERMISSIONS_REQUEST_CODE_ACTIVITY_RECOGNITION)

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            Log.w(TAG, "No permission for account/fitnessOptions: $account/$fitnessOptions, proceed to request.")
            GoogleSignIn.requestPermissions(
                activity,
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                account,
                fitnessOptions
            )
        } else {
            Log.d(TAG, "Permission check passed for account/fitnessOptions: $account/$fitnessOptions.")
            subscriptionData(activity)
        }
    }

    private fun subscriptionData(activity: Activity) {
        subscribe(activity, DataType.TYPE_STEP_COUNT_DELTA)
        subscribe(activity, DataType.TYPE_DISTANCE_DELTA)
    }

    private fun subscribe(activity: Activity, dataType: DataType) {
        GoogleSignIn.getLastSignedInAccount(activity)?.let {
            Fitness.getRecordingClient(activity, it)
                .subscribe(dataType)
                .addOnSuccessListener {
                    Log.d(TAG, "subscribe $dataType success: $it")
                }
                .addOnFailureListener {
                    Log.d(TAG, "subscribe $dataType failure: $it")
                }
        }
    }
}