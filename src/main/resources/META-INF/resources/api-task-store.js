// Task store backed by the REST API at /api/tasks.
// Relative, so the page also works when it is not served from the site root.
const BASE_URL = 'api/tasks';

export class ApiTaskStore {
  list() {
    return this.#request('GET', BASE_URL);
  }

  create(name, priority) {
    return this.#request('POST', BASE_URL, { name, priority });
  }

  update(id, changes) {
    return this.#request('PATCH', `${BASE_URL}/${encodeURIComponent(id)}`, changes);
  }

  remove(id) {
    return this.#request('DELETE', `${BASE_URL}/${encodeURIComponent(id)}`);
  }

  async #request(method, url, body) {
    let response;
    try {
      response = await fetch(url, {
        method,
        headers: body === undefined ? {} : { 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new Error('Could not reach the server.');
    }
    if (!response.ok) {
      throw new Error(await errorMessage(response));
    }
    return response.status === 204 ? undefined : response.json();
  }
}

async function errorMessage(response) {
  try {
    const body = await response.json();
    if (body && typeof body.message === 'string') {
      return body.message;
    }
  } catch {
    // Not JSON: fall through to the generic message.
  }
  return `Request failed (HTTP ${response.status}).`;
}
