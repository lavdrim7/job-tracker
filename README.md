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
