# TripDesk – Travel Booking REST API

A Spring Boot backend for searching and booking hotels and flights, with JWT authentication, role-based access, and protection against double bookings.

## Tech stack
- **Java 17**, **Spring Boot 3.3**
- **Spring Security** with stateless **JWT** authentication (USER / ADMIN roles)
- **Spring Data JPA / Hibernate** on **MySQL** (H2 in-memory for local dev and tests)
- **Bean Validation**, global exception handling, pagination and sorting
- **JUnit 5, Mockito, MockMvc** tests
- **Swagger / OpenAPI** docs (springdoc)
- **Docker** and **docker-compose**

## Architecture
```
controller  ->  service  ->  repository  ->  database
   (REST)      (business       (Spring Data
               rules, tx)        JPA)
```
- `entity`: User, Hotel, Room, Flight, Booking
- `security`: JWT generation/validation, auth filter, user details service
- `exception`: `GlobalExceptionHandler` returns a consistent JSON error body with per-field validation errors

### Preventing double bookings
Booking a room runs in a single transaction that takes a **pessimistic write lock** on the room row, then checks for any confirmed booking with overlapping dates (`checkIn < newCheckOut AND checkOut > newCheckIn`). Concurrent requests for the same room are serialized, so only one can win. Flight bookings lock the flight row the same way before decrementing seats, so seats are never oversold. Cancelling a flight booking releases its seats.

## Running locally
```bash
# dev profile: in-memory H2 with sample data, no setup needed
mvn spring-boot:run
```
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Seeded admin: `admin@tripdesk.com` / `admin123`

### With MySQL (Docker)
```bash
docker compose up --build
```

### Tests
```bash
mvn test
```

## API overview
| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create an account, returns JWT |
| POST | `/api/auth/login` | Public | Log in, returns JWT |
| GET | `/api/hotels?city=&page=&size=` | Public | Search hotels (paginated) |
| GET | `/api/hotels/{id}/rooms?checkIn=&checkOut=` | Public | Rooms available for dates |
| POST/PUT/DELETE | `/api/hotels/**` | Admin | Manage hotels and rooms |
| GET | `/api/flights?source=&destination=&date=` | Public | Search flights (paginated) |
| POST | `/api/flights` | Admin | Add a flight |
| POST | `/api/bookings/hotel` | User | Book a room |
| POST | `/api/bookings/flight` | User | Book flight seats |
| GET | `/api/bookings/me` | User | My bookings (paginated) |
| PATCH | `/api/bookings/{id}/cancel` | User | Cancel a booking |

## Example
```bash
# register
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Akhil","email":"akhil@example.com","password":"secret123"}'

# book a room (use the token from register/login)
curl -X POST localhost:8080/api/bookings/hotel -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"roomId":1,"checkIn":"2026-11-07","checkOut":"2026-11-09"}'

# booking the same room for overlapping dates returns 409 Conflict
```
