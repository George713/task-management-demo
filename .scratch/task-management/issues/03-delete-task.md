# 03: Delete a Task

**What to build:** A user can delete a Task from the list with a delete button, and the Task is gone for good. See `spec.md` (API contract, Frontend) and D12 in `DECISIONS.md`.

**Blocked by:** 02 (Change a Task). Not a logical dependency: tickets 02 and 03 touch the same resource and page, so they run one after the other to avoid conflicts.

**Status:** resolved

- [x] `DELETE /api/tasks/{id}` returns 204 and the Task no longer appears in `GET /api/tasks`.
- [x] `DELETE` on an unknown id returns 404.
- [x] UI: each row has a delete button. After deleting, the list refreshes. A 404 (already deleted elsewhere) is shown inline.
- [x] The API task store implements `remove(id)`.
- [x] Tests (same style as ticket 01) cover the successful delete (204, gone from the list) and 404 on an unknown id. `./mvnw test` passes.
- [x] Any new decision is appended to `DECISIONS.md`.

## Comments

**Resolved 2026-11-19** (merged into `feature/task-management`): branch `ticket/03-delete-task` (2409795). 26 tests passed. UI verified in a browser during ticket 05 and the code-review fix. Decisions D36-D37.
