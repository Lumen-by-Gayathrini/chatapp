package com.gayathrini.chatapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. [@HiltAndroidApp] bootstraps the Hilt dependency graph.
 */
@HiltAndroidApp
class ChatApplication : Application()
