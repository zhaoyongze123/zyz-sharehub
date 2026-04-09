import { createId, stripBullet } from './normalize'
import type { ResumeField, ResumeItem, ResumeSection, ResumeSectionKind, SectionDraft } from './types'

const BASIC_FIELD_ALIASES: Array<{ key: string; label: string; aliases: string[] }> = [
  { key: 'name', label: '姓名', aliases: ['姓名', '名字'] },
  { key: 'intent', label: '求职意向', aliases: ['求职意向', '意向岗位', '目标岗位'] },
  { key: 'phone', label: '联系电话', aliases: ['联系电话', '手机', '电话', '手机号'] },
  { key: 'email', label: '电子邮箱', aliases: ['邮箱', '电子邮箱', 'email', 'e-mail'] },
  { key: 'city', label: '意向城市', aliases: ['意向城市', '目标城市'] },
  { key: 'currentCity', label: '现居地', aliases: ['现居地', '所在地', '当前城市'] },
  { key: 'experience', label: '工作经验', aliases: ['工作年限', '工作经验', '经验'] },
  { key: 'education', label: '最高学历', aliases: ['学历', '最高学历'] }
]

function createField(label: string, value: string, key = label) {
  return {
    id: createId('field'),
    key,
    label,
    value: value.trim(),
    visible: true
  }
}

function createItem(): ResumeItem {
  return {
    id: createId('item'),
    visible: true,
    fields: [],
    descriptions: []
  }
}

function appendDescription(item: ResumeItem, text: string) {
  const cleaned = stripBullet(text)
  if (!cleaned) {
    return
  }
  item.descriptions.push({
    id: createId('desc'),
    text: cleaned,
    visible: true
  })
}

function matchBasicAlias(label: string) {
  const lowered = label.toLowerCase()
  return BASIC_FIELD_ALIASES.find((item) => item.aliases.some((alias) => lowered === alias.toLowerCase()))
}

function parseKeyValueLine(line: string) {
  const colonMatch = line.match(/^([^:]{1,20}):\s*(.+)$/)
  if (colonMatch) {
    return { label: colonMatch[1].trim(), value: colonMatch[2].trim() }
  }

  const spacedMatch = line.match(/^([一-龥A-Za-z]{2,12})\s+(.+)$/)
  if (spacedMatch) {
    return { label: spacedMatch[1].trim(), value: spacedMatch[2].trim() }
  }

  return null
}

function splitInlinePairs(line: string) {
  return line
    .split(/[|｜]/)
    .map((part) => part.trim())
    .filter(Boolean)
}

function inferBasicField(line: string): ResumeField | null {
  const kv = parseKeyValueLine(line)
  if (kv) {
    const alias = matchBasicAlias(kv.label)
    return createField(alias?.label ?? kv.label, kv.value, alias?.key ?? kv.label)
  }

  if (/^[\u4e00-\u9fa5]{2,4}$/.test(line)) {
    return createField('姓名', line, 'name')
  }
  if (/1[3-9]\d{9}/.test(line)) {
    return createField('联系电话', line.match(/1[3-9]\d{9}/)?.[0] ?? line, 'phone')
  }
  if (/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i.test(line)) {
    return createField('电子邮箱', line.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0] ?? line, 'email')
  }
  if (/(现居|意向|求职|经验|学历)/.test(line)) {
    const guessed = parseKeyValueLine(line.replace(/\s{2,}/g, ' '))
    if (guessed) {
      return createField(guessed.label, guessed.value, guessed.label)
    }
  }
  return null
}

function parseBasic(lines: string[]): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  const fields: ResumeField[] = []
  const leftovers: string[] = []

  for (const rawLine of lines) {
    const parts = splitInlinePairs(rawLine)
    let matchedInLine = false
    for (const part of parts) {
      const field = inferBasicField(part)
      if (field) {
        fields.push(field)
        matchedInLine = true
      } else if (parts.length > 1) {
        leftovers.push(part)
      }
    }

    if (!matchedInLine && parts.length === 1) {
      const fallback = inferBasicField(rawLine)
      if (fallback) {
        fields.push(fallback)
      } else {
        leftovers.push(rawLine)
      }
    }
  }

  if (leftovers.length) {
    fields.push(createField('补充信息', leftovers.join(' / '), 'extra'))
  }

  return {
    layout: 'fields',
    fields,
    items: [],
    text: ''
  }
}

function parseTimeline(lines: string[], kind: ResumeSectionKind): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  const items: ResumeItem[] = []
  let current = createItem()

  const flush = () => {
    if (current.fields.length || current.descriptions.length) {
      items.push(current)
      current = createItem()
    }
  }

  for (const raw of lines) {
    const line = stripBullet(raw)
    if (!line) {
      flush()
      continue
    }

    const kv = parseKeyValueLine(line)
    if (kv && /学校|专业|学历|时间|GPA|公司|职位|项目|角色|机构|等级|奖项/.test(kv.label)) {
      current.fields.push(createField(kv.label, kv.value, kv.label))
      continue
    }

    if (/\d{4}[./-]\d{1,2}(\s*[-~至]\s*(今|\d{4}[./-]\d{1,2}))?/.test(line) && current.fields.length) {
      current.fields.push(createField('时间', line.match(/\d{4}[./-]\d{1,2}(\s*[-~至]\s*(今|\d{4}[./-]\d{1,2}))?/)?.[0] ?? line, 'time'))
      continue
    }

    if ((kind === 'education' && /(大学|学院|学校)/.test(line)) || (kind !== 'education' && /(.{2,30})(公司|项目|科技|中心|实验室)/.test(line))) {
      flush()
      current.fields.push(createField(kind === 'education' ? '学校' : kind === 'project' ? '项目' : '公司', line, kind === 'education' ? 'school' : kind === 'project' ? 'projectName' : 'company'))
      continue
    }

    if (/^(负责|参与|主导|完成|实现|优化|搭建|获得|荣获|证书|奖项)/.test(line) || current.fields.length) {
      appendDescription(current, line)
      continue
    }

    if (!current.fields.length) {
      current.fields.push(createField(kind === 'project' ? '项目' : kind === 'education' ? '学校' : '标题', line))
    } else {
      appendDescription(current, line)
    }
  }

  flush()

  return {
    layout: 'items',
    fields: [],
    items,
    text: ''
  }
}

function parseSkills(lines: string[]): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  const fields: ResumeField[] = []
  const texts: string[] = []

  for (const raw of lines) {
    const line = stripBullet(raw)
    const kv = parseKeyValueLine(line)
    if (kv) {
      fields.push(createField(kv.label, kv.value, kv.label))
      continue
    }
    const pairMatch = line.match(/^(.{1,20})(熟悉|熟练|精通|掌握|了解)(.+)?$/)
    if (pairMatch) {
      fields.push(createField(pairMatch[1].trim(), `${pairMatch[2]}${pairMatch[3] ?? ''}`.trim(), pairMatch[1].trim()))
      continue
    }
    texts.push(line)
  }

  return {
    layout: texts.length && !fields.length ? 'text' : 'fields',
    fields,
    items: [],
    text: texts.join('\n')
  }
}

function parseSummary(lines: string[]): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  return {
    layout: 'text',
    fields: [],
    items: [],
    text: lines.join('\n').trim()
  }
}

export function parseSectionContent(draft: SectionDraft): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  switch (draft.detectedKind) {
    case 'basic':
      return parseBasic(draft.lines)
    case 'education':
    case 'experience':
    case 'project':
    case 'awards':
      return parseTimeline(draft.lines, draft.detectedKind)
    case 'skills':
      return parseSkills(draft.lines)
    case 'summary':
      return parseSummary(draft.lines)
    case 'custom':
      return {
        layout: 'text',
        fields: [],
        items: [],
        text: draft.lines.join('\n').trim()
      }
    default:
      return {
        layout: 'text',
        fields: [],
        items: [],
        text: draft.lines.join('\n').trim()
      }
  }
}
