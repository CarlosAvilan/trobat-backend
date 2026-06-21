package com.trobatapp.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification as FcmNotification

object FcmService {

    const val TOPIC_ALERTAS = "alertas-trobat"

    fun sendToTopic(title: String, body: String): String {
        val message = Message.builder()
            .setNotification(
                FcmNotification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .setTopic(TOPIC_ALERTAS)
            .build()
        return FirebaseMessaging.getInstance().send(message)
    }
}
