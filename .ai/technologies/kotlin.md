# Kotlin Development Standards

These technology-specific standards complement the global Code Quality and Architecture principles.

## Language Features & Best Practices

- **Early Returns**: Prefer early returns to minimize the scope of logic. Avoid logic nesting deeper than 3 levels.
- **Immutability by Default**: Always use `val` for properties and variables unless mutation is strictly required.
- **Null Safety**: 
    - Leverage Kotlin's null safety features. Avoid the use of the double-bang operator (`!!`) at all costs.
    - Use nullable types (`Type?`) only when a null value is a valid and expected state.
    - Prefer the elvis operator (`?:`) and safe calls (`?.`) for handling optionality.
- **Data Classes**: 
    - Use `data class` for all DTOs, configuration objects, and simple data holders.
    - Ensure all properties in `data class` are `val` to maintain immutability.

## Coding Style & Syntax

- **Expression Body**: Use expression body syntax for simple single-function implementations (e.g., `fun getName(): String = name`).
- **Trailing Lambdas**: When a function's last parameter is a lambda, always use the trailing lambda syntax to improve readability.
- **Named and Default Arguments**: Use named arguments in constructors and function calls to make the purpose of parameters explicit (e.g., `ConfigurationField(name = "...", value = "...")`).
- **String Templates**: Always prefer string templates (`"Hello, $name"`) over string concatenation.

## Performance

- **Avoid N+1 Pattern**: Never allow N+1 patterns in database queries or API calls. Always prefer batching or bulk operations to minimize network/disk roundtrips.
- **Measure Before Optimizing**: Never perform "premature optimization." Always use profiling or benchmarks to identify actual bottlenecks before attempting to optimize.
- **Object Allocation Optimization**: Avoid redundant object creation. If an object is constructed repeatedly within a loop or a method, hoist its creation to a constant or a higher-scoped variable to reduce CPU and memory consumption.

## Error & Exception Handling

- **Explicit Error States**: Where possible, use sealed classes or sealed interfaces to represent different error or result states instead of throwing generic exceptions.
- **Result Pattern**: For complex operations (like API wrappers), consider returning a `Result<T>` or a custom `sealed class` to make the error path explicit and part of the function signature.
