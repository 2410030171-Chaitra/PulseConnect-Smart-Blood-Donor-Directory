# PulseConnect – Smart Blood Donor Directory

A smart platform that connects blood donors with recipients instantly, matching by blood group and location for quick, life‑saving support.

## Contents
- Overview
- Features
- Architecture & Tech stack
- Quick start (Windows, one click)
- Run with different profiles (H2/MySQL, Mongo optional)
- Scripts cheat‑sheet
- Configuration (.env and properties)
- API overview (key endpoints)
- URLs (Homepage, Swagger, H2 Console)
- Data seeding
- Build from source
- Troubleshooting

## Overview
Pulse Connect is a Spring Boot 3 app with a modern static frontend (vanilla HTML/CSS/JS) served directly by the backend. It ships with an embedded H2 database for zero‑setup development and can optionally connect to MySQL and MongoDB. SMS providers (Fast2SMS, Twilio, or a simple log provider) are pluggable via configuration.

## Features
- Donor directory with search (by blood group, city, radius)
- Emergency request broadcast (bulk SMS to eligible donors)
- Simple authentication APIs (register/login) for integrating real auth later
- Clean, responsive landing page with modern UI and animations
- Auto data seeding: 1,200+ donors across Telangana for instant demo
- API documentation via Swagger UI
- H2 console for quick DB inspection

Frontend highlights (from `src/main/resources/static/`):
- Hero section with animated graphics and counters
- Donor search form and results cards
- Emergency request form
- Feature showcase and about sections

## Architecture & Tech stack
- Language: Java 21
- Framework: Spring Boot 3.4 (Web, Security, Validation, Mail)
- Persistence: Spring Data JPA (H2 by default, MySQL optional)
- Optional store: Spring Data MongoDB (for document views/sync)
- Build: Maven
- UI: Static HTML/CSS/JS (served by Spring static resources)
- Utilities: Lombok, ModelMapper, jjwt (placeholder token), Apache Commons

## Quick start (Windows, one click)
The repository includes a one‑click launcher that builds (if needed), starts the server with fast local defaults, waits until it’s ready, and opens your browser.

1) Double‑click `start-website.bat` (or run `start-website.ps1`).
- Uses H2 database and disables Mongo sync for a quick start
- Serves live static files from `src/main/resources/static/` (no rebuild needed for UI tweaks)
- Logs to `./logs/pulseconnect.log`

App URL: http://localhost:8081

## Run with different profiles
By default, the app runs with the embedded H2 database. You can switch profiles or backends as needed.

- H2 (default, zero setup)
	- Run with `start-website.bat` or `start-pulseconnect.ps1`

- MySQL
	- Configure env vars (or set in `.env`):
		- `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DB`, `MYSQL_USERNAME`, `MYSQL_PASSWORD`
	- Start with: `start-pulseconnect-mysql.ps1` (uses `--spring.profiles.active=mysql`)

- MongoDB (optional)
	- For local dev you can start a Mongo + mongo‑express stack:
		- `docker-compose up -d` (requires Docker Desktop)
	- Default startup path disables Mongo sync (`--mongo.sync.enabled=false`) so Mongo is not required to run the app.

## Scripts cheat‑sheet
- `start-website.bat` / `start-website.ps1` – One‑click local run (H2, no Mongo sync). Opens browser.
- `start-pulseconnect.ps1` – Start with live static resources; uses defaults (H2). Logs to `logs/pulseconnect.log`.
- `open-pulseconnect.ps1` – Start if needed, then open http://localhost:8081.
- `start-pulseconnect-mysql.ps1` – Run with MySQL profile (`application-mysql.properties`).
- `start-app.ps1` – Minimal launcher (expects the JAR to exist).

## Configuration (.env and properties)
You can place a `.env` file in the repo root for local development. Known keys:
- Security (dev fallback):
	- `spring.security.user.name`, `spring.security.user.password`
- Mongo:
	- `MONGODB_URI` (defaults to `mongodb://localhost:27017/pulseconnect`)
	- `MONGO_SYNC_ENABLED` (defaults to `true`; we pass `--mongo.sync.enabled=false` for speed locally)
- SMS provider (choose one):
	- `SMS_ENABLED` (true/false), `SMS_PROVIDER` (log|twilio|fast2sms), `SMS_DEFAULT_CC` (e.g., +91)
	- Twilio: `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN`, `TWILIO_PHONE_NUMBER`
	- Fast2SMS: `FAST2SMS_API_KEY`, `FAST2SMS_SENDER_ID`, `FAST2SMS_ROUTE`
- Mail (example: Gmail):
	- `spring.mail.username`, `spring.mail.password` (use app password)
- Google Maps:
	- `google.maps.api.key`

See `src/main/resources/application.properties` and `application-mysql.properties` for all defaults and comments.

## API overview (key endpoints)
Base URL: `http://localhost:8081`

- Auth (`/api/auth`)
	- `POST /register` – Create a user + donor profile (if role=DONOR)
	- `POST /login` – Simple login; returns a demo token (no full JWT yet)

- Donors (`/api/donors`)
	- `GET /search?bloodGroup=A%2B&city=Hyderabad&radius=20` – Filtered donor listing; uses demo distance
	- `POST /emergency` – Broadcast SMS to eligible donors of a blood group
		- Body: `{ "patientName": "...", "contactNumber": "...", "requiredBloodGroup": "A_POSITIVE", "unitsRequired": "1", "hospitalLocation": "...", "additionalDetails": "..." }`

- Users (`/api/users`)
	- `GET /{id}` – Fetch a user
	- `PUT /{id}` – Update user details (name, phone, address, etc.)

- SMS (dev helper)
	- `POST /sendSMS` – Send a single SMS via active provider (or log)
	- `POST /sendSMS/bulk` – Bulk SMS with guardrails

Note: `SecurityConfig` currently permits all requests for easy local testing. Tighten rules as you build real auth.

## URLs
- Homepage: http://localhost:8081/
- Swagger UI: http://localhost:8081/swagger-ui.html
- H2 Console: http://localhost:8081/h2-console (JDBC URL: `jdbc:h2:file:./data/pulseconnect`)

## Data seeding
On first run, `DataInitializer` seeds ~1,280 donors across Telangana (multiple cities × 8 blood groups × 5 donors each). Subsequent runs won’t reseed if data exists.

## Build from source
Requires Java 21+ and Maven.

```powershell
# From repo root
mvn -DskipTests package
java -jar target\blood-donor-directory-1.0.0.jar --spring.web.resources.static-locations="file:src/main/resources/static/,classpath:/static/" --mongo.sync.enabled=false
```

## Troubleshooting
- Port 8081 not reachable yet
	- Startup includes schema init and data seeding; give it 10–30s, then refresh.
	- See `logs/pulseconnect.log` for details (scripts tail this when a timeout happens).

- Java not found
	- Install JDK 21+ and ensure `JAVA_HOME` and/or `java` is on PATH. The launcher attempts to auto‑detect common JDK installs.

- Maven missing (first build only)
	- Install Maven or build once from VS Code’s Maven extension. Commands: `mvn -DskipTests package`.

- Mongo errors during startup
	- Local launcher disables Mongo sync; if you need Mongo, run `docker-compose up -d` first or point `MONGODB_URI` to an available instance.

---

Made with Spring Boot + a clean static frontend. Contributions welcome!
