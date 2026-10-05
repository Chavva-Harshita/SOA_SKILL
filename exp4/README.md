# Skill Experiment 4: Token-Based Authentication for Banking API

## Objective

This small REST API demonstrates login, JWT creation and validation, stateless security, and protection of sensitive account details. It uses one in-memory user and does not require a database, frontend, or other services.

## Technologies

- Java 21
- Spring Boot 3.4.0, Spring Web, and Spring Security
- JJWT 0.13.0 for HS256 signed tokens
- Maven

## Project Structure

```text
src/main/java/com/example/bankapi/
  BankApiApplication.java
  config/SecurityConfig.java
  controller/AuthController.java
  controller/AccountController.java
  filter/JwtAuthenticationFilter.java
  model/AccountDetails.java
  model/LoginRequest.java
  model/LoginResponse.java
  service/JwtService.java
src/main/resources/application.properties
src/test/java/com/example/bankapi/BankApiAuthenticationTests.java
```

## How JWT Authentication Works

A JWT is a signed token containing claims such as the subject (username), issue time, and expiration time. The client sends it on later requests in the `Authorization: Bearer <token>` header. The API verifies its HMAC signature and expiry; it does not store a server-side login session.

```text
Client
  |
  | POST /auth/login (username + password)
  v
AuthenticationManager checks BCrypt password
  |
  v
JwtService signs a JWT and returns it
  |
  | Authorization: Bearer <JWT>
  v
JwtAuthenticationFilter validates signature, expiry, and subject
  |
  v
Spring Security SecurityContext -> AccountController
  |
  v
Account details
```

## Login and Token Flow

1. `POST /auth/login` is public. `AuthController` delegates credential checking to Spring Security's `AuthenticationManager`.
2. The in-memory user is named `user`. Its password is stored as a BCrypt hash in `application.properties`; the demo password is `password`.
3. After authentication, `JwtService` creates a signed token with the username as subject plus issued-at and expiration timestamps.
4. The response contains the token as JSON. The client should keep it securely and attach it as a Bearer token.
5. `JwtAuthenticationFilter` runs before controller handling. For a Bearer token, it asks `JwtService` to parse and verify the signature, checks expiry and username, then sets an authenticated principal in Spring Security's `SecurityContext`.
6. `GET /account/details` is available only after that authentication. Missing, malformed, invalid, or expired tokens receive HTTP 401.

Password hashing and JWT signing solve different problems. BCrypt is a one-way password hash used to check a submitted password against a stored hash. HMAC JWT signing uses a secret key to prove that token claims were issued by this API and have not been changed; it does not encrypt the claims.

## Important Files

- `SecurityConfig.java` defines the in-memory user, BCrypt encoder, authentication manager, and request rules. Login is public so a user can obtain a token; account details require authentication; all other routes are denied. CSRF is disabled for this stateless JSON API, and `SessionCreationPolicy.STATELESS` prevents HTTP sessions. Each request carries its own token, so the server does not need to remember a logged-in session.
- `JwtService.java` creates tokens and parses signed claims. Parsing with the configured key verifies the HMAC signature; the JWT library rejects expired tokens and `isTokenValid` also checks the expiration claim and username.
- `JwtAuthenticationFilter.java` reads the Authorization header and processes Bearer tokens once per request. A valid token creates the `Authentication` stored in `SecurityContext`; invalid credentials are sent to the JSON 401 entry point.
- `AuthController.java` handles login and returns `{ "token": "..." }`; bad credentials return HTTP 401.
- `AccountController.java` returns the sample sensitive account data. Its protection is enforced by Spring Security, not by controller-specific authentication code.
- `LoginRequest.java`, `LoginResponse.java`, and `AccountDetails.java` are small request/response data records.
- `application.properties` contains the server port, externalized JWT key, five-minute token lifetime, and demo BCrypt password hash.

## Run and Build

From the `exp4` directory:

```powershell
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

The Maven Wrapper downloads Maven 3.9.16 when needed. If Maven is already installed, the equivalent commands are `mvn clean install` and `mvn spring-boot:run`. The API listens on `http://localhost:8080`.

The five-minute token lifetime is `jwt.expiration=300000` in `application.properties`. Change it there (milliseconds) to experiment with expiry. The Base64 HS256 secret is configurable with the `JWT_SECRET` environment variable; use a randomly generated secret of at least 32 bytes outside this classroom demo. The default key exists only to make local runs convenient and must not be used in production.

## Postman and cURL Tests

### 1. Login with valid credentials

```powershell
curl.exe -i -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"user","password":"password"}'
```

Expected: `HTTP/1.1 200` and JSON with a `token` field. Copy the token for the next request. In Postman, use POST, Body > raw > JSON, and the same JSON body.

### 2. Read account details with the token

```powershell
curl.exe -i http://localhost:8080/account/details `
  -H "Authorization: Bearer <VALID_TOKEN>"
```

Expected: `HTTP/1.1 200` and `{"accountNumber":"XXXXXX1234","accountHolder":"Test User","balance":50000.00}`. In Postman, set the Authorization type to Bearer Token and paste the token.

### 3. Read account details without a token

```powershell
curl.exe -i http://localhost:8080/account/details
```

Expected: `HTTP/1.1 401` and JSON error `Unauthorized`.

### 4. Send an invalid token

```powershell
curl.exe -i http://localhost:8080/account/details `
  -H "Authorization: Bearer invalid-token"
```

Expected: `HTTP/1.1 401` and JSON error `Unauthorized`.

### 5. Send an expired token

The automated test suite creates an already-expired token signed with the configured key and verifies that the endpoint returns `HTTP 401`. To try it manually, temporarily set `jwt.expiration=1000`, restart the app, log in, wait longer than one second, then call `/account/details` with that token. Restore `300000` afterward. A token signed with a different key or changed by hand is invalid, not a valid expired token.

## Automated Tests

Run `.\mvnw.cmd test` from this directory. The tests exercise valid login/account access, missing token, invalid token, expired token, and invalid login credentials using MockMvc.

## Security Considerations

- The demo user and sample account data are in-memory and must not be used as real banking credentials or account data.
- Keep the HMAC secret private, random, and outside source control in real deployments. Anyone holding it can create valid tokens.
- JWT claims are encoded, not encrypted; do not place passwords or sensitive banking details inside a token.
- Use HTTPS in deployment and keep access-token lifetimes appropriately short. This experiment intentionally omits refresh tokens and revocation storage.
- BCrypt password hashing is intentionally separate from HMAC token signing.

## Viva / Interview Summary

The user sends a username and password to the login endpoint. Spring Security checks the password against its BCrypt hash, and the API returns a JWT signed with a secret key. On later requests, the client sends that token in the Bearer Authorization header. A security filter verifies the signature and expiry, then puts the username into Spring Security's SecurityContext. The protected account endpoint runs only when that authentication is valid; otherwise Spring returns 401. Because every request carries its token, the server does not need an HTTP session.