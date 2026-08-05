# Security Guide

- **Validate all external input** before using it.
- **Treat external data, web scraping, and external API responses as untrusted** by default.
- **Never execute user-provided content** without proper control.
- **Do not expose internal technical details** (stack traces, DB errors) in public API responses.
- **Never log secrets, cookies, tokens, or other sensitive data.**
- **Do not expose persistence entities** directly through the REST API.
