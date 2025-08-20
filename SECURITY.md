# Security Policy

This policy applies to all repositories under Project DARAK (Backend: Kotlin/Spring Boot, Frontend: Flutter). It is aligned with ISMS‑P principles (management system, protection measures, and lifecycle requirements for personal data).

---

## 🔒 Reporting a Vulnerability

Please **do not** open a public issue. Use the private channel below.

* **Email**: [developer@cultr.camp](mailto:developer@cultr.camp)
* or **[Security vulnerability report](https://github.com/ProjectDARAK/DarakServer/security)**
* **Recommended subject format**: `[Vuln Report] <Affected Area> - <Brief Summary>`
* **Required details**

    * Impact scope and reproducible steps (attach a minimal PoC if possible)
    * Estimated severity/impact (e.g., privilege escalation, auth bypass, information disclosure)
    * Affected commit/version, runtime/build environment
    * Logs/screenshots (**mask any sensitive or personal data**)

We follow **Coordinated Vulnerability Disclosure (CVD)**. After we ship a fix within an agreed window, we will publish a security advisory covering affected assets/versions and mitigations. Researcher credit is provided upon consent.

> **Safe Harbor**: Good‑faith security research will not trigger legal action. Do **not** perform service disruption (DoS), data exfiltration/destruction, or unauthorized collection of personal data. Accessing or modifying any account or data (yours or others) without explicit permission is prohibited.

---

## 🧭 Scope

* This policy covers Project DARAK’s **backend API servers** (Spring Boot/Kotlin) and **Flutter‑based clients**.
* Vulnerabilities in third‑party services/libraries should be reported to their vendors. If the issue is caused by our integration, please report it under this policy.

---

## 📦 Supported Versions

* Principle: security patches are applied to the **`main`**\*\* branch\*\* and the **latest stable release** with priority.
* Where necessary, we may consider backporting to the previous minor (N‑1), subject to project constraints and compatibility.

---

## 🛡️ Security Requirements for Contributors

* **No secrets in commits**: Do not include API keys/tokens/credentials/internal URLs. Use environment variables or runtime injection.
* **Input validation & output encoding**: Validate all untrusted input; prevent SQL/path/command injection. Never log secrets or sensitive data.
* **Least privilege**: Design/implement access to functions and data with the minimum required privileges.
* **Environment segregation**: Separate dev/test/prod environments and data. Use anonymized/pseudonymized test data.
* **Dependency hygiene**: Update regularly and run vulnerability checks (e.g., tools in the Gradle/Flutter ecosystems).
* **Mandatory review**: At least one reviewer must perform a security‑focused code review before merge.

> **PR checklist (recommended)**: No secrets, input validation, encryption applied, no vulnerable dependencies, tests included.

---

## 🔐 Data Protection

* **In transit**: HTTPS/TLS is mandatory. For web builds, open CORS only to server‑approved origins.
* **At rest**: Encrypt sensitive data and personal data. Do not store secrets in plaintext on mobile/desktop/web clients.
* **PII minimization & masking**: Minimize collection of personal data in logs/error reports/analytics and mask where necessary.

---

## 📱 Frontend (Flutter) Hardening

* Inject runtime configuration via **build‑time defines** (e.g., `--dart-define=API_BASE_URL`). Do not store secrets in the repository.
* **HTTPS required**, even in tests; do not weaken security (e.g., accepting untrusted certificates).
* For Android releases, use `--obfuscate` and `--split-debug-info`. Store symbol files in a secure external vault.
* **No real network calls in tests**: use mocks/fakes.

---

## 🖥️ Backend (Spring Boot/Kotlin) Hardening

* Use **JPA/parameter binding** for DB access (no string concatenation). Do not include secrets in exceptions/logs.
* **AuthZ/AuthN**: Token‑based authentication with MFA (OTP/WebAuthn) is assumed as a baseline.
* Inject secrets via properties/env (e.g., environment variables/secret manager). Disable debug/test endpoints in production.

---

## 🧪 Testing & CI/CD

* Test data must be **anonymized/pseudonymized**. Never use production data.
* CI should run static analysis and tests. Fail the pipeline on security rule violations (e.g., secret leakage).

---

## 🧯 Incident Response

1. Intake & triage (reproduce and assess impact)
2. Mitigation and fix preparation (isolate branches, add monitoring)
3. Release & advisory (affected versions, mitigations, update steps)
4. Postmortem (preventive actions, documentation)

Critical issues may be hot‑fixed with immediate advisories. If breaking changes or release risk is high, provide temporary mitigations.

---

## 🧾 Vulnerability Severity

* Prioritize using **CVSS v3.x** as guidance. Adjust upward based on our threat model and project context where appropriate.

---

## 📚 References

* Project rules: CONTRIBUTING.md, Coding Conventions (Flutter/Kotlin)
* Security baseline: ISMS‑P certification criteria (management controls, protection measures, and personal‑data lifecycle requirements)

---

## 📬 Contact

Security inquiries: [developer@cultr.camp](mailto:developer@cultr.camp)

---

This policy may evolve as the project changes. We will maintain history with version and date for any updates.
- V1.0, 2025-08-20
