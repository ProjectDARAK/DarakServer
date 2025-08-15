# Darak (Attic) — SOHO/Personal NAS Platform

Darak (Korean: "다락", meaning attic/loft) is a SOHO/personal NAS solution. This repository contains the backend API server built with Spring Boot and Kotlin, designed to power the Darak ecosystem, including a Flutter-based client.

## Repository Scope
- This repo: Backend REST API server
- Tech stack: Spring Boot 3 (Kotlin), Spring Security, Spring Data JPA, PostgreSQL, Gradle, optional GraalVM native image
- Client(s): Flutter app (separate repository)

## Key Features

### 1) User Management
- Local user accounts (registration, authentication, roles)
- Active Directory integration and SSO support (e.g., via LDAP/AD + OAuth2/OpenID Connect)

### 2) Security
- Security design aligned with ISMS-P principles (input validation, least privilege, secret management, auditability)
- Multi-factor Authentication (MFA)
  - TOTP-based OTP (Google Authenticator compatible)
  - WebAuthn Passkeys for phishing-resistant authentication

### 3) File Management
- Upload and download
- Internal sharing (between users/groups within Darak)
- External sharing via:
  - Link-based sharing with optional password protection
  - File-serving mode (direct file serving without folder browsing)

### 4) NAS Management
- Docker Hub (local or configured remote host)
  - List, run, restart, stop, and delete containers
- Web terminal (browser-accessible shell)
- System monitoring
  - Process list
  - Memory/CPU usage
- Storage management
  - Total and used capacity inspection
  - Per-user quota enforcement
  - External storage connectors
    - USB media
    - Google Drive
    - Microsoft OneDrive
    - WebDAV servers
    - S3 API–compatible object storage

### 5) Chatbot
- Small LLM (sLLM) selection and execution (enabled when system memory ≥ 4GB)
- RAG (Retrieval-Augmented Generation): vectorize uploaded files and retrieve relevant chunks for context-augmented answers

## Architecture Overview
- API server exposes REST endpoints consumed by the Flutter client
- Authentication/Authorization via Spring Security
- Persistence via JPA (default driver: PostgreSQL)
- Pluggable providers for:
  - Identity (Local, AD/SSO)
  - MFA (TOTP, WebAuthn)
  - Storage backends (local filesystem + external connectors)
  - LLM/RAG pipeline

## Getting Started

### Prerequisites
- JDK 21+
- PostgreSQL 14+ (or compatible)
- Gradle (wrapper included)
- Optional for native build: GraalVM 22.3+

### Build and Run (Dev)
1. Configure application properties (see below)
2. Run the API server:
   - On your host: `./gradlew bootRun`
   - Or build a jar: `./gradlew build` then `java -jar build/libs/DarakServer-0.0.1-SNAPSHOT.jar`
3. The server defaults to port 8080

### Container Image
- Build OCI image with Buildpacks: `./gradlew bootBuildImage`
- Run: `docker run --rm -p 8080:8080 darakserver:0.0.1-snapshot`

## Configuration
Configuration is managed via Spring Boot properties (application.properties or environment variables). The following keys illustrate the expected configuration surface. Exact names may evolve as modules are implemented.

- Server
  - `server.port=8080`

- Database (PostgreSQL)
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/darak`
  - `spring.datasource.username=darak`
  - `spring.datasource.password=change-me`
  - `spring.jpa.hibernate.ddl-auto=validate` (or `update` for dev)

- Security / Identity
  - Local users: enabled by default
  - Active Directory/LDAP:
    - `darak.auth.ldap.enabled=true`
    - `darak.auth.ldap.url=ldap://ad.example.com:389`
    - `darak.auth.ldap.base-dn=DC=example,DC=com`
    - `darak.auth.ldap.user-dn-pattern=uid={0},OU=Users,DC=example,DC=com`
  - OIDC (SSO):
    - `spring.security.oauth2.client.registration.<id>.client-id=...`
    - `spring.security.oauth2.client.registration.<id>.client-secret=...`
    - `spring.security.oauth2.client.provider.<id>.issuer-uri=...`

- MFA
  - TOTP (Google Authenticator): `darak.mfa.totp.enabled=true`
  - WebAuthn Passkeys: `darak.mfa.webauthn.enabled=true`

- File Management
  - `darak.files.root=/var/lib/darak/data`
  - External sharing passwords required: `darak.files.external.password-required=true` (optional)

- Docker Management
  - Local Docker socket or remote:
    - `darak.docker.mode=local|remote`
    - `darak.docker.remote.host=tcp://docker-host:2376`
    - `darak.docker.remote.tls=true`

- Web Terminal
  - `darak.terminal.enabled=true`

- Monitoring
  - `darak.monitoring.process.enabled=true`
  - `darak.monitoring.resources.enabled=true`

- Storage Management
  - Quotas: `darak.storage.quota.enabled=true`, `darak.storage.quota.default=50GB`
  - External connectors
    - USB: auto-detected or `darak.storage.usb.mount-root=/media`
    - Google Drive: `darak.storage.gdrive.credentials-file=/secrets/gdrive.json`
    - OneDrive: `darak.storage.onedrive.client-id=...`
    - WebDAV: `darak.storage.webdav.url=https://webdav.example.com/`
    - S3-compatible: `darak.storage.s3.endpoint=https://s3.example.com`, `darak.storage.s3.bucket=darak`

- Chatbot / RAG
  - `darak.ai.enabled=true`
  - `darak.ai.min-memory-mb=4096`
  - Model provider selection: `darak.ai.provider=llama.cpp|ollama|local-gguf|custom`
  - Vector store path: `darak.ai.vector-store.path=/var/lib/darak/vector-store`

Note: Do not commit real secrets. Use environment variables or secrets managers.

## API Surface
The API endpoints are being incrementally implemented. High-level areas will include:
- Auth: login, refresh, MFA enrollment/verification, WebAuthn registration/assertion
- Users & Groups: CRUD, roles, quotas
- Files: upload, download, share links, internal shares, password-protected links
- Docker: list/run/restart/stop/delete containers (local or remote)
- Terminal: session management and streaming
- Monitoring: process list, CPU/memory metrics
- Storage: capacity, mounts, external connectors (list/configure)
- Chatbot: model management, embeddings, RAG queries

Refer to HELP.md for Gradle and native image commands.

## Security Posture
- ISMS-P aligned: input validation, output encoding, secure storage of secrets, audit logs, data minimization
- MFA-first mindset: TOTP and WebAuthn supported
- Principle of Least Privilege for all services and integrations
- Regular dependency updates and vulnerability scans

## Development
- Kotlin style and secure coding: see CONVENTION.md
- Testing: JUnit 5 (see build.gradle.kts)
- Code review checklist includes security items (ISMS-P aligned)

## Roadmap (High-Level)
- [ ] Implement full WebAuthn ceremonies (server-side)
- [ ] AD/LDAP and OIDC SSO production hardening
- [ ] External storage connectors (GDrive/OneDrive/WebDAV/S3) modules
- [ ] Docker remote TLS integration and terminal sandboxing
- [ ] RAG pipeline with pluggable vector DB
- [ ] Admin UI endpoints for quotas and monitoring

## Contributing
Contributions are welcome! Please read CONTRIBUTING.md and follow CONVENTION.md.

## License
Specify your license here (e.g., Apache-2.0, MIT). If absent, all rights reserved by default.
