package com.trobatapp.service

import com.google.firebase.cloud.StorageClient
import java.net.URLEncoder
import java.util.UUID

object FirebaseStorageService {

    private val bucketName: String
        get() = System.getenv("FIREBASE_STORAGE_BUCKET") ?: "trobat-40cea.firebasestorage.app"

    fun uploadImage(imageBytes: ByteArray, contentType: String = "image/jpeg"): String {
        val fileName = "casos/${UUID.randomUUID()}.jpg"
        val downloadToken = UUID.randomUUID().toString()

        val bucket = StorageClient.getInstance().bucket(bucketName)
        val blob = bucket.create(fileName, imageBytes, contentType)

        blob.toBuilder()
            .setMetadata(mapOf("firebaseStorageDownloadTokens" to downloadToken))
            .build()
            .update()

        val encodedPath = URLEncoder.encode(fileName, "UTF-8")
        return "https://firebasestorage.googleapis.com/v0/b/$bucketName/o/$encodedPath?alt=media&token=$downloadToken"
    }
}
