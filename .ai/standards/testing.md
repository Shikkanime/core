# Testing Standards

These standards ensure the reliability, stability, and maintainability of the codebase through a robust testing strategy.

## Core Requirements

- **Minimum Coverage**: Maintain a minimum of 80% code coverage across the project.
- **Business Logic Coverage**: Every component containing business logic must be covered by automated tests. No exceptions.
- **Regression Prevention**: Tests must be part of the automated CI/CD pipeline. A failure in any test must block the deployment/merging process.

## Test Pyramid & Levels

- **Unit Tests**: Focus on isolated business logic. Use mocks/stubs for internal dependencies to ensure speed and determinism.
- **Integration & Regression Tests**: 
    - **CRITICAL**: For external services and APIs (e.g., Crunchyroll), **DO NOT** mock the network calls or the API response. These tests must interact with the real (or live-like) interface to detect breaking changes in the upstream provider's structure or behavior.
    - Verify the actual interaction between our system and the external provider to ensure the scrapers/parsers are still valid.
- **End-to-End (E2E) / Smoke Tests**: Verify critical user flows and the overall system health in a production-like environment.

## Testing Strategy & Best Practices

- **Boundary & Edge Case Testing**: Always test extreme values, null/empty inputs, and unexpected data formats.
- **Error Path Testing**: Do not only test the "happy path." Explicitly test how the system handles errors, exceptions, and invalid states to ensure graceful degradation.
- **Mocking Strategy**:
    - **DO** use mocks for internal components to isolate logic and maintain performance.
    - **DO** use mocks to simulate specific error scenarios that are hard to trigger naturally (e.g., random network timeouts, 500 Internal Server Error, corrupted payloads).
    - **DO NOT** mock external APIs that are the core focus of the project's functionality.
- **Test Readability**: Tests should serve as living documentation. Use descriptive names that explain the *scenario* and the *expected outcome* (e.g., `should_return_error_when_user_is_not_found`).
- **Deterministic Tests**: Avoid tests that depend on the system clock, random number generators, or the order of execution.
