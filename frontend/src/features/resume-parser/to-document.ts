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

function sortSections(sections: ResumeSection[]) {
  return [...sections].sort((left, right) => {
    const leftIndex = DEFAULT_SECTION_ORDER.indexOf(left.kind)
    const rightIndex = DEFAULT_SECTION_ORDER.indexOf(right.kind)
    return (leftIndex === -1 ? 99 : leftIndex) - (rightIndex === -1 ? 99 : rightIndex)
  })
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
      fields: [],
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

  const normalizedSections = sections.filter((section) => section.fields.length || section.items.length || section.text)

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
      sections: sortSections(normalizedSections)
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
