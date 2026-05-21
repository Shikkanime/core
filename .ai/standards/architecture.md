# Architectural Principles

These principles must be strictly followed to ensure a scalable, maintainable, and predictable codebase.

## Core Design Principles

- **SOLID, DRY, KISS, SRP**: Strict adherence to Single Responsibility, Don't Repeat Yourself, Keep It Simple, and SOLID principles.
- **Layer Separation**: Never mix transport (API/CLI/Interface), business logic, and persistence (Database/Filesystem) layers. Each layer must have a distinct and isolated responsibility.
- **Business Logic Isolation**: Business logic must never reside within controllers, entry points, or UI components. It should be encapsulated in dedicated service or domain layers.

## Data & State Management

- **Immutability First**: Always prefer immutable data structures and objects. Avoid in-place modifications to prevent unexpected side effects.
- **Explicit Side Effects**: Side effects (I/O, database writes, network calls, etc.) must be explicitly declared and easily identifiable. Avoid "hidden" mutations or logic that happens as a byproduct of a simple getter or property access.
