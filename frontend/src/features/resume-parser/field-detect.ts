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
  { key: 'education', label: '最高学历', aliases: ['学历', '最高学历', '毕业院校'] },
  { key: 'gender', label: '性别', aliases: ['性别'] },
  { key: 'age', label: '年龄', aliases: ['年龄'] },
  { key: 'arrival', label: '到岗时间', aliases: ['到岗时间', '到岗情况', '入职时间'] }
]

const DEFAULT_BASIC_FIELDS = [
  { key: 'name', label: '姓名' },
  { key: 'intent', label: '求职意向' },
  { key: 'phone', label: '联系电话' },
  { key: 'email', label: '电子邮箱' },
  { key: 'city', label: '意向城市' },
  { key: 'currentCity', label: '现居地' },
  { key: 'experience', label: '工作经验' },
  { key: 'education', label: '最高学历' },
  { key: 'gender', label: '性别' },
  { key: 'age', label: '年龄' },
  { key: 'arrival', label: '到岗时间' },
  { key: 'avatar', label: '头像' }
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

function upsertBasicField(fields: ResumeField[], key: string, value: string, label?: string) {
  if (!value.trim()) {
    return
  }
  const target = fields.find((field) => field.key === key)
  if (target) {
    target.value = value.trim()
    if (label) {
      target.label = label
    }
    return
  }
  fields.push(createField(label ?? key, value.trim(), key))
}

function inferBasicFieldByValue(line: string, fields: ResumeField[]) {
  const text = line.trim()
  const phone = text.match(/1[3-9]\d{9}/)?.[0]
  if (phone) {
    upsertBasicField(fields, 'phone', phone, '联系电话')
  }

  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0]
  if (email) {
    upsertBasicField(fields, 'email', email, '电子邮箱')
  }

  const gender = text.match(/(^|\s)(男|女)(\s|$)/)?.[2]
  if (gender) {
    upsertBasicField(fields, 'gender', gender, '性别')
  }

  const age = text.match(/(\d{2})\s*岁/)?.[1]
  if (age) {
    upsertBasicField(fields, 'age', `${age}岁`, '年龄')
  }

  const intent = text.match(/(Java开发工程师|Java|后端开发工程师|后端开发|前端开发工程师|前端开发|产品经理|运营|测试工程师|算法工程师|数据分析师|设计师)/)?.[1]
  if (intent) {
    upsertBasicField(fields, 'intent', intent, '求职意向')
  }

  const cities = text.match(/(上海|北京|深圳|广州|杭州|苏州|成都|南京|武汉|西安|长沙|天津|重庆)/g)
  if (cities?.length) {
    if (/现居|所在地|居住地/.test(text)) {
      upsertBasicField(fields, 'currentCity', cities[0], '现居地')
    } else if (/意向|目标/.test(text)) {
      upsertBasicField(fields, 'city', cities[0], '意向城市')
    } else {
      if (!fields.find((field) => field.key === 'currentCity')?.value) {
        upsertBasicField(fields, 'currentCity', cities[0], '现居地')
      } else if (!fields.find((field) => field.key === 'city')?.value) {
        upsertBasicField(fields, 'city', cities[0], '意向城市')
      }
    }
  }

  const education = text.match(/(博士|硕士|研究生|本科|大专|专科|高中)/)?.[1]
  if (education) {
    upsertBasicField(fields, 'education', education, '最高学历')
  }

  const school = text.match(/([\u4e00-\u9fa5]{2,20}(大学|学院))/)?.[1]
  if (school) {
    const existingEducation = fields.find((field) => field.key === 'education')?.value ?? ''
    if (!existingEducation.includes(school)) {
      upsertBasicField(fields, 'education', existingEducation ? `${school} ${existingEducation}`.trim() : school, '最高学历')
    }
  }

  const experience = text.match(/(\d+\s*年(?:工作)?经验|应届生)/)?.[1]
  if (experience) {
    upsertBasicField(fields, 'experience', experience, '工作经验')
  }

  const arrival = text.match(/(一周内到岗|两周内到岗|一个月内到岗|随时到岗)/)?.[1]
  if (arrival) {
    upsertBasicField(fields, 'arrival', arrival, '到岗时间')
  }

  if (!fields.find((field) => field.key === 'name')?.value) {
    const leadingName = text.match(/^([\u4e00-\u9fa5]{2,4})(?=\s|$)/)?.[1]
    if (leadingName && !/(上海|北京|深圳|广州)/.test(leadingName)) {
      upsertBasicField(fields, 'name', leadingName, '姓名')
    }
  }
}

function inferBasicFieldFromDenseLine(line: string, fields: ResumeField[]) {
  const text = line.replace(/\s+/g, ' ').trim()
  if (!text) {
    return
  }

  const parts = text.split(' ').filter(Boolean)
  if (parts.length < 2) {
    return
  }

  const nameCandidate = parts[0]
  if (/^[\u4e00-\u9fa5]{2,4}$/.test(nameCandidate)) {
    upsertBasicField(fields, 'name', nameCandidate, '姓名')
  }

  const rest = parts.slice(1).join(' ')
  inferBasicFieldByValue(rest, fields)
}

function parseTopSummaryLine(line: string, fields: ResumeField[]) {
  const text = line.replace(/\s+/g, ' ').trim()
  if (!text) {
    return
  }

  const tokens = text.split(' ').filter(Boolean)
  if (!tokens.length) {
    return
  }

  const [first, ...restTokens] = tokens
  if (/^[\u4e00-\u9fa5]{2,4}$/.test(first)) {
    upsertBasicField(fields, 'name', first, '姓名')
  }

  const cityToken = restTokens.find((token) => /(上海|北京|深圳|广州|杭州|苏州|成都|南京|武汉|西安|长沙|天津|重庆)/.test(token))
  if (cityToken) {
    upsertBasicField(fields, 'currentCity', cityToken, '现居地')
  }

  const arrivalToken = restTokens.find((token) => /(一周内到岗|两周内到岗|一个月内到岗|随时到岗)/.test(token))
  if (arrivalToken) {
    upsertBasicField(fields, 'arrival', arrivalToken, '到岗时间')
  }

  const intentTokens = restTokens.filter((token) => token !== cityToken && token !== arrivalToken)
  if (intentTokens.length) {
    upsertBasicField(fields, 'intent', intentTokens.join(' '), '求职意向')
  }
}

function parseProfileLine(line: string, fields: ResumeField[]) {
  const text = line.replace(/\s+/g, ' ').trim()
  if (!text) {
    return
  }

  inferBasicFieldByValue(text, fields)

  const phone = text.match(/1[3-9]\d{9}/)?.[0]
  const email = text.match(/[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i)?.[0]

  if (phone) {
    upsertBasicField(fields, 'phone', phone, '联系电话')
  }
  if (email) {
    upsertBasicField(fields, 'email', email, '电子邮箱')
  }
}

function parseEducationProfileLine(line: string, fields: ResumeField[]) {
  const text = line.replace(/\s+/g, ' ').trim()
  if (!text) {
    return
  }

  const school = text.match(/([\u4e00-\u9fa5]{2,30}(大学|学院))/)?.[1]
  const degree = text.match(/(博士|硕士|研究生|本科|大专|专科|高中)/)?.[1]
  const time = text.match(/\d{4}[./-]\d{1,2}\s*[~\-至]\s*(?:\d{4}[./-]\d{1,2}|今)/)?.[0]

  let major = ''
  if (school) {
    const afterSchool = text.slice(text.indexOf(school) + school.length)
    major = afterSchool
      .replace(/^[\s\-–—]+/, '')
      .replace(/\b(博士|硕士|研究生|本科|大专|专科|高中)\b/, '')
      .replace(/\d{4}[./-]\d{1,2}\s*[~\-至]\s*(?:\d{4}[./-]\d{1,2}|今)/, '')
      .replace(/[\s\-–—]+$/g, '')
      .trim()
  }

  if (school) {
    upsertBasicField(fields, 'school', school, '毕业院校')
  }
  if (major) {
    upsertBasicField(fields, 'major', major, '专业')
  }
  if (degree) {
    upsertBasicField(fields, 'education', degree, '最高学历')
  }
  if (time) {
    upsertBasicField(fields, 'educationTime', time, '学制时间')
  }
}

function parseBasic(lines: string[]): Pick<ResumeSection, 'layout' | 'fields' | 'items' | 'text'> {
  const fields = DEFAULT_BASIC_FIELDS.map((item) => createField(item.label, '', item.key))
  const leftovers: string[] = []

  const upsertField = (nextField: ResumeField) => {
    const target = fields.find((field) => field.key === nextField.key)
    if (target) {
      target.value = nextField.value
      return
    }
    fields.push(nextField)
  }

  lines.forEach((line, index) => {
    if (index === 0) {
      parseTopSummaryLine(line, fields)
      return
    }

    if (index <= 2) {
      parseProfileLine(line, fields)
    }

    if (/大学|学院|本科|硕士|博士|专科|高中/.test(line)) {
      parseEducationProfileLine(line, fields)
    }
  })

  for (const rawLine of lines) {
    inferBasicFieldFromDenseLine(rawLine, fields)
    const parts = splitInlinePairs(rawLine)
    let matchedInLine = false
    for (const part of parts) {
      const field = inferBasicField(part)
      if (field) {
        upsertField(field)
        matchedInLine = true
      } else if (parts.length > 1) {
        inferBasicFieldByValue(part, fields)
        leftovers.push(part)
      }
    }

    if (!matchedInLine && parts.length === 1) {
      const fallback = inferBasicField(rawLine)
      if (fallback) {
        upsertField(fallback)
      } else {
        inferBasicFieldByValue(rawLine, fields)
        leftovers.push(rawLine)
      }
    }
  }

  const consumedValues = new Set(
    fields
      .filter((field) => field.value)
      .map((field) => field.value)
  )

  const unresolved = leftovers.filter((line) => ![...consumedValues].some((value) => value && line.includes(value)))

  if (unresolved.length) {
    fields.push(createField('补充信息', unresolved.join(' / '), 'extra'))
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
  let currentBlockLabel = ''

  const flush = () => {
    if (current.fields.length || current.descriptions.length) {
      items.push(current)
      current = createItem()
      currentBlockLabel = ''
    }
  }

  for (const raw of lines) {
    const line = stripBullet(raw)
    if (!line) {
      flush()
      continue
    }

    if (kind === 'project' && /^项目[一二三四五六七八九十\d]+[:：]/.test(line)) {
      flush()
      current.fields.push(createField('项目', line.replace(/^项目[一二三四五六七八九十\d]+[:：]\s*/, '').trim(), 'projectName'))
      continue
    }

    const kv = parseKeyValueLine(line)
    if (kv && /学校|专业|学历|时间|GPA|公司|职位|项目|角色|机构|等级|奖项|技术栈|项目描述|核心贡献|链接|地址/.test(kv.label)) {
      const normalizedKey = /项目/.test(kv.label) ? 'projectName' : /技术栈/.test(kv.label) ? 'stack' : /链接|地址/.test(kv.label) ? 'link' : kv.label
      current.fields.push(createField(kv.label, kv.value, normalizedKey))
      currentBlockLabel = ''
      continue
    }

    if (/^(技术栈|项目描述|核心贡献|工作内容|职责|成果)[:：]?$/.test(line)) {
      currentBlockLabel = line.replace(/[:：]/g, '')
      continue
    }

    if (/\d{4}[./-]\d{1,2}(\s*[-~至]\s*(今|\d{4}[./-]\d{1,2}))?/.test(line) && current.fields.length) {
      current.fields.push(createField('时间', line.match(/\d{4}[./-]\d{1,2}(\s*[-~至]\s*(今|\d{4}[./-]\d{1,2}))?/)?.[0] ?? line, 'time'))
      currentBlockLabel = ''
      continue
    }

    if ((kind === 'education' && /(大学|学院|学校)/.test(line)) || (kind !== 'education' && /(.{2,30})(公司|项目|科技|中心|实验室)/.test(line))) {
      flush()
      current.fields.push(createField(kind === 'education' ? '学校' : kind === 'project' ? '项目' : '公司', line, kind === 'education' ? 'school' : kind === 'project' ? 'projectName' : 'company'))
      currentBlockLabel = ''
      continue
    }

    if (/^(负责|参与|主导|完成|实现|优化|搭建|获得|荣获|证书|奖项)/.test(line) || current.fields.length) {
      appendDescription(current, currentBlockLabel ? `${currentBlockLabel}：${line}` : line)
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
