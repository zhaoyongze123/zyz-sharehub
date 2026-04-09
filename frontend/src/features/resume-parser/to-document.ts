import { createId } from './normalize'
import { parseSectionContent } from './field-detect'
import { detectSections } from './section-detect'
import type { ParseResumeResult, ResumeDocument, ResumeSection, ResumeSectionKind } from './types'

const DEFAULT_SECTION_ORDER: ResumeSectionKind[] = [
  'basic',
  'education',
  'experience',
  'project',
  'skills',
  'awards',
  'summary'
]

const DEFAULT_SECTION_NAMES: Record<ResumeSectionKind, string> = {
  basic: '基本信息',
  education: '教育经历',
  experience: '工作经历',
  project: '项目经历',
  skills: '技能特长',
  awards: '证书/荣誉',
  summary: '自我评价',
  custom: '自定义模块',
  unclassified: '未识别内容'
}

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
  { key: 'avatar', label: '头像' }
]

function createDefaultBasicFields() {
  return DEFAULT_BASIC_FIELDS.map((field) => ({
    id: createId('field'),
    key: field.key,
    label: field.label,
    value: '',
    visible: true
  }))
}

function ensureBasicSection(section: ResumeSection) {
  if (section.kind !== 'basic') {
    return section
  }

  const merged = createDefaultBasicFields()
  section.fields.forEach((field) => {
    const target = merged.find((entry) => entry.key === field.key)
    if (target) {
      target.value = field.value
      target.visible = field.visible
      target.label = field.label || target.label
    } else {
      merged.push(field)
    }
  })

  return {
    ...section,
    fields: merged
  }
}

export function createEmptyDocument(templateKey: ResumeDocument['templateKey'] = 'classic'): ResumeDocument {
  return {
    id: createId('resume'),
    name: '默认简历',
    templateKey,
    sections: DEFAULT_SECTION_ORDER.map((kind) => ({
      id: createId(kind),
      kind,
      name: DEFAULT_SECTION_NAMES[kind],
      visible: true,
      layout: kind === 'summary' ? 'text' : kind === 'basic' || kind === 'skills' ? 'fields' : 'items',
      fields: kind === 'basic' ? createDefaultBasicFields() : [],
      items: [],
      text: ''
    }))
  }
}

export function buildResumeDocumentFromLines(
  lines: Array<{ page: number; text: string }>,
  templateKey: ResumeDocument['templateKey'],
  sourceFileName?: string
): ParseResumeResult {
  const warnings: string[] = []
  const drafts = detectSections(lines)

  const sections = drafts.map<ResumeSection>((draft) => {
    const parsed = parseSectionContent(draft)
    return {
      id: draft.id,
      kind: draft.detectedKind,
      name:
        draft.detectedKind === 'custom'
          ? draft.title || DEFAULT_SECTION_NAMES.custom
          : draft.detectedKind === 'unclassified'
            ? DEFAULT_SECTION_NAMES.unclassified
            : DEFAULT_SECTION_NAMES[draft.detectedKind] || draft.title,
      visible: true,
      layout: parsed.layout,
      fields: parsed.fields,
      items: parsed.items,
      text: parsed.text
    }
  })

  const normalizedSections = sections
    .map((section) => ensureBasicSection(section))
    .filter((section) => section.fields.length || section.items.length || section.text || section.kind === 'basic')

  if (!normalizedSections.length) {
    warnings.push('未识别出结构化模块，已保留原始文本到未识别内容。')
    normalizedSections.push({
      id: createId('section'),
      kind: 'unclassified',
      name: DEFAULT_SECTION_NAMES.unclassified,
      visible: true,
      layout: 'text',
      fields: [],
      items: [],
      text: lines.map((line) => line.text).join('\n').trim()
    })
  }

  return {
    warnings,
    sourceText: lines.map((line) => line.text).join('\n').trim(),
    document: {
      id: createId('resume'),
      name: sourceFileName ? sourceFileName.replace(/\.pdf$/i, '') : '导入简历',
      templateKey,
      sourceFileName,
      // 导入后的模块顺序严格保持 PDF 自上而下的原始顺序，不做二次重排。
      sections: normalizedSections
    }
  }
}

export function buildResumeDocumentFromText(
  sourceText: string,
  templateKey: ResumeDocument['templateKey'],
  sourceFileName?: string
): ParseResumeResult {
  const lines = sourceText
    .split('\n')
    .map((text, index) => ({ page: 1, text: text.trim(), y: index }))
    .filter((line) => line.text.length)

  return buildResumeDocumentFromLines(lines, templateKey, sourceFileName)
}
