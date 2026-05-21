# 🛡️ Standard: Security

## 🎯 Objective
To ensure the absolute confidentiality, integrity, and availability of all user data and system components, strictly adhering to GDPR (RGPD) principles.

## ⚖️ Hierarchy of Authority
This Standard is the supreme authority of the project. It overrides all `feature:` and `tech:` rules.

## 🔐 1. Identity & Access Control (AuthZ/AuthN)

### 1.1 Ownership Enforcement (Anti-BOLA)
- **Rule:** No user shall access a resource belonging to another user.
- **Implementation:** Every endpoint handling user-specific resources (e.g., `/api/user/{id}/profile`) MUST verify that the `{id}` in the path or body matches the `Principal.id` extracted from the authenticated JWT or Session.
- **Mandate:** Developers must use repository methods that enforce ownership (e.g., `findOwnedById(resourceId, currentUserId)`) rather than generic lookup methods.

### 1.2 Authentication Scopes
- **User Scope (JWT):** Standard users are authenticated via JWT. They are strictly limited to their own resources.
- **Admin Scope (Session):** Administrative functions are handled via Session-based authentication. Access to the `admin` route group is exclusively reserved for users with an active Admin session.

### 1.3 Identity Leakage
- **Rule:** Never disclose the internal identity of the system or the administrative structure to non-privileged users.

## 🛡️ 2. Data Protection & Integrity

### 2.1 Data Transfer Objects (DTO) Mandatory
- **Rule:** An application Entity (`Entity`) must never be exposed directly in an HTTP response.
- **Requirement:** Every controller endpoint MUST map the internal `Entity` to a public-facing `DTO`. This prevents accidental exposure of sensitive fields (e.g., `password_hash`, `internal_role`, `is_admin`).

### 2.2 Sensitive Data Handling (GDPR/RGPD)
- **Rule:** Personal Identifiable Information (PII) such as emails, names, or addresses must be protected.
- **No Plain Text:** Sensitive credentials (passwords, tokens) must never be stored in plain text.
- **Logging:** No PII (email, phone, etc.) shall be logged in plain text. Use masking (e.g., `u***@email.com`) or anonymized identifiers in all application logs and traces.

### 2.3 Input Sanitization
- **Rule:** Never trust user-provided input.
- **Implementation:** All input parameters (Path, Query, Body) must be validated against strict constraints (regex, length, type) and sanitized to prevent SQL Injection and XSS.

## 🌐 3. Web Security Invariants

### 3.1 Architectural Security Headers
*Note: These are architectural invariants implemented at the framework level and are non-negotiable.*
- **Anti-Clickjacking:** The `X-Frame-Options: DENY` header is automatically injected to all responses.
- **Anti-MIME-Sniffing:** The `X-Content-Type-Options: nosniff` header is automatically injected.
- **Anti-XSS:** A strict `Content-Security-Policy` (CSP) is automatically enforced for all web-facing routes.
- **Transport Security:** `Strict-Transport-Security` (HSTS) is globally enforced to ensure HTTPS-only communication.
