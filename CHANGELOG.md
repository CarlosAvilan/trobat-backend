# Changelog — Trobat Backend

## Sesión 2026-06-19

---

### 1. Git pull — rama `develop`

Se incorporaron cambios del repositorio remoto. Archivos actualizados:

- `models/Usuario.kt`
- `plugins/HTTP.kt`
- `routes/AuthRoutes.kt`
- `service/AuthService.kt`
- `service/IAuthService.kt`

---

### 2. Campo `imagen` en `Desaparecido`

**Archivos modificados:** `models/Caso.kt`, `routes/CasosRoutes.kt`

Se agregó el campo `imagen: String` al modelo `Desaparecido`. La imagen corresponde a una foto del desaparecido que se sube al bucket externamente; el cliente envía la URL resultante en el body del `POST /casos`.

El campo es **obligatorio**.

**Body esperado en `POST /casos`:**
```json
{
  "oficial_administrador_id": "...",
  "desaparecido": {
    "nombre": "Juan Pérez",
    "descripcion": "...",
    "edad": 30,
    "imagen": "https://bucket.url/foto.jpg",
    "ultima_ubicacion_oficial": { ... }
  },
  "representante_externo": { ... }
}
```

**Cambios internos:**
- `Desaparecido.imagen: String` agregado al data class.
- En `CasosRoutes.kt`: se guarda con `.append("imagen", req.desaparecido.imagen)` dentro de `desaparecidoDoc`.
- En `toCasoResponse()`: se lee con `desDoc.getString("imagen") ?: ""`.

---

### 3. Campo `edad` en `Desaparecido`

**Archivos modificados:** `models/Caso.kt`, `routes/CasosRoutes.kt`

Se agregó el campo `edad: Int` al modelo `Desaparecido`.

El campo es **obligatorio**.

**Cambios internos:**
- `Desaparecido.edad: Int` agregado al data class.
- En `CasosRoutes.kt`: se guarda con `.append("edad", req.desaparecido.edad)` dentro de `desaparecidoDoc`.
- En `toCasoResponse()`: se lee con `desDoc.getInteger("edad") ?: 0`.

---

### 4. Nuevo endpoint — Usuarios Reportantes (mobile)

**Archivos creados:**
- `models/UsuarioReportante.kt`
- `routes/UsuariosReportantesRoutes.kt`

**Archivos modificados:**
- `Application.kt`

#### Colección MongoDB
Nombre: `Usuario_reportante`  
Completamente separada de la colección `usuarios` (oficiales). No hay relación entre ambas.

#### Modelos (`models/UsuarioReportante.kt`)

```kotlin
data class InformacionPersonal(
    val dni: String,
    val nombre_completo: String,
    val telefono: String
)

data class RegistroReportanteRequest(
    val mail: String,
    val contrasenia: String,
    val informacion_personal: InformacionPersonal
)

data class LoginReportanteRequest(
    val mail: String,
    val contrasenia: String
)

data class UsuarioReportanteResponse(
    val id: String,
    val mail: String,
    val informacion_personal: InformacionPersonal
)
```

#### Endpoints

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/usuarios-reportantes/registro` | No | Registra un nuevo usuario mobile |
| `POST` | `/usuarios-reportantes/login` | No | Login, devuelve JWT |
| `GET` | `/usuarios-reportantes/perfil` | JWT | Devuelve el perfil del usuario autenticado |

#### `POST /usuarios-reportantes/registro`

**Body:**
```json
{
  "mail": "usuario@mail.com",
  "contrasenia": "secreta123",
  "informacion_personal": {
    "dni": "12345678",
    "nombre_completo": "Juan Pérez",
    "telefono": "1122334455"
  }
}
```

**Respuestas:**
- `201 Created` — `{ "id": "...", "mensaje": "Usuario registrado exitosamente" }`
- `400 Bad Request` — campos obligatorios faltantes
- `409 Conflict` — el mail ya está registrado

#### `POST /usuarios-reportantes/login`

**Body:**
```json
{
  "mail": "usuario@mail.com",
  "contrasenia": "secreta123"
}
```

**Respuesta `200 OK`:**
```json
{
  "token": "<jwt>",
  "tipo": "reportante",
  "id": "...",
  "nombre": "Juan Pérez"
}
```

- `401 Unauthorized` — credenciales inválidas

#### `GET /usuarios-reportantes/perfil`

Requiere header `Authorization: Bearer <token>`.

**Respuesta `200 OK`:**
```json
{
  "id": "...",
  "mail": "usuario@mail.com",
  "informacion_personal": {
    "dni": "12345678",
    "nombre_completo": "Juan Pérez",
    "telefono": "1122334455"
  }
}
```

- `401 Unauthorized` — token inválido o ausente
- `404 Not Found` — ID del token no existe en `Usuario_reportante`

#### Seguridad y separación de colecciones

- La contraseña se hashea con **BCrypt** antes de guardarse (`contrasenia_hash`).
- El JWT generado en login lleva `role: "reportante"` y es válido por **24 horas**.
- La separación entre usuarios reportantes y oficiales es por colección: cualquier token válido que intente acceder a `/perfil` pero cuyo ID no exista en `Usuario_reportante` recibe `404`, sin mezclas.

#### Cambios en `Application.kt`

```kotlin
// Nueva colección registrada
val usuariosReportantes = database.getCollection<Document>("Usuario_reportante")

// Nueva ruta registrada en Application.module()
configureUsuariosReportantesRouting()
```

---

### 5. Campo `ubicacion_label` — geocodificación inversa

**Archivos creados:**
- `utils/GeocodingUtil.kt`

**Archivos modificados:**
- `models/Caso.kt`
- `models/ReporteCaso.kt`
- `models/ReporteRespuesta.kt`
- `routes/CasosRoutes.kt`
- `routes/ReportesRoutes.kt`

#### Lógica general

Se agregó el campo `ubicacion_label: String?` en todos los modelos que devuelven coordenadas, sin reemplazar los campos existentes (`coordinates`, `lat`, `lng`).

La traducción de coordenadas a texto se realiza **una sola vez al insertar** el documento, llamando a la API de Nominatim (OpenStreetMap). Las lecturas posteriores simplemente leen el campo guardado en Mongo, sin llamadas externas.

Ejemplo de valor: `"Av. Corrientes 1234, San Nicolás, Buenos Aires, Argentina"`

Documentos anteriores a este cambio devuelven `ubicacion_label: null`.

#### `utils/GeocodingUtil.kt` (nuevo)

Utilidad suspendible que llama a:
```
GET https://nominatim.openstreetmap.org/reverse?format=json&lat={lat}&lon={lon}&accept-language=es
```
- Timeout: 5 segundos (conexión y lectura).
- Devuelve el campo `display_name` del response, o `null` si la llamada falla.
- No requiere API key.

#### Modelos actualizados

| Modelo | Campo agregado | Junto a |
|--------|---------------|---------|
| `Desaparecido` | `ubicacion_label: String? = null` | `ultima_ubicacion_oficial` |
| `ReporteCasoResponse` | `ubicacion_label: String? = null` | `location` |
| `ReporteRespuesta` | `ubicacion_label: String? = null` | `lat` / `lng` |

#### Comportamiento al insertar

**`POST /reportes`** — geocodifica `location` antes de guardar:
```kotlin
val ubicacionLabel = GeocodingUtil.reverseGeocode(lat = req.location.latitud, lon = req.location.longitud)
// se guarda como "ubicacion_label" en el documento
```

**`POST /casos`** — geocodifica `ultima_ubicacion_oficial` si está presente:
```kotlin
val ubicacionLabel = req.desaparecido.ultima_ubicacion_oficial?.let { ub ->
    GeocodingUtil.reverseGeocode(lat = ub.latitud, lon = ub.longitud)
}
// se guarda como "ubicacion_label" dentro del subdocumento "desaparecido"
```

---

### 6. Fix — referencia residual a `imagen` en `CasosRoutes.kt`

**Archivo modificado:** `routes/CasosRoutes.kt`

Al mover `imagen` de `CrearCasoRequest` a `Desaparecido`, quedó una línea huérfana en `casoDoc` que referenciaba `req.imagen`. Esto causaba un error de compilación `Unresolved reference: imagen` al levantar el servidor.

**Línea eliminada:**
```kotlin
.append("imagen", req.imagen)  // ← removida de casoDoc (ya se guarda dentro de desaparecidoDoc)
```

---

### 7. Colección Postman y cURLs

**Archivos creados:**
- `trobat.postman_collection.json` — reescritura completa (la versión anterior estaba desactualizada)
- `curls.md` — cURLs de todos los endpoints

La colección Postman reemplaza la versión anterior que tenía rutas inexistentes (`/casos/activos`, `/seed/casos`). Ambos archivos cubren los mismos 25 endpoints organizados en 7 secciones:

| Sección | Endpoints |
|---------|-----------|
| Auth | registro, login, login oficial, reset password, logout |
| Casos | listar, obtener, crear, actualizar estado, agregar/quitar agente |
| Reportes | listar por caso, crear, listar, obtener, validar |
| Usuarios | obtener por ID, FCM token |
| Oficiales | listar, obtener por ID, FCM token |
| Usuarios Reportantes | registro, login, perfil |
| Legacy | health check, ver-reportes, reportes-cercanos, crear-reporte multipart |

Variables de colección: `base_url`, `token`, `caso_id`, `reporte_id`, `usuario_id`, `oficial_id`, `agente_id`, `reportante_id`.

---

### 8. Script de prueba — `test_crear_caso.py`

**Archivo creado:** `test_crear_caso.py`

Script Python que prueba el flujo completo de creación de un caso:
1. Login como oficial (`POST /auth/login/oficial`) para obtener el JWT.
2. Crea un caso con ese token (`POST /casos`).
3. Imprime la respuesta con el ID del caso creado.

**Uso:**
```bash
py -3.11 test_crear_caso.py
```

---

### 9. Limpieza — modelo `Reporte` legacy eliminado

**Archivos modificados:**
- `routes/Routing.kt` — eliminadas rutas legacy (`/ver-reportes`, `/reportes-cercanos`, `/crear-reporte multipart`). Solo queda `GET /`
- `Application.kt` — eliminada la colección `coleccion` (`reportes_fotos`) y el import de `Reporte`

`Reporte.kt` y `ReporteRespuesta.kt` quedan como archivos muertos sin referencias.

---

### 10. Eliminación de `prioridad_policial` en reportes

**Archivos modificados:** `models/ReporteCaso.kt`, `routes/ReportesRoutes.kt`

Se eliminó el campo `prioridad_policial` de `CrearReporteRequest` y `ReporteCasoResponse`. El estado de un reporte queda determinado únicamente por `validated: Boolean`, actualizable vía `PATCH /reportes/:id/validar`.

---

### 11. Renombrado completo Spanglish → inglés

**Archivos modificados:** todos los modelos y routes.

Todos los campos almacenados en MongoDB y devueltos en los responses fueron renombrados a inglés.

| Campo anterior | Campo nuevo |
|----------------|-------------|
| `desaparecido` | `missing_person` |
| `representante_externo` | `external_contact` |
| `oficial_administrador_id` | `admin_officer_id` |
| `agentes_asignados` | `assigned_agents` |
| `estado` | `status` |
| `"investigacion_activa"` | `"active_investigation"` |
| `"resuelto"` | `"resolved"` |
| `"cerrado"` | `"closed"` |
| `"suspendido"` | `"suspended"` |
| `total_reportes` | `total_reports` |
| `fecha_creacion` | `created_at` |
| `nombre` | `name` |
| `descripcion` | `description` |
| `edad` | `age` |
| `imagen` | `image` |
| `ultima_ubicacion_oficial` | `last_known_location` |
| `ubicacion_label` | `location_label` |
| `caso_id` | `case_id` |
| `metadata_seguridad` | `security_metadata` |
| `anonimo` | `anonymous` |
| `datos_contacto` | `contact_info` |
| `telefono` | `phone` |
| `validado` | `validated` |
| `mail` | `email` |
| `contrasenia` | `password` |
| `contrasenia_hash` | `password_hash` |
| `informacion_personal` | `personal_info` |
| `nombre_completo` | `full_name` |
| `dni` | `national_id` |

También se renombraron las clases `MetadataSeguridad` → `SecurityMetadata` y `DatosContacto` → `ContactInfo` en `ReporteCaso.kt`.

> Los documentos existentes en MongoDB con los nombres anteriores devolverán `null` en los campos renombrados. Se recomienda limpiar la base de datos de desarrollo.

El script `test_crear_caso.py` también fue actualizado con los nuevos nombres de campos.

---

### 12. Nuevo endpoint — `GET /casos/cercanos`

**Archivos modificados:**
- `models/Caso.kt` — nuevos modelos `CasoCercanoResponse` y `CasosCercanosPaginados`
- `routes/CasosRoutes.kt` — nuevo endpoint
- `Application.kt` — índice 2dsphere sobre `missing_person.last_known_location`

#### Descripción

Devuelve casos cercanos a la ubicación del usuario usando el índice geoespacial de MongoDB. El radio es configurable desde el frontend (5, 10, 20 km) y los resultados están paginados. Cada caso incluye la distancia en km al punto enviado.

#### Endpoint

```
GET /casos/cercanos?lat=-34.6037&lng=-58.3816&radio=5&page=0&limit=20
```

| Parámetro | Tipo | Default | Descripción |
|-----------|------|---------|-------------|
| `lat` | Double | requerido | Latitud del usuario |
| `lng` | Double | requerido | Longitud del usuario |
| `radio` | Double | `5` | Radio en km (rango: 1–50) |
| `page` | Int | `0` | Número de página |
| `limit` | Int | `20` | Resultados por página (máx. 100) |

#### Response `200 OK`

```json
{
  "data": [
    {
      "caso": {
        "id": "...",
        "missing_person": { "name": "María García", ... },
        "status": "active_investigation",
        "distance_km": 1.23
      },
      "distance_km": 1.23
    }
  ],
  "total": 8,
  "page": 0,
  "limit": 20,
  "radius_km": 5.0,
  "hasMore": false
}
```

#### Implementación

Usa el operador `$geoNear` de MongoDB en un pipeline de agregación. Se ejecutan dos pipelines: uno con `$count` para obtener el total, y otro con `$skip` + `$limit` para la página solicitada.

```kotlin
val geoNearStage = Document("\$geoNear", Document()
    .append("near", Document("type", "Point").append("coordinates", listOf(lng, lat)))
    .append("distanceField", "distance_meters")
    .append("maxDistance", radiusKm * 1000)
    .append("spherical", true)
    .append("key", "missing_person.last_known_location")
)
```

#### Índice creado al iniciar el servidor

```kotlin
casos.createIndex(Indexes.geo2dsphere("missing_person.last_known_location"))
```

> Casos sin `last_known_location` cargada no aparecen en los resultados de este endpoint.
