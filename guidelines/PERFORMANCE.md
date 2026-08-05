# Performance Guide

- **Avoid external calls** when valid cached data is available.
- **Deduplicate input** before starting unnecessary work.
- **Limit database round trips.** Use batch queries or projections for list views.
- **Keep critical paths simple and predictable.**
- **Keep retry mechanisms bounded and measurable.**
