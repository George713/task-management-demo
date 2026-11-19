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
const demoBanner = document.querySelector('#demo-banner');
const resetButton = document.querySelector('#reset');

// Only the browser-only demo store can be reset (task-store.js), so only it gets the banner.
if (typeof store.reset === 'function') {
  demoBanner.hidden = false;
  resetButton.addEventListener('click', async () => {
    showError(formError, null);
    try {
      await store.reset();
    } catch (error) {
      await refresh(error.message);
      return;
    }
    await refresh();
  });
}

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

// Shows the latest list. A message, if given, is shown above it (after the list is reloaded).
async function refresh(message) {
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
  showError(listError, message);
}

function renderTask(task) {
  const item = document.createElement('li');
  if (task.done) {
    item.classList.add('done');
  }

  const done = document.createElement('input');
  done.type = 'checkbox';
  done.checked = task.done;
  done.setAttribute('aria-label', `Done: ${task.name}`);
  done.addEventListener('change', async () => {
    done.disabled = true;
    try {
      await store.update(task.id, { done: done.checked });
    } catch (error) {
      // Reload so the checkbox shows the real state again (or the Task disappears if it was deleted).
      await refresh(error.message);
      return;
    }
    await refresh();
  });

  const name = document.createElement('span');
  name.className = 'name';
  name.textContent = task.name;
  name.title = 'Click to rename';
  name.tabIndex = 0;
  name.addEventListener('click', () => startRename(item, name, task));
  name.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      startRename(item, name, task);
    }
  });

  const priority = document.createElement('mark');
  priority.dataset.priority = task.priority;
  priority.textContent = task.priority;

  const remove = document.createElement('button');
  remove.type = 'button';
  remove.textContent = 'Delete';
  remove.setAttribute('aria-label', `Delete: ${task.name}`);
  remove.addEventListener('click', async () => {
    remove.disabled = true;
    try {
      await store.remove(task.id);
    } catch (error) {
      // Typically a 404: the Task was already deleted elsewhere. The reload drops it from the list.
      await refresh(error.message);
      return;
    }
    await refresh();
  });

  item.append(done, name, priority, remove);
  return item;
}

// Replaces the name with an input: Enter saves, Escape (or leaving the field) cancels.
function startRename(item, name, task) {
  const input = document.createElement('input');
  input.className = 'name';
  input.value = task.name;
  input.setAttribute('aria-label', `New name for ${task.name}`);

  const error = document.createElement('small');
  error.className = 'error';
  error.setAttribute('role', 'alert');
  error.hidden = true;

  let saving = false;
  const cancel = () => {
    if (!saving) {
      input.replaceWith(name);
      error.remove();
    }
  };

  input.addEventListener('keydown', async (event) => {
    if (event.key === 'Escape') {
      cancel();
    } else if (event.key === 'Enter') {
      event.preventDefault();
      saving = true;
      showError(error, null);
      try {
        await store.update(task.id, { name: input.value });
      } catch (failure) {
        saving = false;
        showError(error, failure.message);
        input.focus();
        return;
      }
      await refresh();
    }
  });
  input.addEventListener('blur', cancel);

  name.replaceWith(input);
  item.append(error);
  input.focus();
  input.select();
}

function showError(element, message) {
  element.textContent = message ?? '';
  element.hidden = !message;
}

refresh();
