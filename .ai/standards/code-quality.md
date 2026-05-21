# Code Quality Standards

These standards apply to all codebases within the project, regardless of the programming language used.

## Core Principles

- **Readability**: Code must be clear and easy to understand by any developer.
- **Maintainability & Testability**: Code must be written to be easily maintained and structured to allow for straightforward unit and integration testing.
- **Simplicity over Complexity**: Always prefer the simplest solution that solves the problem. Avoid "over-engineering."
- **Complexity Management**: Prefer early returns to reduce cognitive load. Avoid logic nesting deeper than 3 levels.
- **DRY (Don't Repeat Yourself)**: Avoid code duplication. Logic should be encapsulated in reusable components.
- **Minimal Abstraction**: Do not introduce abstractions (interfaces, inheritance, etc.) unless they are strictly necessary and provide clear value.

## Naming & Structure

- **Explicit Suffixes**: Use suffixes to make the nature of an object explicit when it adds clarity (e.g., `UserDto`, `OrderRepository`, `AuthService`).
- **No Single-Character Variables**: Avoid using single-character variable names. The only exception is for standard loop indices (e.g., `i` in a `for` loop).
