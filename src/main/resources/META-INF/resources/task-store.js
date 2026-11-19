// The only way the page reaches Tasks (D8, ADR-0003). Every store offers:
//
//   list()                      -> Promise<Task[]>  all Tasks in display order
//   create(name, priority)      -> Promise<Task>    priority may be omitted (NORMAL)
//   update(id, {name?, done?})  -> Promise<Task>
//   remove(id)                  -> Promise<void>
//
// A Task has the API's JSON shape: {id, name, done, created, priority}.
// Failures reject with an Error whose message is readable by a person.
//
// The demo store also offers reset() -> Promise<void>, which deletes every stored Task.
// The page shows the demo banner and reset button only for a store that has it.
import { ApiTaskStore } from './api-task-store.js';
import { LocalTaskStore } from './local-task-store.js';

export function createTaskStore(kind) {
  switch (kind) {
    case 'api':
      return new ApiTaskStore();
    case 'local':
      return new LocalTaskStore();
    default:
      throw new Error(`Unknown task store "${kind}" in config.js`);
  }
}
