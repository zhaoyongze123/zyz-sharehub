import type { NormalizedLine, RawLine } from './types'

export function createId(prefix = 'id') {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}-${Date.now().toString(36)}`
}

export function normalizeText(value: string) {
  return value
    .replace(/\u00a0/g, ' ')
    .replace(/[：﹕]/g, ':')
    .replace(/[（]/g, '(')
    .replace(/[）]/g, ')')
    .replace(/[【]/g, '[')
    .replace(/[】]/g, ']')
    .replace(/[•·▪◦●]/g, '•')
    .replace(/\s+/g, ' ')
    .trim()
}

export function stripBullet(value: string) {
  return value.replace(/^[•\-*]\s*/, '').trim()
}

export function normalizeLines(lines: RawLine[]): NormalizedLine[] {
  const merged = lines
    .map((line) => ({
      page: line.page,
      text: normalizeText(line.text)
    }))
    .filter((line) => line.text.length)

  const counts = new Map<string, number>()
  merged.forEach((line) => {
    counts.set(line.text, (counts.get(line.text) ?? 0) + 1)
  })

  return merged.filter((line) => {
    if (line.text.length <= 2) {
      return false
    }
    if (/^第?\d+\s*页$/.test(line.text)) {
      return false
    }
    if (/^page\s+\d+$/i.test(line.text)) {
      return false
    }
    if (/^[\-=_.]{4,}$/.test(line.text)) {
      return false
    }
    return (counts.get(line.text) ?? 0) <= 3
  })
}

export function linesToText(lines: Array<{ text: string }>) {
  return lines.map((line) => line.text).join('\n').trim()
}
