# Trobat Backend — Documentación

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Framework | Ktor 2.3.10 (Netty) |
| Base de datos | MongoDB Atlas |
| Serialización | Kotlinx Serialization |
| Autenticación | JWT (24h de expiración) |
| Hashing de contraseñas | BCrypt |
| Puerto | **8081** (8080 ocupado por Apache local) |

---

## Estructura del proyecto

```
src/main/kotlin/
├── Application.kt              ← Entry point + colecciones MongoDB
├── models/
│   ├── Caso.kt                 ← Modelos de CASOS
│   ├── ReporteCaso.kt          ← Modelos de REPORTES (vinculados a casos)
│   ├── Reporte.kt              ← Modelo legacy (reportes_fotos)
│   ├── ReporteRespuesta.kt     ← DTO legacy
│   └── Usuario.kt              ← Modelos de auth y usuarios
├── plugins/
│   ├── Auth.kt                 ← Configuración JWT
│   ├── HTTP.kt                 ← CORS
│   └── Serialization.kt        ← JSON
├── routes/
│   ├── AuthRoutes.kt           ← /auth/*
│   ├── CasosRoutes.kt          ← /casos/*
│   ├── ReportesRoutes.kt       ← /reportes/*
│   ├── UsuariosRoutes.kt       ← /usuarios/* y /oficiales/*
│   └── Routing.kt              ← Legacy (reportes_fotos)
└── utils/
    ├── JwtUtil.kt              ← Generación de tokens
    └── RouteUtils.kt           ← Helper verificarRol()
```

---

## Colecciones en MongoDB

### ¿Qué cambió respecto a las colecciones originales?

| Colección | Estado | Detalle |
|---|---|---|
| `reportes_fotos` | **Sin cambios** | Colección original, sigue funcionando igual |
| `casos` | **Nueva** | Se crea automáticamente al insertar el primer caso |
| `reportes` | **Nueva** | Reportes vinculados a casos (distinto de `reportes_fotos`) |
| `usuarios` | **Nueva** | Solo ciudadanos. El backend espera los mismos campos que ya tenés |
| `oficiales` | **Nueva** | Solo policías. El backend espera los mismos campos que ya tenés |

> **Importante:** Las colecciones `usuarios` y `oficiales` estaban mezcladas en tu vista original. El backend las trata como **dos colecciones separadas**. Si tus documentos existentes están en una sola colección, necesitás migrarlos.

---

### Esquema esperado por colección

#### `casos`
```json
{
  "_id": ObjectId,
  "oficial_administrador_id": ObjectId,
  "agentes_asignados": [ObjectId],
  "desaparecido": {
    "nombre": "string",
    "descripcion": "string",
    "ultima_ubicacion_oficial": {
      "type": "Point",
      "coordinates": [lng, lat]
    }
  },
  "representante_externo": {
    "nombre": "string",
    "email": "string",
    "telefono": "string"
  },
  "estado": "investigacion_activa | resuelto | cerrado | suspendido",
  "total_reportes": 0,
  "fecha_creacion": Date
}
```

#### `reportes`
```json
{
  "_id": ObjectId,
  "caso_id": ObjectId,
  "location": {
    "type": "Point",
    "coordinates": [lng, lat]
  },
  "timestamp": Date,
  "prioridad_policial": false,
  "descripcion": "string",
  "photo_url": "string | null",
  "metadata_seguridad": {
    "anonimo": true
  },
  "datos_contacto": {
    "nombre": "string | null",
    "telefono": "string | null",
    "email": "string | null"
  },
  "validado": false
}
```

#### `usuarios`
```json
{
  "_id": ObjectId,
  "id": "UUID string",
  "name": "string",
  "email": "string",
  "password_hash": "$2a$...",
  "fcm_tokens": [],
  "role": "user",
  "created_at": "ISO string",
  "is_verified": false
}
```

#### `oficiales`
```json
{
  "_id": ObjectId,
  "nombre": "string",
  "email_institucional": "string",
  "hash_contrasenia": "$2b$...",
  "rango": "string",
  "legajo": "string",
  "fcm_tokens": []
}
```

> Los oficiales **no se registran por API**. Se cargan directamente en la BD con la contraseña ya hasheada en BCrypt.

---

## Endpoints

**Base URL:** `http://localhost:8081`

### Autenticación

#### `POST /auth/registro`
Registra un nuevo ciudadano. No requiere token.

**Request:**
```json
{
  "name": "Lara Pou",
  "email": "lara@uade.edu.ar",
  "password": "miPassword123"
}
```

**Response `201`:**
```json
{
  "id": "68a1f8...",
  "mensaje": "Usuario registrado exitosamente"
}
```

**Errores:** `400` campos vacíos, `409` email ya registrado.

---

#### `POST /auth/login`
Login de ciudadano. Devuelve JWT.

**Request:**
```json
{
  "email": "lara@uade.edu.ar",
  "password": "miPassword123"
}
```

**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "user",
  "id": "68a1f8...",
  "nombre": "Lara Pou"
}
```

**Errores:** `400` cuerpo inválido, `401` credenciales incorrectas.

---

#### `POST /auth/login/oficial`
Login de oficial policial. Devuelve JWT.

**Request:**
```json
{
  "email_institucional": "juan.perez@policia.gob.ar",
  "password": "miPassword123"
}
```

**Response `200`:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "oficial",
  "id": "6a1f8a...",
  "nombre": "Juan Pérez"
}
```

---

### Casos

#### `GET /casos` — Público
Lista todos los casos.

**Response `200`:**
```json
[
  {
    "id": "6a1f8a...",
    "oficial_administrador_id": "6a1f8a...",
    "agentes_asignados": ["6a1f8a...", "6a1f8b..."],
    "desaparecido": {
      "nombre": "Mia García",
      "descripcion": "Niña de 8 años...",
      "ultima_ubicacion_oficial": {
        "type": "Point",
        "coordinates": [-58.4173, -34.6158]
      }
    },
    "representante_externo": {
      "nombre": "Jorge García",
      "email": "jorge@email.com",
      "telefono": "1145678901"
    },
    "estado": "investigacion_activa",
    "total_reportes": 3,
    "fecha_creacion": "2026-06-01T10:30:00Z"
  }
]
```

---

#### `GET /casos/{id}` — Público
Obtiene un caso por su ID de MongoDB.

---

#### `POST /casos` — Solo oficial 🔒
Crea un nuevo caso.

**Headers:** `Authorization: Bearer {token}`

**Request:**
```json
{
  "oficial_administrador_id": "6a1f8ae73d256e14d8bfb08e",
  "agentes_asignados": ["6a1f8ae73d256e14d8bfb08f"],
  "desaparecido": {
    "nombre": "Mia García",
    "descripcion": "Niña de 8 años, cabello negro...",
    "ultima_ubicacion_oficial": {
      "type": "Point",
      "coordinates": [-58.4173, -34.6158]
    }
  },
  "representante_externo": {
    "nombre": "Jorge García",
    "email": "jorge@email.com",
    "telefono": "1145678901"
  }
}
```

**Response `201`:**
```json
{ "id": "68a2c3...", "mensaje": "Caso creado exitosamente" }
```

---

#### `PATCH /casos/{id}/estado` — Solo oficial 🔒
Actualiza el estado del caso.

**Request:**
```json
{ "estado": "resuelto" }
```
Valores válidos: `investigacion_activa`, `resuelto`, `cerrado`, `suspendido`

---

#### `POST /casos/{id}/agentes/{agenteId}` — Solo oficial 🔒
Agrega un agente asignado al caso.

---

#### `DELETE /casos/{id}/agentes/{agenteId}` — Solo oficial 🔒
Remueve un agente del caso.

---

### Reportes

#### `GET /casos/{id}/reportes` — Público
Lista todos los reportes de un caso específico.

---

#### `POST /reportes` — Público (anónimo)
Crea un nuevo avistamiento. No requiere autenticación.

**Request:**
```json
{
  "caso_id": "6a1f8ae73d256e14d8bfb091",
  "location": {
    "type": "Point",
    "coordinates": [-58.3816, -34.6037]
  },
  "descripcion": "Vi a una niña con esas características en Av. Corrientes",
  "photo_url": "https://...",
  "prioridad_policial": false,
  "metadata_seguridad": { "anonimo": true },
  "datos_contacto": {
    "nombre": null,
    "telefono": null,
    "email": null
  }
}
```

**Response `201`:**
```json
{ "id": "68b3d4...", "mensaje": "Reporte creado exitosamente" }
```

> Al crear un reporte se incrementa automáticamente `total_reportes` en el caso correspondiente.

---

#### `GET /reportes` — Autenticado 🔒
Lista reportes. Acepta filtro opcional por caso.

**Query param opcional:** `?caso_id=6a1f8ae73d256e14d8bfb091`

---

#### `GET /reportes/{id}` — Autenticado 🔒
Obtiene un reporte por ID.

---

#### `PATCH /reportes/{id}/validar` — Solo oficial 🔒
Valida o invalida un reporte.

**Request:**
```json
{ "validado": true }
```

---

### Usuarios

#### `GET /usuarios/{id}` — Autenticado 🔒
Devuelve el perfil del ciudadano. No expone `password_hash`.

**Response `200`:**
```json
{
  "id": "69e049...",
  "name": "Lara Pou",
  "email": "lara@uade.edu.ar",
  "role": "user",
  "is_verified": false,
  "created_at": "2026-04-16T02:28:51Z"
}
```

---

#### `POST /usuarios/{id}/fcm-token` — Autenticado 🔒
Registra un token de push notification (Firebase Cloud Messaging).

**Request:**
```json
{ "fcm_token": "dGhpcyBpcyBhIHRva2Vu..." }
```

---

#### `GET /oficiales` — Solo oficial 🔒
Lista todos los oficiales registrados.

---

#### `GET /oficiales/{id}` — Solo oficial 🔒
Perfil de un oficial. No expone `hash_contrasenia`.

---

#### `POST /oficiales/{id}/fcm-token` — Solo oficial 🔒
Registra FCM token del oficial.

---

## Cómo consumir el JWT desde el frontend

### 1. Login y guardar el token

```kotlin
// Retrofit / Ktor Client — guardar en SharedPreferences o DataStore
val response = api.login(LoginRequest(email, password))
prefs.edit().putString("jwt_token", response.token).apply()
prefs.edit().putString("user_id", response.id).apply()
prefs.edit().putString("user_role", response.tipo).apply()
```

### 2. Agregar el token en cada request protegido

```kotlin
// Interceptor de Retrofit
class AuthInterceptor(private val prefs: SharedPreferences) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = prefs.getString("jwt_token", null)
        val request = if (token != null) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}
```

### 3. Manejar respuestas de error de auth

| Código | Significado |
|---|---|
| `401 Unauthorized` | Token ausente, expirado o inválido |
| `403 Forbidden` | Token válido pero rol insuficiente (ej: ciudadano intentando crear un caso) |

```kotlin
// Interceptor para manejar 401 → redirigir al login
if (response.code == 401) {
    prefs.edit().remove("jwt_token").apply()
    // navegar a pantalla de login
}
```

### 4. Coordenadas GeoJSON

MongoDB usa el orden **[longitud, latitud]** (al revés de lo habitual). Tenés que invertirlo al enviar y recibir:

```kotlin
// Al enviar
val location = Location(
    type = "Point",
    coordinates = listOf(longitud, latitud)  // lng primero
)

// Al recibir
val lat = coordinates[1]
val lng = coordinates[0]
```

### 5. El token JWT expira en 24 horas

Si recibís un `401`, borrá el token guardado y pedile al usuario que vuelva a loguearse.

---

## Notas importantes

- **Puerto:** el servidor corre en **8081**. El 8080 está ocupado por un Apache (`httpd.exe`) local. Si en el futuro lo liberás o deployás, podés volver a 8080 cambiando `port` en `application.yaml`.
- **Oficiales:** no se registran por API. Se insertan directamente en la colección `oficiales` con la contraseña ya hasheada en BCrypt (`$2b$` o `$2a$` son compatibles).
- **`total_reportes`:** se incrementa automáticamente al crear un reporte vía API. No lo modifiques directamente en la BD.
- **`validado`:** siempre empieza en `false` al crear un reporte. Solo un oficial puede cambiarlo.
- **`reportes_fotos`:** la colección original sigue funcionando en los endpoints legacy (`/ver-reportes`, `/crear-reporte`, `/reportes-cercanos`). Es independiente del nuevo sistema de `casos` + `reportes`.
- **CORS:** está configurado con `anyHost()`. Cambiarlo a dominios específicos antes de producción.
- **Secret JWT:** el valor en `application.yaml` debe reemplazarse por una clave segura en producción.

---

## Historial de desarrollo

### Lo que se construyó (sesión 2026-06-02/03)

El proyecto arrancó con solo un endpoint legacy (`/ver-reportes`) conectado a la colección `reportes_fotos` en Atlas. Se construyó el backend completo en las siguientes etapas:

#### Etapa 1 — CASOS
- Nuevos modelos: `Desaparecido`, `RepresentanteExterno`, `CrearCasoRequest`, `ActualizarEstadoRequest`, `CasoResponse`, `MensajeResponse`, `CrearCasoResponse`
- 6 endpoints CRUD para la colección `casos`
- Manejo de `ObjectId` de MongoDB con conversión defensiva a String en las respuestas

#### Etapa 2 — REPORTES
- Nuevos modelos: `MetadataSeguridad`, `DatosContacto`, `CrearReporteRequest`, `ValidarReporteRequest`, `ReporteCasoResponse`
- 5 endpoints para la colección `reportes`
- `POST /reportes` incrementa automáticamente `total_reportes` en el caso correspondiente via `$inc`

#### Etapa 3 — USUARIOS y AUTH
- Separación de ciudadanos (`usuarios`) y policías (`oficiales`) en colecciones independientes
- Dependencias agregadas: `ktor-server-auth-jvm`, `ktor-server-auth-jwt-jvm`, `jbcrypt:0.4`
- Config JWT en `application.yaml` (`secret`, `issuer`, `audience`, `realm`)
- `JwtUtil` singleton con generación de tokens de 24h
- Plugin `configureAuth()` que instala el sistema JWT y expone `JwtUtil` al resto de la app
- 3 endpoints de auth: registro ciudadano, login ciudadano, login oficial

#### Etapa 4 — Protección de rutas
- Nuevo helper `verificarRol(vararg roles)` en `utils/RouteUtils.kt`
- Refactor de `CasosRoutes`, `ReportesRoutes` y `UsuariosRoutes` para usar bloques `authenticate("auth-jwt")`
- Esquema de acceso:
  - **Público:** auth endpoints, `GET /casos`, `GET /casos/{id}`, `GET /casos/{id}/reportes`, `POST /reportes`
  - **Autenticado (cualquier rol):** `GET /reportes`, `GET /reportes/{id}`, `GET /usuarios/{id}`, FCM tokens de usuarios
  - **Solo oficial:** todo lo que modifica casos, valida reportes, y gestiona oficiales

---

### Errores encontrados y resueltos durante el build

| Error | Causa | Fix aplicado |
|---|---|---|
| `JVM target mismatch (23 vs 21)` | Java 23 instalado, Kotlin apunta a 21 | `kotlin { jvmToolchain(21) }` en `build.gradle.kts` |
| `Unresolved reference: firstOrNull` | Import faltante en 4 archivos de rutas | `import kotlinx.coroutines.flow.firstOrNull` en `CasosRoutes`, `ReportesRoutes`, `AuthRoutes`, `UsuariosRoutes` |
| `Unresolved reference: principal` | Import faltante en `RouteUtils.kt` | `import io.ktor.server.auth.*` |
| `ApplicationConfigurationException` en test | El test no tenía la config de JWT | `MapApplicationConfig` con valores de JWT en `ApplicationTest.kt` |
| `Address already in use` en puerto 8080 | Apache (`httpd.exe`) corriendo en 8080 | Puerto cambiado a **8081** en `application.yaml` |
| `Neither port nor sslPort specified` | `EngineMain` no podía leer el YAML sin soporte YAML | Dependencia `ktor-server-config-yaml:2.3.10` agregada |

---

### Pruebas realizadas contra Atlas (2026-06-03)

Todas las pruebas se hicieron con el servidor corriendo en `http://localhost:8081` conectado al cluster Atlas real.

#### ✅ `GET /` — Health check
```
Backend de Trobat conectado!
```

#### ✅ `GET /casos` — Lista casos reales de Atlas
Devolvió los 2 casos existentes en la BD:
- `Lucía Fernández` (caso `6a1f8ae73d256e14d8bfb091`, 3 reportes, `investigacion_activa`)
- `Tomás Herrera` (caso `6a1f8ae73d256e14d8bfb092`, 1 reporte, `investigacion_activa`)

#### ✅ `POST /auth/registro` — Creó usuario en Atlas
```json
{ "id": "6a1f99559f9173127a2c950f", "mensaje": "Usuario registrado exitosamente" }
```

#### ✅ `POST /auth/login` — Devolvió JWT válido
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJ0cm9iYXQtYmFja2VuZCIsImF1ZCI6InRyb2JhdC1hcHAiLCJpZCI6IjZhMWY5OTU1OWY5MTczMTI3YTJjOTUwZiIsInJvbGUiOiJ1c2VyIiwibm9tYnJlIjoiVGVzdCBVc2VyIiwiZXhwIjoxNzgwNTQyMTczfQ.EB-rwyDkw-MdFsRfCqc8UuPODGDydtB3UcjeH1m-ok4
```

#### ✅ `GET /reportes` con JWT — Devolvió 4 reportes reales de Atlas
Los 4 reportes de los casos existentes, incluyendo `prioridad_policial`, `photo_url`, `validado: false`.

#### ✅ `POST /auth/login/oficial` sin hash real — Respuesta correcta
```json
{ "mensaje": "Credenciales inválidas" }
```
El hash de los oficiales en la BD son placeholders de prueba (`$2b$12$KIVxamplehash...`). Cuando se carguen hashes reales, el login funcionará.

#### ✅ Verificación de roles — `401` sin token, `403` con rol incorrecto
El sistema de `verificarRol()` funciona: un ciudadano autenticado que intenta acceder a rutas de oficial recibe `403 Forbidden`.
