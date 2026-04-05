# trobat-backend

Trobat es un sistema de reportes de geolocalización diseñado para ayudar en la búsqueda de niños desaparecidos. Este repositorio contiene el Backend desarrollado en Kotlin.

El objetivo principal es permitir que ciudadanos puedan subir avistamientos o fotos de forma anónima para colaborar con investigaciones activas de usuarios registrados.

🛠️ Tecnologías utilizadas
Lenguaje: Kotlin

Framework: Ktor (Motor Netty)

Base de Datos: MongoDB Atlas (NoSQL)

Serialización: Kotlinx Serialization

Gestor de Dependencias: Gradle (Kotlin DSL)

🏗️ Arquitectura del Proyecto
El proyecto sigue una estructura de plugins de Ktor para mantener el código organizado:

Serialization.kt: Configuración de ContentNegotiation para manejar JSON.

Routing.kt: Definición de los endpoints de la API.

Reporte.kt: Modelo de datos (Data Class) para los reportes.

Application.kt: Punto de entrada y conexión a la base de datos.