# RAF platforma za realizaciju kolokvijuma i ispita iz programiranja (server)

Spring Boot REST API za predmet *Razvoj softvera*. Nastavnik postavlja zadatke (ZIP projekat),
studenti ih preuzimaju i predaju rešenja tokom termina, a nastavnik pregleda predate radove,
ostavlja komentare po fajlu i liniji i unosi poene. Ovaj repozitorijum sadrži serverski deo.

## Tehnologije

Java 21, Spring Boot 4.1, Spring Data JPA (Hibernate), Spring Security (JWT, HS256),
MySQL/MariaDB (XAMPP), H2 za testove, Maven (mvnw u repozitorijumu).

## Preduslovi

- JDK 21
- XAMPP sa pokrenutim MySQL modulom (korisnik `root`, prazna lozinka, port 3306)

## Pokretanje

1. Pokreni **MySQL** u XAMPP-u.
2. U folderu `raf-lms-server` kopiraj `.env.example` u `.env` i upiši svoj `JWT_SECRET`
   (najmanje 32 znaka). Fajl `.env` nije u repozitorijumu.
   U IntelliJ-u se umesto toga može podesiti Environment variables u Run konfiguraciji.
3. Pokreni aplikaciju:
   ```
   cd raf-lms-server
   .\mvnw.cmd spring-boot:run
   ```
   Server radi na `http://localhost:8080`.

Baza `raf_lms` se pravi sama (`createDatabaseIfNotExist`). Podrazumevano (`DB_DDL_AUTO=create-drop`)
tabele pravi Hibernate iz entiteta, a `Seeder` ih puni probnim podacima. Posle gašenja aplikacije
tabele se brišu.

### Varijanta sa SQL skriptom

Šema je u `raf-lms-server/database/01_create_tables.sql`.

1. U phpMyAdmin-u uvezi (Import) tu skriptu. Ona sama pravi bazu `raf_lms`.
2. Pokreni aplikaciju sa `DB_DDL_AUTO=validate`. Hibernate tada samo proverava da li se tabele
   poklapaju sa entitetima, a Seeder popunjava praznu bazu.

## Konfiguracija (promenljive okruženja ili `.env`)

| Promenljiva | Podrazumevano | Opis |
|---|---|---|
| `JWT_SECRET` | (obavezno) | tajni ključ za potpisivanje tokena, najmanje 32 znaka |
| `DB_URL` | `jdbc:mysql://localhost:3306/raf_lms?...` | adresa baze |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / prazno | pristup bazi |
| `DB_DDL_AUTO` | `create-drop` | `create-drop` ili `validate` |
| `SEED_ENABLED` | `true` | punjenje probnim podacima pri startu (samo ako je baza prazna) |
| `SHOW_SQL` | `false` | ispis SQL upita u konzoli |
| `STORAGE_ROOT` | `./storage` | folder za ZIP projekte i predata rešenja |
| `CORS_ORIGINS` | localhost:3000, 5173, 4200 | adrese veb aplikacije |

## Probni korisnici (Seeder)

Lozinka za sve: `lozinka123`

| Korisnik | Uloga | Napomena |
|---|---|---|
| `teacher1` | nastavnik | predmet "matematika" (id 1) |
| `teacher2` | nastavnik | predmet "geografija" |
| `student0` … `student4` | student | prijavljeni na aktivan termin (id 2), grupe A/B |
| `student0` | student | prijavljen i na budući termin (id 3) |
| `student5` | student | nije prijavljen ni na jedan termin |

Termini predmeta 1: prošli (id 1), aktivan (id 2) i budući (id 3). Zadaci u aktivnom terminu su
"mnozenje" (sve grupe), "sabiranje" (grupa A) i "oduzimanje" (grupa B). Pre nego što student može
da ih preuzme, nastavnik mora da postavi ZIP projekat za zadatak.

## Testovi

```
cd raf-lms-server
.\mvnw.cmd test
```
Testovi koriste H2 bazu u memoriji, pa MySQL nije potreban.

## Autentifikacija

`POST /api/auth/login` sa telom `{"username": "...", "password": "..."}` vraća
`{"accessToken": "...", "tokenType": "Bearer", "role": "TEACHER"}`.
Token se šalje u zaglavlju `Authorization: Bearer <token>`. Važi 4 sata (ispit traje satima,
a refresh token nije implementiran). Putanje `/api/teacher/**` su dozvoljene samo ulozi
TEACHER, a `/api/student/**` samo ulozi STUDENT. Nastavnik radi samo nad predmetima koje predaje.

## Pregled endpointa

Greške se vraćaju kao `{"message": "..."}` sa statusom 400, 401, 403, 404 ili 409.

### Student (`/api/student`)

| Metoda | Putanja | Opis |
|---|---|---|
| GET | `/exam-sessions` | termini na koje je student prijavljen (predmet, rok, vreme, grupa, status) |
| GET | `/exam-sessions/{sessionId}/assignments` | zadaci dostupni studentu (samo tokom termina) |
| GET | `/exam-sessions/{sessionId}/assignments/{assignmentId}/project` | preuzimanje ZIP projekta (prvo preuzimanje beleži početak rada) |
| POST | `/exam-sessions/{sessionId}/assignments/{assignmentId}/submissions/final` | predaja rešenja (ZIP, multipart) |
| GET | `/exam-sessions/{sessionId}/result` | poeni, povratna informacija i komentari (tek kad nastavnik oceni) |

### Nastavnik (`/api/teacher`)

| Metoda | Putanja | Opis |
|---|---|---|
| GET | `/courses` | predmeti koje nastavnik predaje |
| GET, POST | `/exam-periods` | ispitni rokovi |
| GET, POST | `/courses/{courseId}/exam-sessions` | termini predmeta |
| GET, POST | `/courses/{courseId}/exam-sessions/{sessionId}/groups` | grupe u terminu |
| GET, POST | `/courses/{courseId}/exam-sessions/{sessionId}/registrations` | spisak prijavljenih studenata (unos: indeks, grupa, učionica) |
| GET, POST | `/courses/{courseId}/exam-sessions/{sessionId}/assignments` | zadaci termina |
| PUT, DELETE | `/courses/{courseId}/exam-sessions/{sessionId}/assignments/{assignmentId}` | izmena i brisanje zadatka |
| POST, GET | `/courses/{courseId}/exam-sessions/{sessionId}/assignments/{assignmentId}/project` | postavljanje i preuzimanje ZIP projekta |
| GET | `/courses/{courseId}/exam-sessions/{sessionId}/monitoring` | status svakog studenta, vremena, prosečno trajanje |
| GET | `/courses/{courseId}/submissions` | pregled predatih rešenja (filteri: rok, termin, grupa, učionica, indeks) |
| GET | `/courses/{courseId}/submissions/{submissionId}/download` | preuzimanje jednog rešenja |
| GET | `/courses/{courseId}/submissions/download` | ZIP svih rešenja po filteru |
| GET | `/courses/{courseId}/submissions/{submissionId}/files` | fajlovi u rešenju |
| GET, POST | `/courses/{courseId}/submissions/{submissionId}/comments` | komentari po fajlu i liniji |
| PUT, DELETE | `/courses/{courseId}/submissions/{submissionId}/comments/{commentId}` | izmena i brisanje komentara |
| GET, PUT | `/courses/{courseId}/attempts/{attemptId}/grade` | poeni i povratna informacija |

## Poslovna pravila

- Student može da preuzima i predaje samo tokom termina, samo za svoju grupu i samo ako je prijavljen.
- Pokušaj rada (vreme početka) nastaje kad student prvi put preuzme projekat.
- Za svaki zadatak važi poslednja predaja. Predaja posle isteka termina se odbija.
- Komentari se ostavljaju samo na finalna rešenja, a ocenu nastavnik unosi po pokušaju (0 do maksimum termina).
- Student ne vidi poene, povratnu informaciju ni komentare dok nastavnik ne oceni rad.

## Poznata ograničenja

- Nastavnik može da oceni rad i dok termin još traje.
- Ako student ponovo preda rešenje, komentari ostaju vezani za stariju predaju.
- ZIP projekat zadatka se ne može zameniti kad je jednom postavljen.
- Preuzimanje svih rešenja pravi ZIP u memoriji.