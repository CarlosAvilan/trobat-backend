package com.trobatapp.services

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.AndroidNotification
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.trobatapp.usuarios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

data class PushNotification(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)

object NotificationService {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    suspend fun sendToToken(token: String, notification: PushNotification): String? {
        return withContext(Dispatchers.IO) {
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(notification.title)
                            .setBody(notification.body)
                            .build()
                    )
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(
                                AndroidNotification.builder()
                                    .setTitle(notification.title)
                                    .setBody(notification.body)
                                    .build()
                            )
                            .build()
                    )
                    .setApnsConfig(
                        ApnsConfig.builder()
                            .setAps(Aps.builder().setSound("default").build())
                            .build()
                    )
                    .putAllData(notification.data)
                    .build()

                FirebaseMessaging.getInstance().send(message)
            } catch (e: FirebaseMessagingException) {
                logger.warn("Error enviando notificación a token $token: ${e.messagingErrorCode}")
                null
            }
        }
    }

    suspend fun sendToMultipleTokens(tokens: List<String>, notification: PushNotification): List<String> {
        if (tokens.isEmpty()) return emptyList()

        val tokensFallidos = mutableListOf<String>()

        tokens.chunked(500).forEach { chunk ->
            withContext(Dispatchers.IO) {
                try {
                    val message = MulticastMessage.builder()
                        .addAllTokens(chunk)
                        .setNotification(
                            Notification.builder()
                                .setTitle(notification.title)
                                .setBody(notification.body)
                                .build()
                        )
                        .setAndroidConfig(
                            AndroidConfig.builder()
                                .setPriority(AndroidConfig.Priority.HIGH)
                                .build()
                        )
                        .setApnsConfig(
                            ApnsConfig.builder()
                                .setAps(Aps.builder().setSound("default").build())
                                .build()
                        )
                        .putAllData(notification.data)
                        .build()

                    val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)

                    response.responses.forEachIndexed { index, sendResponse ->
                        if (!sendResponse.isSuccessful) {
                            val errorCode = sendResponse.exception?.messagingErrorCode
                            when (errorCode) {
                                MessagingErrorCode.UNREGISTERED,
                                MessagingErrorCode.INVALID_ARGUMENT -> tokensFallidos.add(chunk[index])
                                else -> logger.warn("Error enviando a ${chunk[index]}: $errorCode")
                            }
                        }
                    }
                } catch (e: FirebaseMessagingException) {
                    logger.error("Error en multicast: ${e.message}")
                }
            }
        }

        if (tokensFallidos.isNotEmpty()) {
            limpiarTokensInvalidos(tokensFallidos)
        }

        return tokensFallidos
    }

    private suspend fun limpiarTokensInvalidos(tokens: List<String>) {
        try {
            usuarios.updateMany(
                Filters.`in`("fcm_tokens", tokens),
                Updates.pullAll("fcm_tokens", tokens)
            )
            logger.info("Eliminados ${tokens.size} tokens inválidos de la BD.")
        } catch (e: Exception) {
            logger.error("Error limpiando tokens inválidos: ${e.message}")
        }
    }
}
