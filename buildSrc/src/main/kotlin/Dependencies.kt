@file:Suppress("PublicApiImplicitType")

object Versions {
    const val kotlin = "1.3.31"
    const val ktlint = "8.0.0"
    const val gradle_versions = "0.21.0"
    const val jgitver = "0.9.0"
    const val junit = "5.5.0-M1"
    const val kluent = "1.50"
}

object Libs {
    const val junit_jupiter_api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit}"
    const val junit_jupiter_engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit}"
    const val kluent = "org.amshove.kluent:kluent:${Versions.kluent}"
}
