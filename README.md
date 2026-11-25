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
DB_PASSWORD=<passord> ./gradlew bootRun
```

I PowerShell: `$env:DB_PASSWORD = "<passord>"; .\gradlew.bat bootRun`

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
| `SERVER_PORT` | `8080` |

## API: søknader

Alle endepunkter ligger under `/api/applications`. Statusverdier: `WISHLIST`, `APPLIED`, `INTERVIEW`, `OFFER`, `REJECTED`.

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
  -H "Content-Type: application/json" \
  -d '{"company":"Acme","position":"Backend-utvikler","status":"APPLIED","deadline":"2026-12-01"}'
```

Feil returneres som `{"status":400,"message":"...","errors":["..."]}`. Ukjent id gir 404, ugyldig input gir 400.
