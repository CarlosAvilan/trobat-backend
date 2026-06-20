import requests
import json

BASE_URL = "http://127.0.0.1:8081"

# 1. Login como oficial para obtener el token
login_res = requests.post(f"{BASE_URL}/auth/login/oficial", json={
    "email_institucional": "juan.perez@policia.gob.ar",
    "password": "policia123"
})

if login_res.status_code != 200:
    print(f"Login fallido ({login_res.status_code}):", login_res.text)
    exit(1)

login_data = login_res.json()
token      = login_data["token"]
oficial_id = login_data["id"]
print(f"Login OK — oficial_id: {oficial_id}")

# 2. Crear caso
caso_res = requests.post(
    f"{BASE_URL}/casos",
    headers={"Authorization": f"Bearer {token}"},
    json={
        "admin_officer_id": oficial_id,
        "assigned_agents": [],
        "missing_person": {
            "name": "María García",
            "description": "Cabello castaño, ojos marrones, 1.60m",
            "age": 28,
            "image": "https://bucket.url/foto_prueba.jpg",
            "last_known_location": {
                "type": "Point",
                "coordinates": [-58.3816, -34.6037]
            }
        },
        "external_contact": {
            "name": "Carlos García",
            "email": "carlos@mail.com",
            "phone": "1122334455"
        }
    }
)

print(f"\nCrear caso ({caso_res.status_code}):")
print(json.dumps(caso_res.json(), indent=2, ensure_ascii=False))
