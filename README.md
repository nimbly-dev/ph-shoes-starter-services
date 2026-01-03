# ph-shoes-starter-services
Shared Spring Boot starters for PH-SHOES microservices.

Modules:
- `ph-shoes-starter-services-common-core`: shared models/config/utilities and Dynamo helpers.
- `ph-shoes-starter-services-common-web`: rate limiting interceptor and `/system/status`.
- `ph-shoes-starter-services-common-security`: JWT helpers/filters.

Opt-out defaults:
- Disable JWT auto-config: `phshoes.security.jwt.enabled=false`.
- Disable email crypto: `phshoes.security.email.enabled=false`.

Rate limiting (web):
- Enable: `phshoes.api.rate-limit.enabled=true`.
- Configure limits under `phshoes.api.rate-limit.*`.

Service status (web):
- Enable: `phshoes.status.enabled=true`.
- Optional path: `phshoes.status.path=/system/status`.
- Add `ServiceStatusContributor` beans for dependency checks.

Notes:
- Consumers own OpenAPI config and security rules for `/system/status`.
