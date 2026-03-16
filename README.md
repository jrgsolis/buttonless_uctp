UCTP Buttonless Demo (Java + Maven + Spring Boot) – backend /sessions
=====================================================================

Why /sessions (not /capture-contexts)
Unified Click to Pay starts with a server-to-server call to the sessions API, and the captureContext JWT used
by the JS SDK (initialize()) is "Returned from the backend sessions call". See UCTP Developer Guide.

What this project does
- Backend: POST https://{vas.host}{vas.sessions.path} to create a session. Parses captureContext JWT from response.
- Frontend: Loads the JS SDK using clientLibrary + clientLibraryIntegrity from JWT payload, then runs:
  initialize() -> getCards() -> OTP (if required) -> checkout().

Configure
Edit src/main/resources/application.properties
- vas.host (default apitest.visaacceptance.com)
- vas.sessions.path (default /sessions)
- merchant.id, merchant.keyId, merchant.secretKey (secretKey must be BASE64)

Run
- mvn spring-boot:run
- open http://localhost:8080/uctp-buttonless

Run (local HTTPS for UCTP)
UCTP `targetOrigins` must be HTTPS. To run locally:
1) Generate a self-signed keystore in the project root:
   - keytool -genkeypair -alias local-https -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore local-keystore.p12 -validity 3650
   - Use password: changeit
   - When prompted for name/host, use: localhost
2) Start Spring Boot with the HTTPS profile:
   - mvn spring-boot:run -Dspring-boot.run.profiles=localhttps
3) Open:
   - https://localhost:8443/uctp-buttonless
   (Your browser will warn because it’s self-signed; proceed for local testing.)

Notes
- The /sessions response schema can differ by tenant; update parsing in SessionsService if your JWT field name differs.
- In production, origins must be HTTPS and you should validate JWT signatures.

Debug: decrypt JWE (MLE)
If you enabled the UI debug call to `/api/debug/decrypt-jwe`, you must configure your MLE private key path:
- Set `ctp.mle.private-key-path` in `src/main/resources/application.properties` (PEM PKCS#8).
- The key file should not be committed (see `.gitignore`).
