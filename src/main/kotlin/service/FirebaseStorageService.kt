package com.trobatapp.service

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.firebase.cloud.StorageClient
import java.net.URLEncoder
import java.util.UUID

object FirebaseStorageService {

    private val bucketName = "trobat-40cea.firebasestorage.app"

    fun uploadImage(imageBytes: ByteArray, contentType: String = "image/jpeg"): String {
        val fileName = "reportes/${UUID.randomUUID()}.jpg"
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
