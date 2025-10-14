package com.opacity.example

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.opacitylabs.opacitycore.OpacityCore

class GitHubProfileWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "🚀 Background task started at $startTime")

        return try {
            OpacityCore.get("github:profile", null)
            
            val endTime = System.currentTimeMillis()
            val duration = (endTime - startTime) / 1000.0
            Log.d(TAG, "✅ SUCCESS at $endTime: GitHub profile fetched")
            Log.d(TAG, "⏱️ Duration: $duration seconds")
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "❌ FAILURE at ${System.currentTimeMillis()}: Failed to fetch GitHub profile", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "GitHubProfileWorker"
    }
}