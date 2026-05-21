# SQL & Database Standards

These standards ensure data integrity, security, and optimal performance of the database layer, covering both raw SQL and ORM usage.

## Liquibase (Database Migrations)

- **Migration Identification**: Every migration series is identified by a unique `id` based on the current timestamp in milliseconds.
- **Changeset Naming Convention**: Each changeset within a feature group follows the pattern `${id}-XX`, where `XX` is an incremental index (01, 02, ...) representing the sequence of changes for that specific feature.
- **Mandatory Metadata**: Every migration context/file must explicitly define two variables:
    - `id`: The current timestamp in milliseconds.
    - `author`: The developer responsible for the change.
- **Pre-conditions & Safety**: Every changeset MUST include a `precondition`.
    - To prevent partial or broken executions, always use **`onFail="MARK_RAN"`** for preconditions.
- **Immutability of Migrations**: Once a changeset has been deployed, never modify it. Create a new changeset to revert or fix.

## Hibernate & Repository Patterns

- **CriteriaBuilder & JPA Metamodel**: When constructing queries, **always** use the JPA Metamodel (e.g., `Anime_.uuid`) instead of hardcoded strings. This ensures type safety and prevents breakage during schema changes.
- **Standardized Pagination**: Never implement custom pagination. Always use the pagination implementation provided in **`AbstractRepository`** to ensure consistency and performance.
 
- **Query Construction & Caching**: Always use the established query construction utilities provided within the project. This is critical to ensure that the queries are compatible with and benefit from **Hibernate's second-level cache**.

## Transaction Management

- **Read Operations**: **Never** initiate a new transaction for read-only operations. Use the existing transaction context to minimize overhead.
- **Write Operations**: Transactions must be explicitly managed and used **only** for operations that involve data modification (Create, Update, Delete).

## Security & Performance

- **SQL Injection Prevention**: Never use string concatenation. Always use parameterized queries or Criteria API.
- **Avoid N+1 Query Problem**: Use `JOIN FETCH` or entity graphs to retrieve related entities in a single roundtrip.
- **Efficient Data Retrieval**: Always specify required columns in projections and use pagination for any user-facing list.
