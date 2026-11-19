# 02: Change a Task: rename and mark Done

**What to build:** A user can mark a Task as Done or un-mark it with one click, and rename a Task by clicking its name and editing it in place. Priority and Created stay fixed: any attempt to change them through the API is rejected. See `spec.md` (API contract, Backend structure, Frontend) and D4, D11, D12 in `DECISIONS.md`.

**Blocked by:** 01 (Create and list Tasks)

**Status:** ready-for-agent

- [ ] The Task entity offers `rename(name)` (trims) and `markDone(boolean)` as its only ways to change a Task.
- [ ] `PATCH /api/tasks/{id}` with `{name?, done?}` returns 200 and the updated Task. Fields left out stay unchanged; an empty body leaves the Task unchanged.
- [ ] `PATCH` returns 400 with a readable message for a blank or over-long name, malformed JSON, or a body containing `priority` or `created`.
- [ ] `PATCH` on an unknown id returns 404.
- [ ] UI: the Done checkbox in each row toggles Done. Clicking the name opens an inline editor: Enter saves, Escape cancels without saving. Errors (validation, 404 for a Task deleted elsewhere) are shown inline. The list refreshes after a change and reflects the new order.
- [ ] The API task store implements `update(id, {name?, done?})`.
- [ ] Tests (same style as ticket 01) cover: rename with trimming, rename rejected when blank or over-long, mark Done and un-mark Done, both fields in one PATCH, empty PATCH, rejection of `priority` and `created`, 404 on an unknown id, and that a Done Task moves below open Tasks in the list. `./mvnw test` passes.
- [ ] Any new decision is appended to `DECISIONS.md`.
