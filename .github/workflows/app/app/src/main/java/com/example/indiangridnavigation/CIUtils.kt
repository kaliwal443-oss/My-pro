package com.example.indiangridnavigation

import android.util.Log

object CIUtils {
    private const val TAG = "CIUtils"
    
    fun isRunningOnCI(): Boolean {
        return BuildConfig.CI_BUILD == "true" || BuildConfig.IS_CI
    }
    
    fun logBuildInfo() {
        if (isRunningOnCI()) {
            Log.i(TAG, "🔄 Running on CI Environment")
            Log.i(TAG, "📱 Build Time: ${BuildConfig.BUILD_TIME}")
            Log.i(TAG, "🔗 Git SHA: ${BuildConfig.GIT_SHA}")
            Log.i(TAG, "🏗️ Build Type: ${BuildConfig.BUILD_TYPE}")
        }
    }
    
    fun setupCIMode() {
        if (isRunningOnCI()) {
            // Disable certain features in CI mode
            Log.i(TAG, "🔧 CI Mode Enabled - Adjusting app behavior")
            
            // You can add CI-specific configurations here
            // For example, use mock location data, disable animations, etc.
        }
    }
}
