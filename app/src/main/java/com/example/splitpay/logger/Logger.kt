package com.example.splitpay.logger

import android.util.Log

fun Any.logD(message: String) {
    Log.d(this::class.java.simpleName, message)
}

fun Any.logI(message: String) {
    Log.i(this::class.java.simpleName, message)
}

fun Any.logW(message: String) {
    Log.w(this::class.java.simpleName, message)
}

fun Any.logE(message: String, throwable: Throwable? = null) {
    Log.e(this::class.java.simpleName, message)
}