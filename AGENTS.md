# LLM Agent Instructions

As an AI agent, your primary directive is to adhere to the established patterns and architectural principles of this project. Your goal is to write clean, maintainable, and secure code that aligns with the existing multi-module codebase.

This file contains your core, non-negotiable rules. For detailed implementation guidance, refer to the linked documents in the `guidelines` directory.

## 1. The Prime Directive: Respect the Multi-Module Architecture

The project follows a strict multi-module architecture. **Do not violate this structure.**

- **`:models`**: Pure Kotlin domain objects (`Anime`, `Episode`, `Metadata`). Zero external dependencies.
- **`:database`**: Persistence layer with Exposed. Internal tables & entities (`AnimeTable`, `AnimeEntity`). Entities **must never leak** outside this module.
- **`:api`, `:site`, `:admin`, `:jobs`**: Standalone application modules. Each depends on `:models` and `:database`, but applications **must remain independent** of each other.

For a deeper understanding, read the [Architecture Guide](guidelines/ARCHITECTURE.md) and [Models Guide](guidelines/MODELS.md).

## 2. Layer Responsibilities & Data Protection

- **Controllers / Routes** (`:api`, `:site`, `:admin`) expose HTTP routes and must not contain heavy business logic.
- **Services** orchestrate business rules.
- **Repositories** (`:database`) handle persistence and Exposed queries only, extending `AbstractRepository`.
- **API DTOs** must remain separate from domain models and database entities.

Consult the [Database Guide](guidelines/DATABASE.md) and [API Conventions](guidelines/API_CONVENTIONS.md) for details.

## 3. Security is Not Optional

- Treat **all** external inputs as untrusted. This includes API requests, data from external sources, and web scrapers.
- Validate and sanitize all incoming data before use.
- **Never** expose internal technical details (e.g., stack traces, database errors) in public-facing errors.
- **Never** log sensitive information like passwords, tokens, or cookies.

Consult the [Security Guide](guidelines/SECURITY.md) for detailed instructions.

## 4. Write Code for Humans (and other AIs)

- Write all code, comments, and documentation in **English**.
- Your code must be readable and self-explanatory. Add comments only when the logic is non-obvious.
- **Reuse existing patterns and conventions.** Before writing new code, understand how similar features are implemented in the project.

Refer to the style and testing guides for specifics:
- [Code Style Guide](guidelines/CODE_STYLE.md)
- [Kotlin Conventions](guidelines/KOTLIN_CONVENTIONS.md)
- [API Conventions](guidelines/API_CONVENTIONS.md)
- [Testing Guide](guidelines/TESTING.md)

## 5. Before Submitting Changes

Before concluding your task, perform a final check to ensure you have followed all directives:
- Does your code fit into the correct module (`:models`, `:database`, `:api`, `:site`, `:admin`, `:jobs`)?
- Are database entities kept strictly internal to `:database`?
- Are domain models in `:models` pure with no framework dependencies?
- Have you reused existing patterns (e.g., `AbstractRepository` DSL)?
- Is all external data handled securely?
- Do all unit and integration tests pass?

Consult the other guides for [Performance](guidelines/PERFORMANCE.md) rules as needed.
