# 03: Delete a Task

**What to build:** A user can delete a Task from the list with a delete button, and the Task is gone for good. See `spec.md` (API contract, Frontend) and D12 in `DECISIONS.md`.

**Blocked by:** 02 (Change a Task). Not a logical dependency: tickets 02 and 03 touch the same resource and page, so they run one after the other to avoid conflicts.

**Status:** ready-for-agent

- [ ] `DELETE /api/tasks/{id}` returns 204 and the Task no longer appears in `GET /api/tasks`.
- [ ] `DELETE` on an unknown id returns 404.
- [ ] UI: each row has a delete button. After deleting, the list refreshes. A 404 (already deleted elsewhere) is shown inline.
- [ ] The API task store implements `remove(id)`.
- [ ] Tests (same style as ticket 01) cover the successful delete (204, gone from the list) and 404 on an unknown id. `./mvnw test` passes.
- [ ] Any new decision is appended to `DECISIONS.md`.
