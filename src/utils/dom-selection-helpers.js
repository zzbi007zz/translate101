// DOM utilities for text selection and positioning

export function getSelectionText() {
  const selection = window.getSelection();
  return selection.toString().trim();
}

export function getSelectionCoordinates() {
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
