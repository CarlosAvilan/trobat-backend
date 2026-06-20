# cURLs — Trobat Backend

> Reemplazá `BASE_URL`, `TOKEN`, `CASO_ID`, etc. con los valores reales.

```bash
BASE_URL="http://localhost:8081"
TOKEN="jwt_aqui"
CASO_ID="id_aqui"
REPORTE_ID="id_aqui"
USUARIO_ID="id_aqui"
OFICIAL_ID="id_aqui"
AGENTE_ID="id_aqui"
REPORTANTE_ID="id_aqui"
```

---

## Auth

### Registro de usuario
```bash
curl -X POST "$BASE_URL/auth/registro" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan Pérez",
    "email": "juan@mail.com",
    "password": "secreta123"
  }'
```

### Login de usuario
```bash
curl -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@mail.com",
    "password": "secreta123",
    "fcm_token": null
  }'
```

### Login de oficial
```bash
curl -X POST "$BASE_URL/auth/login/oficial" \
  -H "Content-Type: application/json" \
  -d '{
    "email_institucional": "oficial@policia.gov",
    "password": "secreta123",
    "fcm_token": null
  }'
```

### Reset password de oficial
```bash
curl -X POST "$BASE_URL/auth/reset-password/oficial" \
  -H "Content-Type: application/json" \
  -d '{
    "email_institucional": "oficial@policia.gov",
    "nueva_password": "nueva123"
  }'
```

### Logout
```bash
curl -X POST "$BASE_URL/auth/logout" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fcm_token": "token_fcm_aqui"
  }'
```

---

## Casos

### Listar casos (paginado)
```bash
curl "$BASE_URL/casos?page=0&limit=20"
```

### Obtener caso por ID
```bash
curl "$BASE_URL/casos/$CASO_ID"
```

### Crear caso
```bash
curl -X POST "$BASE_URL/casos" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "oficial_administrador_id": "'"$OFICIAL_ID"'",
    "agentes_asignados": [],
    "desaparecido": {
      "nombre": "María García",
      "descripcion": "Cabello castaño, ojos marrones, 1.60m",
      "edad": 28,
      "imagen": "https://bucket.url/foto.jpg",
      "ultima_ubicacion_oficial": {
        "type": "Point",
        "coordinates": [-58.3816, -34.6037]
      }
    },
    "representante_externo": {
      "nombre": "Carlos García",
      "email": "carlos@mail.com",
      "telefono": "1122334455"
    }
  }'
```

### Actualizar estado de caso
```bash
curl -X PATCH "$BASE_URL/casos/$CASO_ID/estado" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "estado": "resuelto"
  }'
```

> Estados válidos: `investigacion_activa`, `resuelto`, `cerrado`, `suspendido`

### Agregar agente a caso
```bash
curl -X POST "$BASE_URL/casos/$CASO_ID/agentes/$AGENTE_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Quitar agente de caso
```bash
curl -X DELETE "$BASE_URL/casos/$CASO_ID/agentes/$AGENTE_ID" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Reportes

### Listar reportes de un caso (público)
```bash
curl "$BASE_URL/casos/$CASO_ID/reportes"
```

### Crear reporte (público)
```bash
curl -X POST "$BASE_URL/reportes" \
  -H "Content-Type: application/json" \
  -d '{
    "caso_id": "'"$CASO_ID"'",
    "location": {
      "type": "Point",
      "coordinates": [-58.3816, -34.6037]
    },
    "descripcion": "Vi a una persona similar cerca de la plaza",
    "photo_url": "https://bucket.url/foto.jpg",
    "prioridad_policial": false,
    "metadata_seguridad": { "anonimo": true },
    "datos_contacto": {
      "nombre": null,
      "telefono": null,
      "email": null
    }
  }'
```

### Listar reportes (autenticado, con paginación y filtro opcional por caso)
```bash
curl "$BASE_URL/reportes?page=0&limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

```bash
# Filtrar por caso
curl "$BASE_URL/reportes?caso_id=$CASO_ID&page=0&limit=20" \
  -H "Authorization: Bearer $TOKEN"
```

### Obtener reporte por ID
```bash
curl "$BASE_URL/reportes/$REPORTE_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Validar reporte
```bash
curl -X PATCH "$BASE_URL/reportes/$REPORTE_ID/validar" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "validado": true
  }'
```

---

## Usuarios

### Obtener usuario por ID
```bash
curl "$BASE_URL/usuarios/$USUARIO_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Registrar FCM token de usuario
```bash
curl -X POST "$BASE_URL/usuarios/$USUARIO_ID/fcm-token" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fcm_token": "token_fcm_aqui"
  }'
```

---

## Oficiales

### Listar oficiales
```bash
curl "$BASE_URL/oficiales" \
  -H "Authorization: Bearer $TOKEN"
```

### Obtener oficial por ID
```bash
curl "$BASE_URL/oficiales/$OFICIAL_ID" \
  -H "Authorization: Bearer $TOKEN"
```

### Registrar FCM token de oficial
```bash
curl -X POST "$BASE_URL/oficiales/$OFICIAL_ID/fcm-token" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fcm_token": "token_fcm_aqui"
  }'
```

---

## Usuarios Reportantes

### Registro
```bash
curl -X POST "$BASE_URL/usuarios-reportantes/registro" \
  -H "Content-Type: application/json" \
  -d '{
    "mail": "reportante@mail.com",
    "contrasenia": "secreta123",
    "informacion_personal": {
      "dni": "12345678",
      "nombre_completo": "Ana López",
      "telefono": "1122334455"
    }
  }'
```

### Login
```bash
curl -X POST "$BASE_URL/usuarios-reportantes/login" \
  -H "Content-Type: application/json" \
  -d '{
    "mail": "reportante@mail.com",
    "contrasenia": "secreta123"
  }'
```

### Obtener perfil
```bash
curl "$BASE_URL/usuarios-reportantes/perfil" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Legacy

### Health check
```bash
curl "$BASE_URL/"
```

### Ver reportes (colección reportes_fotos)
```bash
curl "$BASE_URL/ver-reportes"
```

### Reportes cercanos
```bash
curl "$BASE_URL/reportes-cercanos?lat=-34.6037&lng=-58.3816&radio=1000"
```

### Crear reporte (multipart)
```bash
curl -X POST "$BASE_URL/crear-reporte" \
  -F "id_solicitud=sol-001" \
  -F "descripcion=Vi a una persona similar cerca de la plaza" \
  -F "estado=activo" \
  -F "latitud=-34.6037" \
  -F "longitud=-58.3816" \
  -F "foto=@/ruta/a/imagen.jpg"
```
