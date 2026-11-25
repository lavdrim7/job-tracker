# Job Tracker

Fullstack-app for å følge jobbsøknader (selskap, stilling, status, frist, notater).
Porteføljeprosjekt, så kodekvalitet, tester og dokumentasjon teller.

## Stack
- Backend: Kotlin, Spring Boot 3, Gradle (Kotlin DSL), Java 21
- Database: PostgreSQL, migrasjoner med Flyway
- Auth: Spring Security med JWT, passord hashet med BCrypt
- Frontend: React + TypeScript + Vite
- Drift: Docker Compose lokalt, GitHub Actions for CI
- Mapper: `backend/`, `frontend/`

## Arbeidsregler
- Små commits med Conventional Commits (feat:, fix:, test:, docs:, chore:)
- Hver ny funksjon skal ha tester. Kjør alle tester før commit.
- Aldri commit hemmeligheter. Bruk miljøvariabler og `.env.example`.
- Ingen endringer i DB-skjema uten ny Flyway-migrasjon.
- Hold README oppdatert med hvordan man kjører prosjektet.

## Kommandoer
- Backend-tester: `cd backend && ./gradlew test`
- Frontend dev: `cd frontend && npm run dev`
- Alt lokalt: `docker compose up`
