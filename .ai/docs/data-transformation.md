# Data Transformation & Serialization

This document defines the rules for moving data between external APIs and the internal domain.

## Inbound Flow (External $\to$ Internal)

- **Ingestion**: Use `HttpRequest` (via Ktor) for all external API fetches.
- **Parsing Strategy**:
    - **Primary**: Use **Kotlin Serialization** for all structured JSON responses (`response.body<T>()`).
    - **Legacy**: Use `ObjectParser` (Gson) **only** for unstructured or highly unstable JSON that cannot be handled by Kotlin Serialization.
- **Normalization (The Boundary)**: 
    - `Wrappers` must act as a strict boundary.
    - All API-specific DTOs (e.g., `CrunchyrollResponse`) must be transformed into **Domain Models** (e.g., `Series`, `Episode`) before leaving the `Wrapper` layer.
    - The core application must never encounter API-specific DTOs.

## Outbound Flow (Internal $\to$ External)

- **Transformation (The Factory Rule)**: 
    - All conversions from **Domain Models** to **DTOs** must be performed using the project's dedicated **Factories**.
    - **Never** manually instantiate a DTO within a Service or Controller.
- **Responsibility**: Factories must encapsulate all logic for flattening, renaming, or restructuring data for external consumption.
- **Serialization**: The final step is the serialization of the populated DTO into the required format (JSON, etc.) for the end user.
