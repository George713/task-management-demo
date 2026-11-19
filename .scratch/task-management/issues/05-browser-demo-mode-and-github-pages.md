# 05: Browser-only demo mode and GitHub Pages deployment

**What to build:** A reviewer opens a GitHub Pages link and uses the same UI without any backend. Tasks live in the browser's `localStorage` and follow the same rules as the real app. A banner makes clear that this is a demo, and a reset button clears it. The real app is unaffected. See `spec.md` (Frontend, Running and delivery), D3, D8–D10 in `DECISIONS.md` and ADR-0003.

**Blocked by:** 02 (Change a Task), 03 (Delete a Task)

**Status:** ready-for-agent

- [ ] A local task store implements the same interface as the API store (list / create / update / remove), keeps Tasks in `localStorage`, and returns them in the same JSON shape.
- [ ] The local store mirrors the server rules: name trimmed, not blank, at most 255 characters; Priority defaults to NORMAL and only LOW/NORMAL/URGENT are accepted; Done starts false; Created set at creation; Priority and Created cannot be changed; same sort order; "not found" for unknown ids. Errors surface in the UI the same way as API errors.
- [ ] Setting the config script to `local` selects the local store with no other change to the page. The Quarkus app still ships with `api`.
- [ ] In local mode only: a persistent banner ("Demo mode: tasks are stored in your browser only, not in Postgres") and a reset button that clears the stored Tasks.
- [ ] Demo Tasks survive a page reload.
- [ ] A GitHub Actions workflow publishes the static frontend to GitHub Pages with the config switched to `local`, on pushes to the default branch.
- [ ] Verified by hand in the browser in both modes: create, rename, mark Done/un-mark, delete, validation errors, ordering, reload, reset.
- [ ] Any new decision is appended to `DECISIONS.md`.
