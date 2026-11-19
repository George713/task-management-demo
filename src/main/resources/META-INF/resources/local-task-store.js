// Task store that keeps Tasks in the browser's localStorage: the GitHub Pages demo (D9, ADR-0003).
// It mirrors the server's rules and messages, so the demo behaves like the real app.
// The real rules live on the server; this copy is the accepted duplication.
const PRIORITIES = ['LOW', 'NORMAL', 'URGENT'];
const PRIORITY_ORDER = { URGENT: 0, NORMAL: 1, LOW: 2 };
const CHANGEABLE_FIELDS = ['done', 'name'];
const MAX_NAME_LENGTH = 255;
const STORAGE_KEY = 'task-management-demo.tasks';
const STORAGE_UNAVAILABLE = 'Browser storage is not available, so the demo cannot store Tasks.';

export class LocalTaskStore {
  async list() {
    return this.#load().tasks.sort(displayOrder);
  }

  async create(name, priority) {
    const chosen = priority ?? 'NORMAL';
    if (!PRIORITIES.includes(chosen)) {
      throw new Error(`Invalid priority '${chosen}'. Allowed values: ${PRIORITIES.join(', ')}`);
    }
    const data = this.#load();
    const task = {
      id: data.nextId,
      name: validName(name),
      done: false,
      created: new Date().toISOString(),
      priority: chosen,
    };
    data.nextId += 1;
    data.tasks.push(task);
    this.#save(data);
    return task;
  }

  async update(id, changes = {}) {
    const unknown = Object.keys(changes).find((field) => !CHANGEABLE_FIELDS.includes(field));
    if (unknown !== undefined) {
      throw new Error(`Field '${unknown}' is not allowed. Allowed fields: ${CHANGEABLE_FIELDS.join(', ')}`);
    }
    const { name, done } = changes;
    if (done != null && typeof done !== 'boolean') {
      throw new Error('Invalid value for done');
    }
    const newName = name == null ? undefined : validName(name);

    const data = this.#load();
    const task = data.tasks.find((t) => t.id === Number(id));
    if (!task) {
      throw notFound(id);
    }
    if (newName !== undefined) {
      task.name = newName;
    }
    if (done != null) {
      task.done = done;
    }
    this.#save(data);
    return task;
  }

  async remove(id) {
    const data = this.#load();
    const index = data.tasks.findIndex((t) => t.id === Number(id));
    if (index === -1) {
      throw notFound(id);
    }
    data.tasks.splice(index, 1);
    this.#save(data);
  }

  // Demo only: forget every stored Task and start the ids again from 1.
  async reset() {
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      throw new Error(STORAGE_UNAVAILABLE);
    }
  }

  // Always returns fresh objects parsed from storage, so callers can't change stored Tasks by accident.
  #load() {
    let raw;
    try {
      raw = localStorage.getItem(STORAGE_KEY);
    } catch {
      throw new Error(STORAGE_UNAVAILABLE);
    }
    if (raw === null) {
      return { nextId: 1, tasks: [] };
    }
    try {
      const data = JSON.parse(raw);
      if (Number.isInteger(data.nextId) && Array.isArray(data.tasks)) {
        return data;
      }
    } catch {
      // Fall through to the message below.
    }
    throw new Error('The stored demo Tasks could not be read. Use "Reset demo" to start over.');
  }

  #save(data) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch {
      throw new Error('The demo Tasks could not be saved in browser storage.');
    }
  }
}

// Same rules and messages as the API: a string (D47), trimmed, then not blank and at most 255 characters.
function validName(name) {
  if (name != null && typeof name !== 'string') {
    throw new Error('Invalid value for name');
  }
  const trimmed = (name ?? '').trim();
  if (trimmed.length === 0) {
    throw new Error('Name must not be blank');
  }
  if (trimmed.length > MAX_NAME_LENGTH) {
    throw new Error(`Name must be at most ${MAX_NAME_LENGTH} characters`);
  }
  return trimmed;
}

function notFound(id) {
  return new Error(`Task ${id} was not found. It may have been deleted.`);
}

// Open before Done, then URGENT, NORMAL, LOW, then newest Created first, then newest id (D13, D25).
function displayOrder(a, b) {
  return (
    Number(a.done) - Number(b.done) ||
    PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority] ||
    Date.parse(b.created) - Date.parse(a.created) ||
    b.id - a.id
  );
}
