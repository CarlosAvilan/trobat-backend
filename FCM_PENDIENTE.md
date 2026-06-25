# FCM — Estado y pendientes

## Qué está hecho (branch: feature/fcm)

### Firebase Admin SDK integrado
- Dependencia agregada en `build.gradle.kts` (firebase-admin:9.3.0, excluye grpc-netty-shaded)
- Conflicto de Guava resuelto con `resolutionStrategy.force("com.google.guava:guava:32.1.3-jre")`
- `src/main/kotlin/config/FirebaseConfig.kt` — inicializa Firebase al arrancar desde `firebase-service-account.json` en resources
- `Application.kt` — llama `configureFirebase()` como primer paso en `module()`
- `firebase-service-account.json` está en `src/main/resources/` y está en `.gitignore`

### NotificationService
- `src/main/kotlin/services/NotificationService.kt`
- `sendToToken(token, notification)` — envía a un token, retorna messageId o null
- `sendToMultipleTokens(tokens, notification)` — chunked(500), retorna tokens fallidos
- Auto-limpieza de tokens inválidos (UNREGISTERED, INVALID_ARGUMENT) con `$pull` en MongoDB
- AndroidConfig priority HIGH, ApnsConfig sound "default"

### Endpoint de prueba (TEMPORAL — eliminar antes de producción)
- `POST /test-notificacion` en `Routing.kt`
- Body: `{ "token": "...", "titulo": "...", "mensaje": "..." }`
- Responde 200 con `messageId` o 500 si falla
- Verificado: el SDK llega a Firebase correctamente (tokens placeholder devuelven INVALID_ARGUMENT como esperado)

---

## Qué falta implementar

### 1. Disparar notificaciones en los eventos del sistema

Estos son los 3 eventos donde hay que llamar a `NotificationService`:

#### a) Nuevo reporte creado → notificar al oficial y agentes del caso
**Archivo:** `src/main/kotlin/routes/ReportesRoutes.kt`
**Dónde:** después de `reportes.insertOne(reporteDoc)` y el `$inc` de `total_reportes`
**Lógica:**
- Buscar el caso por `req.caso_id`
- Obtener `oficial_administrador_id` y `agentes_asignados` del caso
- Buscar los `fcm_tokens` de esos IDs en la colección `usuarios`
- Llamar `NotificationService.sendToMultipleTokens()`

#### b) Reporte validado → notificar al ciudadano si dejó contacto
**Archivo:** `src/main/kotlin/routes/ReportesRoutes.kt`
**Dónde:** en `PATCH /{id}/validar`, después del `updateOne` exitoso
**Lógica:** si el reporte tiene `datos_contacto.email` o similar, y el ciudadano tiene FCM tokens, notificar

#### c) Estado del caso cambiado → notificar a agentes asignados
**Archivo:** `src/main/kotlin/routes/CasosRoutes.kt`
**Dónde:** en `PATCH /{id}/estado`, después del `updateOne` exitoso
**Lógica:** obtener `agentes_asignados` del caso y sus `fcm_tokens`, llamar `sendToMultipleTokens()`

### 2. Probar con token real
- Necesitás la app Android con login funcionando
- El token FCM real se genera al loguear en la app y se guarda automáticamente via `POST /auth/login` (campo `fcm_token` opcional en el body)
- Una vez que tenés el token real, probás con `POST /test-notificacion`
- Después eliminás ese endpoint

### 3. Eliminar endpoint de prueba
- Borrar el bloque `// TODO: eliminar antes de producción` en `Routing.kt`
- Borrar los imports de `JsonObject` y `jsonPrimitive` que quedaron

---

## Datos útiles

- FCM tokens en Atlas: colección `usuarios`, campo `fcm_tokens: Array`
- Oficiales identificados por campo `email_institucional` (misma colección `usuarios`)
- `NotificationService` es un `object` — se llama directo sin instanciar
- El service usa `Dispatchers.IO` para las llamadas a Firebase (no bloquea coroutines de Ktor)
