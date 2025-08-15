# Kotlin Style Guide

This document defines consistent coding practices for **Kotlin** backend development in this project.  
It incorporates secure coding principles aligned with ISMS-P requirements.

---

## 1. General Principles

- **Readability First**: Write code that is easy to read and maintain.
- **Security by Default**: Follow secure coding practices from the start.
- **Consistency**: Apply consistent formatting, naming, and structure across all modules.
- **Peer Review Compliance**: Code should pass security-focused code review before merging.

---

## 2. Code Formatting
- Use **4 spaces** for indentation.
- Max line length: **120 characters**.
- Use UTF-8 encoding.
- Always use braces `{}` for control structures, even for single-line statements.

---

## 3. Naming Conventions
- Classes & Interfaces: `PascalCase` (e.g., `UserService`, `AuthController`).
- Functions & Properties: `camelCase` (e.g., `getUserData`, `isAuthorized`).
- Constants: `UPPER_SNAKE_CASE` (e.g., `DEFAULT_TIMEOUT`).
- Packages: **lowercase**, no underscores (e.g., `com.example.project`).
- Avoid abbreviations unless widely accepted.

---

## 4. Language Features
- Use **data classes** for DTOs and immutable structures.
- Prefer `val` over `var` unless mutation is required.
- Use `sealed class` or `enum class` for controlled type hierarchies.
- Avoid using nullable types unless necessary; prefer explicit null checks.

---

## 5. Error Handling
- Throw specific exceptions, not generic ones.
- Log errors with context, but do **not** log sensitive information.
- Use `Result` or sealed classes for domain-level error handling.

---

## 6. Secure Coding (ISMS-P Aligned)
- Validate all external inputs.
- Sanitize user inputs before using them in queries or rendering.
- Use parameterized queries (no string concatenation in SQL).
- Mask or hash sensitive data.
- Never hardcode credentials or secrets.

---

## 7. Documentation & Comments
- Use KDoc (`/** ... */`) for public APIs.
- Explain the **why**, not just the **what**.
- Keep documentation up to date with code changes.

---

## 8. Testing Requirements
- Use JUnit5 and MockK for testing.
- Write unit tests for core logic.
- Ensure all tests pass before submitting a PR.

---

## 9. Security Checklist Before Merging
- [ ] No hardcoded secrets or credentials.
- [ ] Inputs are validated and sanitized.
- [ ] Sensitive data is masked or encrypted.
- [ ] Dependencies are updated and free from known vulnerabilities.
- [ ] Debug code is removed.
