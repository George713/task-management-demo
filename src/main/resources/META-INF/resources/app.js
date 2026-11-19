import { TASK_STORE } from './config.js';
import { createTaskStore } from './task-store.js';

const store = createTaskStore(TASK_STORE);

const form = document.querySelector('#create-form');
const nameInput = document.querySelector('#name');
const prioritySelect = document.querySelector('#priority');
const formError = document.querySelector('#form-error');
const listError = document.querySelector('#list-error');
const list = document.querySelector('#task-list');
const empty = document.querySelector('#empty');

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  showError(formError, null);
  try {
    await store.create(nameInput.value, prioritySelect.value);
    form.reset();
    nameInput.focus();
  } catch (error) {
    showError(formError, error.message);
    return;
  }
  await refresh();
});

async function refresh() {
  showError(listError, null);
  let tasks;
  try {
    tasks = await store.list();
  } catch (error) {
    showError(listError, error.message);
    return;
  }
  list.replaceChildren(...tasks.map(renderTask));
  empty.hidden = tasks.length > 0;
}

function renderTask(task) {
  const item = document.createElement('li');
  if (task.done) {
    item.classList.add('done');
  }

  const done = document.createElement('input');
  done.type = 'checkbox';
  done.checked = task.done;
  done.disabled = true;
  done.setAttribute('aria-label', task.done ? 'Done' : 'Not done');

  const name = document.createElement('span');
  name.className = 'name';
  name.textContent = task.name;

  const priority = document.createElement('mark');
  priority.dataset.priority = task.priority;
  priority.textContent = task.priority;

  item.append(done, name, priority);
  return item;
}

function showError(element, message) {
  element.textContent = message ?? '';
  element.hidden = !message;
}

refresh();
