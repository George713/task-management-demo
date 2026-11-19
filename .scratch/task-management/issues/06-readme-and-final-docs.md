# 06: README and final documentation pass

**What to build:** A reviewer landing on the repository understands within a minute what this is, how to try it (demo link), how to run it (Docker Compose or plain Java), how to develop and test it, and where the reasoning lives. The decision log matches what was actually built. See `spec.md` (Running and delivery, Further Notes) and D7, D17, D18 in `DECISIONS.md`.

**Blocked by:** 04 (Run with Docker Compose and with plain Java), 05 (Browser-only demo mode and GitHub Pages)

**Status:** ready-for-agent

- [ ] The README replaces the placeholder. It covers: a one-paragraph summary of the app and the brief, the GitHub Pages demo link (with the note that the demo stores data in the browser only), `docker compose up --build`, the plain-Java route, dev mode, running the tests (Docker required), the tech stack, and links to `DECISIONS.md`, `docs/adr/` and `GLOSSARY.md`.
- [ ] The README notes the deliberate interpretations of the brief: Priority is fixed after creation, and fields that aren't allowed in a request are rejected.
- [ ] Every command in the README has been run as written, from a clean checkout.
- [ ] `DECISIONS.md` is checked against the code: any decision that changed during implementation is updated or superseded, and the "How this was built" section reflects the actual process.
- [ ] `GLOSSARY.md` and the ADRs still match the code; any contradiction is surfaced rather than silently fixed.
- [ ] Any new decision is appended to `DECISIONS.md`.
