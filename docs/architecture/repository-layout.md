# Repository layout

YONGTUO is organized as a single repository with separate deployable applications
and infrastructure configuration:

```text
apps/
├── web/       # Nuxt 3 public website
├── admin/     # Vue 3 + Vite management console
└── api/       # Spring Boot monolith API
infra/
├── nginx/     # Reverse-proxy configuration
└── scripts/   # Local and deployment support scripts
docs/
└── architecture/  # Architecture and repository-level documentation
```

The three application directories are intentionally separate so each can be
built and tested independently. The API owns database migrations through
Flyway. Public media is stored through object-storage configuration rather than
in the application source tree.
