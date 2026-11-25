# Job Tracker

Fullstack-app for å følge jobbsøknader: selskap, stilling, status, frist og notater.

## Krav
- Java 21
- Docker (testene starter PostgreSQL med Testcontainers)

## Backend

Kjør testene (Docker må kjøre):

```bash
cd backend
./gradlew test
```

Start applikasjonen mot en kjørende PostgreSQL:

```bash
cd backend
DB_PASSWORD=<passord> JWT_SECRET=<hemmelighet> ./gradlew bootRun
```

I PowerShell: `$env:DB_PASSWORD = "<passord>"; $env:JWT_SECRET = "<hemmelighet>"; .\gradlew.bat bootRun`

Appen nekter å starte uten `JWT_SECRET`, eller hvis den er kortere enn 32 tegn. Generer en med `openssl rand -base64 48`.

Sjekk at den svarer: `curl http://localhost:8080/api/health` gir `{"status":"UP"}`.

### Miljøvariabler

Se `.env.example`. Spring leser ikke `.env` selv, så variablene må settes i miljøet.

| Variabel | Standard |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `jobtracker` |
| `DB_USER` | `jobtracker` |
| `DB_PASSWORD` | ingen, må settes |
| `JWT_SECRET` | ingen, må settes (minst 32 tegn) |
| `JWT_EXPIRATION` | `PT1H` (ISO-8601-varighet, levetid for token) |
| `SERVER_PORT` | `8080` |

## API: innlogging

| Metode | Sti | Beskrivelse |
|---|---|---|
| `POST` | `/api/auth/register` | Opprett bruker (201). Svarer med `id`, `email` og `createdAt`, aldri passord |
| `POST` | `/api/auth/login` | Svarer med `{"token":"...","tokenType":"Bearer","expiresIn":3600}` |

- `email` må være en gyldig adresse (lagres med små bokstaver, så store/små bokstaver spiller ingen rolle). `password` må være 10-72 tegn og lagres som BCrypt-hash.
- E-post som allerede finnes gir 409.
- Feil e-post og feil passord gir samme 401 (`Ugyldig e-post eller passord`), så svaret avslører ikke hvilke e-poster som er registrert.
- Tokenet er en JWT signert med HS256 (`JWT_SECRET`) og utløper etter `JWT_EXPIRATION`. Det inneholder bare bruker-id.

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"ola@example.com","password":"minst-ti-tegn"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ola@example.com","password":"minst-ti-tegn"}'
```

## API: søknader

Alle endepunkter ligger under `/api/applications` og krever headeren `Authorization: Bearer <token>`. Uten gyldig, signert og ikke utløpt token svarer API-et 401. Hver bruker ser og endrer bare sine egne søknader: en id som tilhører en annen bruker gir 404, akkurat som en id som ikke finnes. Statusverdier: `WISHLIST`, `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`.

| Metode | Sti | Beskrivelse |
|---|---|---|
| `POST` | `/api/applications` | Opprett (201 + `Location`). `status` er `WISHLIST` hvis utelatt |
| `GET` | `/api/applications` | Liste. Valgfritt `?status=APPLIED` og `?sort=deadline` (frist stigende, uten frist sist). Standard er nyeste først |
| `GET` | `/api/applications/{id}` | Hent én |
| `PUT` | `/api/applications/{id}` | Erstatt hele søknaden |
| `DELETE` | `/api/applications/{id}` | Slett (204) |

`company` og `position` er påkrevd og kan ikke være tomme. `deadline` og `appliedDate` bruker formatet `yyyy-MM-dd`.

```bash
curl -X POST http://localhost:8080/api/applications \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"company":"Acme","position":"Backend-utvikler","status":"APPLIED","deadline":"2026-12-01"}'
```

Feil returneres som `{"status":400,"message":"...","errors":["..."]}`. Ukjent id (eller en annen brukers id) gir 404, ugyldig input gir 400, manglende eller ugyldig token gir 401.
