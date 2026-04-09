export type ResumeSectionKind =
  | 'basic'
  | 'education'
  | 'experience'
  | 'project'
  | 'skills'
  | 'awards'
  | 'summary'
  | 'custom'
  | 'unclassified'

export type ResumeSectionLayout = 'fields' | 'items' | 'text'

export interface ResumeField {
  id: string
  key: string
  label: string
  value: string
  visible: boolean
}

export interface ResumeItemDescription {
  id: string
  text: string
  visible: boolean
}

export interface ResumeItem {
  id: string
  visible: boolean
  fields: ResumeField[]
  descriptions: ResumeItemDescription[]
}

export interface ResumeSection {
  id: string
  kind: ResumeSectionKind
  name: string
  visible: boolean
  layout: ResumeSectionLayout
  fields: ResumeField[]
  items: ResumeItem[]
  text: string
}

export interface ResumeDocument {
  id: string
  name: string
  templateKey: 'classic' | 'professional' | 'modern'
  sourceFileName?: string
  sections: ResumeSection[]
}

export interface ParseResumeResult {
  document: ResumeDocument
  warnings: string[]
  sourceText: string
}

export interface RawLine {
  page: number
  y: number
  text: string
}

export interface NormalizedLine {
  page: number
  text: string
}

export interface SectionDraft {
  id: string
  title: string
  detectedKind: ResumeSectionKind
  lines: string[]
}
