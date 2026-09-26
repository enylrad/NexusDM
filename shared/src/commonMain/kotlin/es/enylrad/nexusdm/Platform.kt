package es.enylrad.nexusdm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform