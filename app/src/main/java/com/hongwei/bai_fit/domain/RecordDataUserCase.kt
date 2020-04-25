package com.hongwei.bai_fit.domain

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType


class RecordDataUserCase {
    companion object {
        private const val TAG = "bai-fit.domain"
    }

    fun subscribe(activity: Activity) {
        activity.run {
            GoogleSignIn.getLastSignedInAccount(this)?.let {
                Fitness.getRecordingClient(this, it)
                    .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                    .addOnSuccessListener { Log.i(TAG, "Successfully subscribed!") }
                    .addOnFailureListener { Log.i(TAG, "There was a problem subscribing.") }
            } ?: Log.w(TAG, "GoogleSignIn failure!")
        }
    }
}