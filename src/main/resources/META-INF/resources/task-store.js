// The only way the page reaches Tasks (D8, ADR-0003). Every store offers:
//
//   list()                      -> Promise<Task[]>  all Tasks in display order
//   create(name, priority)      -> Promise<Task>    priority may be omitted (NORMAL)
//   update(id, {name?, done?})  -> Promise<Task>
//   remove(id)                  -> Promise<void>
//
// A Task has the API's JSON shape: {id, name, done, created, priority}.
// Failures reject with an Error whose message is readable by a person.
import { ApiTaskStore } from './api-task-store.js';

export function createTaskStore(kind) {
  switch (kind) {
    case 'api':
      return new ApiTaskStore();
    default:
      throw new Error(`Unknown task store "${kind}" in config.js`);
  }
}
