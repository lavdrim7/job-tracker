# Job Tracker

Fullstack-app for å følge jobbsøknader: selskap, stilling, status, frist og notater.

## Krav
- Java 21
- Docker (testene starter PostgreSQL med Testcontainers)
- Node.js 20+ og npm (frontend)

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

## Frontend

React + TypeScript + Vite i `frontend/`. Sider: registrering, innlogging og søknadsoversikt (tabell med statusmerke, filter på status, sortering på frist, og skjema for å opprette, redigere og slette).

Kjør frontend sammen med backend (to terminaler):

```bash
# 1. Backend på :8080 (se "Backend" over)
cd backend
DB_PASSWORD=<passord> JWT_SECRET=<hemmelighet> ./gradlew bootRun

# 2. Frontend på http://localhost:5173
cd frontend
npm install
npm run dev
```

Vite videresender alle kall til `/api` til `http://localhost:8080` (se `frontend/vite.config.ts`). Nettleseren snakker derfor bare med én opprinnelse, og backend trenger ingen CORS-oppsett.

| Kommando (i `frontend/`) | Gjør |
|---|---|
| `npm run dev` | Utviklingsserver med proxy |
| `npm run lint` | ESLint (inkl. `jsx-a11y`) og Prettier-sjekk |
| `npm run format` | Formater med Prettier |
| `npm test` | Vitest + React Testing Library |
| `npm run build` | Typesjekk og produksjonsbygg til `dist/` |

### Lagring av token

JWT-en holdes **bare i minnet** (en variabel i `src/api/client.ts`), ikke i `localStorage`, `sessionStorage` eller en cookie som JavaScript kan lese.

| Alternativ | Avveining |
|---|---|
| `localStorage` / `sessionStorage` | Enkelt og overlever reload, men all JavaScript på siden kan lese tokenet. Én XSS-feil eller kompromittert npm-pakke gir tyveri av tokenet, og det kan brukes fra hvor som helst til det utløper. |
| `HttpOnly`-cookie | Tryggest mot tyveri, siden JS ikke kan lese den. Krever backend-endringer (sette cookie, `SameSite`, CSRF-beskyttelse), som er utenfor denne grenen. |
| **Minne (valgt)** | JS kan fortsatt *bruke* tokenet mens siden lever, så XSS er ikke løst, men tokenet kan ikke hentes ut og tas med seg senere. Kostnaden er at brukeren må logge inn på nytt etter reload eller ny fane. |

Minne er det tryggeste alternativet som ikke krever å endre backend. Et senere steg kan være `HttpOnly`-cookie med refresh-token. Ved 401 fra et beskyttet endepunkt slettes tokenet, brukeren logges ut og sendes til innlogging.

### Tilgjengelighet

Alle felt har ekte `<label>`-elementer, feil vises som tekst (ikke bare farge) knyttet til feltet med `aria-describedby` og `aria-invalid`, og feilsammendrag annonseres med `role="alert"`. Første ugyldige felt får fokus ved innsending, alle handlinger kan nås og brukes med tastatur, og statusmerker bruker tekst i tillegg til farge.

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
