# Swappable task store so one frontend serves the real app and a browser-only demo

Reviewers should be able to try the UI on GitHub Pages without running anything, but the real submission must persist to Postgres through the Quarkus API. The single frontend talks to tasks only through a small task-store interface with two implementations: one calls the REST API, the other keeps tasks in `localStorage`. A one-line `config.js` selects the implementation (the app ships with `api`; the Pages deploy swaps it to `local`). We rejected a separate demo page because two UIs would drift apart, and auto-detecting an available API because the mode should be explicit.

## Consequences

The browser store has to mirror the server's rules (defaults, validation, sort order). That small duplication is accepted. The real rules live on the server, which is what gets graded.
