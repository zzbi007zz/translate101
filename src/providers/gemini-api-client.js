// Gemini API adapter for translation

const GEMINI_API_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent';

export async function translateWithGemini(text, targetLang, apiKey) {
  const prompt = buildPrompt(text, targetLang);
  const payload = {
    contents: [
      {
        parts: [{ text: prompt }],
      },
    ],
  };

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 10000); // 10s timeout

  try {
    const response = await fetch(`${GEMINI_API_ENDPOINT}?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(`API error: ${response.status} - ${errorText}`);
    }

    const data = await response.json();
    return parseGeminiResponse(data);
  } catch (error) {
    clearTimeout(timeoutId);
    throw error;
  }
}

function buildPrompt(text, targetLang) {
  const langMap = {
    en: 'English',
    vi: 'Vietnamese',
    es: 'Spanish',
    fr: 'French',
    de: 'German',
    ja: 'Japanese',
    ko: 'Korean',
    zh: 'Chinese',
  };

  const language = langMap[targetLang] || targetLang;
  return `Translate the following text to ${language}. Only return the translation, no explanations:\n\n${text}`;
}

function parseGeminiResponse(data) {
  const candidates = data.candidates || [];
  const firstCandidate = candidates[0] || {};
  const content = firstCandidate.content || {};
  const parts = content.parts || [];
  const firstPart = parts[0] || {};
  return firstPart.text ? firstPart.text.trim() : '';
}
