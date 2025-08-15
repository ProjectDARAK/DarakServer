# Contributing Guidelines

Thank you for your interest in contributing!  
This project follows secure software development practices aligned with the [ISMS-P certification framework](https://isms-p.kisa.or.kr) requirements for **information system development security** and **secure software lifecycle management**.

These guidelines apply to **all code contributions** — backend, frontend, and infrastructure code — to ensure the security, integrity, and maintainability of the project.

---

## 1. General Principles

- **Security by Design**: Implement security controls from the earliest stages of development, not as an afterthought.
- **Least Privilege**: Code should enforce minimum required permissions for all operations.
- **Traceability**: All code changes must be traceable to an issue, task, or documented request.
- **Compliance Awareness**: Follow applicable ISMS-P controls related to software development, particularly:
  - Secure requirements definition and review
  - Separation of development, testing, and production environments
  - Secure source code management

---

## 2. Before You Start

1. **Discuss first**  
   Open a GitHub Issue to propose your change and discuss the approach before coding.

2. **Familiarize yourself with secure coding standards**  
   - [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices/)
   - Language/framework-specific security guides (e.g., Kotlin/Spring, JavaScript/React)

3. **Use an assigned branch**  
   - Never commit directly to `main` or `release` branches.
   - Use feature branches named like:
     ```
     feature/<short-description>
     fix/<short-description>
     security/<short-description>
     ```

---

## 3. Secure Development Requirements (ISMS-P aligned)

### 3.1 Define and Review Security Requirements
- Ensure each new feature or change has **security requirements** documented.
- Conduct peer reviews focused on security impact.

### 3.2 Code Review and Approval
- All pull requests require **at least one security-focused review**.
- Reviewers must check for:
  - Input validation and output encoding
  - Authentication and authorization enforcement
  - Secure data handling (no sensitive data in logs, no hardcoded secrets)
  - Dependency vulnerabilities (use tools like `npm audit`, `gradle dependencyCheck`)

### 3.3 Source Code Management
- Store code in a **version-controlled repository** with access control.
- Commit messages should be clear and reference related issue numbers.
- Never commit:
  - Secrets, API keys, credentials
  - Production database connection strings
  - Personal data

### 3.4 Environment Separation
- Development, testing, and production environments must be **logically and physically separated**.
- Test with **sanitized or synthetic data** — no real personal data in dev/test.

### 3.5 Test Data Security
- Anonymize or mask any sample data.
- Remove test accounts and debug code before merging.

### 3.6 Deployment Readiness
- Verify that configuration files are free of sensitive values.
- Ensure that debug endpoints, console logs, and stack traces are disabled in production.

---

## 4. Reporting Security Issues

If you discover a security vulnerability:

1. **Do not open a public issue.**
2. Email the maintainers at **[developer@cultr.camp]**.
3. Include:
   - Steps to reproduce
   - Potential impact
   - Suggested mitigation

---

## 5. Commit and Pull Request Checklist

Before submitting a PR, verify:

- [ ] Code follows project style guidelines
- [ ] Security controls are in place
- [ ] No hardcoded secrets or credentials
- [ ] Input is validated and output is encoded
- [ ] Dependencies are up to date and vulnerability-free
- [ ] Tests are included and pass locally
- [ ] Changes are documented

---

## 6. References

- [Project coding convention](./CONVENTION.md)
- [ISMS-P Certification Criteria — 2.8 Secure Development Controls]  
- [OWASP Top Ten](https://owasp.org/www-project-top-ten/)
- [ISO/IEC 27001 Annex A.14 — System Acquisition, Development and Maintenance]

---

By contributing to this repository, you agree to follow these guidelines and uphold the project’s security and quality standards.
