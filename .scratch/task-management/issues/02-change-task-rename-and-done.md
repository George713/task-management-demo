# 02: Change a Task: rename and mark Done

**What to build:** A user can mark a Task as Done or un-mark it with one click, and rename a Task by clicking its name and editing it in place. Priority and Created stay fixed: any attempt to change them through the API is rejected. See `spec.md` (API contract, Backend structure, Frontend) and D4, D11, D12 in `DECISIONS.md`.

**Blocked by:** 01 (Create and list Tasks)

**Status:** resolved

- [x] The Task entity offers `rename(name)` (trims) and `markDone(boolean)` as its only ways to change a Task.
- [x] `PATCH /api/tasks/{id}` with `{name?, done?}` returns 200 and the updated Task. Fields left out stay unchanged; an empty body leaves the Task unchanged.
- [x] `PATCH` returns 400 with a readable message for a blank or over-long name, malformed JSON, or a body containing `priority` or `created`.
- [x] `PATCH` on an unknown id returns 404.
- [x] UI: the Done checkbox in each row toggles Done. Clicking the name opens an inline editor: Enter saves, Escape cancels without saving. Errors (validation, 404 for a Task deleted elsewhere) are shown inline. The list refreshes after a change and reflects the new order.
- [x] The API task store implements `update(id, {name?, done?})`.
- [x] Tests (same style as ticket 01) cover: rename with trimming, rename rejected when blank or over-long, mark Done and un-mark Done, both fields in one PATCH, empty PATCH, rejection of `priority` and `created`, 404 on an unknown id, and that a Done Task moves below open Tasks in the list. `./mvnw test` passes.
- [x] Any new decision is appended to `DECISIONS.md`.

## Comments

**Resolved 2026-11-19** (merged into `feature/task-management`): branch `ticket/02-change-task` (ec8090d). 23 tests passed, incl. the Done-ordering test deferred from 01. UI later verified in a browser during ticket 05 and the code-review fix. Decisions D28-D31.
