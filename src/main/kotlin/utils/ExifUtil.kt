package com.trobatapp.utils

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.GpsDirectory
import java.io.ByteArrayInputStream

object ExifUtil {
    fun extractGps(imageBytes: ByteArray): Pair<Double, Double>? {
        return try {
            val metadata = ImageMetadataReader.readMetadata(ByteArrayInputStream(imageBytes))
            val gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory::class.java) ?: return null
            val geoLocation = gpsDir.geoLocation ?: return null
            if (geoLocation.isZero) return null
            Pair(geoLocation.latitude, geoLocation.longitude)
        } catch (_: Exception) {
            null
        }
    }
}
