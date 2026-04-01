// DOM utilities for text selection and positioning

export function getSelectionText() {
  const activeEl = document.activeElement;

  // Check if selection is in form field (input/textarea)
  // Exclude password fields for security
  if (
    activeEl &&
    (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')
  ) {
    // Skip password fields
    if (activeEl.type === 'password') {
      return '';
    }

    const start = activeEl.selectionStart;
    const end = activeEl.selectionEnd;
    if (start !== end && start !== null && end !== null) {
      return activeEl.value.substring(start, end).trim();
    }
  }

  // Fallback to document selection (contenteditable, text nodes)
  const selection = window.getSelection();
  return selection.toString().trim();
}

export function getSelectionCoordinates() {
  const activeEl = document.activeElement;

  // Check if selection is in form field (input/textarea)
  if (
    activeEl &&
    (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA')
  ) {
    // Use element's bounding box for form fields
    const rect = activeEl.getBoundingClientRect();
    return {
      x: rect.left + window.scrollX,
      y: rect.top + window.scrollY,
      width: rect.width,
      height: rect.height,
    };
  }

  // Fallback to document selection (contenteditable, text nodes)
  const selection = window.getSelection();
  if (!selection.rangeCount) return null;

  const range = selection.getRangeAt(0);
  const rect = range.getBoundingClientRect();

  return {
    x: rect.left + window.scrollX,
    y: rect.top + window.scrollY,
    width: rect.width,
    height: rect.height,
  };
}

export function clearSelection() {
  const selection = window.getSelection();
  if (selection) {
    selection.removeAllRanges();
  }
}
