// Chrome Storage API wrappers for API keys and settings

export async function getApiKey(provider = 'gemini') {
  const key = `apiKey_${provider}`;
  const result = await chrome.storage.sync.get(key);
  return result[key] || null;
}

export async function setApiKey(provider, key) {
  await chrome.storage.sync.set({ [`apiKey_${provider}`]: key });
}

export async function getSetting(key, defaultValue) {
  const result = await chrome.storage.sync.get(key);
  return result[key] !== undefined ? result[key] : defaultValue;
}

export async function setSetting(key, value) {
  await chrome.storage.sync.set({ [key]: value });
}
