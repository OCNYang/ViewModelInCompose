package com.ocnyang.viewmodel.demo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform