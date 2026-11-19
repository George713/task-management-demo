# JSON REST API with a plain-JavaScript single page

The backend exposes tasks as a JSON REST API. The UI is one static HTML page with plain JavaScript that calls the API, served by the Quarkus app itself. We rejected server-rendered templates (Qute + htmx) because a separate API is a seam that can be tested on its own and that a browser-only demo can swap out. We rejected a React/Vue app because an npm build pipeline isn't worth it for three use cases.
