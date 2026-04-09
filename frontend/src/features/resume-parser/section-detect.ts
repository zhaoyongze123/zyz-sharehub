import { createId, stripBullet } from './normalize'
import type { NormalizedLine, ResumeSectionKind, SectionDraft } from './types'

export const SECTION_ALIASES: Record<Exclude<ResumeSectionKind, 'custom' | 'unclassified'>, string[]> = {
  basic: ['基本信息', '个人信息', '个人资料', '联系方式', '个人概况'],
  education: ['教育经历', '教育背景', '学历信息', '学习经历'],
  experience: ['工作经历', '实习经历', '职业经历', '任职经历'],
  project: ['项目经历', '项目经验', '项目实践', '项目背景'],
  skills: ['技能特长', '专业技能', '技能', '个人技能', '核心能力'],
  awards: ['证书荣誉', '荣誉奖项', '证书', '获奖经历', '荣誉', '资格证书'],
  summary: ['自我评价', '个人评价', '个人总结', '自我介绍', '个人优势']
}

function detectAliasKind(text: string) {
  const cleaned = stripBullet(text).replace(/\s/g, '')
  for (const [kind, aliases] of Object.entries(SECTION_ALIASES) as Array<[ResumeSectionKind, string[]]>) {
    if (aliases.some((alias) => cleaned === alias.replace(/\s/g, '') || cleaned.includes(alias.replace(/\s/g, '')))) {
      return kind
    }
  }
  return null
}

function looksLikeHeading(text: string) {
  const cleaned = stripBullet(text)
  if (!cleaned || cleaned.length > 16) {
    return false
  }
  if (/^\d{1,2}岁\s*(男|女)$/.test(cleaned)) {
    return false
  }
  if (/1[3-9]\d{9}/.test(cleaned) || /@/.test(cleaned)) {
    return false
  }
  if (/(求职意向|意向岗位|目标岗位|联系电话|手机|邮箱|电子邮箱|现居地|所在地|毕业院校|专业|学历|到岗)/.test(cleaned)) {
    return false
  }
  if (/(大学|学院|学校|本科|硕士|博士|大专|专科|高中)/.test(cleaned)) {
    return false
  }
  if (/\d{4}[./-]\d{1,2}\s*[~\-至]/.test(cleaned)) {
    return false
  }
  if (/[，。；？！:：]/.test(cleaned)) {
    return false
  }
  if (/^(技术栈|项目描述|核心贡献|工作内容|职责|成果|角色|时间|项目一|项目二|项目三|项目四)$/i.test(cleaned)) {
    return false
  }
  return /[一-龥A-Za-z]/.test(cleaned)
}

function belongsToBasicSection(text: string) {
  const cleaned = stripBullet(text)
  return (
    /^\d{1,2}岁\s*(男|女)$/.test(cleaned) ||
    /1[3-9]\d{9}/.test(cleaned) ||
    /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i.test(cleaned) ||
    /(求职意向|意向岗位|目标岗位|联系电话|手机|邮箱|电子邮箱|现居地|所在地|毕业院校|专业|学历|到岗)/.test(cleaned) ||
    /(大学|学院|学校|本科|硕士|博士|大专|专科|高中)/.test(cleaned) ||
    /\d{4}[./-]\d{1,2}\s*[~\-至]\s*(?:\d{4}[./-]\d{1,2}|今|至今)/.test(cleaned)
  )
}

function inferKindFromContent(lines: string[]): ResumeSectionKind {
  const content = lines.join('\n')
  if (/(学校|学院|专业|学历|GPA|主修)/.test(content)) {
    return 'education'
  }
  if (/(公司|职责|负责|任职|工作内容|业绩)/.test(content)) {
    return 'experience'
  }
  if (/(项目|技术栈|成果|负责模块|角色)/.test(content)) {
    return 'project'
  }
  if (/(证书|荣誉|奖学金|获奖|资格)/.test(content)) {
    return 'awards'
  }
  if (/(技能|熟悉|掌握|语言能力|工具)/.test(content)) {
    return 'skills'
  }
  if (/(姓名|电话|手机|邮箱|求职意向|现居)/.test(content)) {
    return 'basic'
  }
  return 'custom'
}

export function detectSections(lines: NormalizedLine[]): SectionDraft[] {
  const drafts: SectionDraft[] = []
  let current: SectionDraft | null = null

  const pushCurrent = () => {
    if (!current) {
      return
    }
    current.lines = current.lines.filter(Boolean)
    if (current.detectedKind === 'custom') {
      current.detectedKind = inferKindFromContent(current.lines)
    }
    drafts.push(current)
    current = null
  }

  for (const line of lines) {
    const aliasKind = detectAliasKind(line.text)
    const heading = aliasKind || looksLikeHeading(line.text)

    if (aliasKind) {
      pushCurrent()
      current = {
        id: createId('section'),
        title: stripBullet(line.text),
        detectedKind: aliasKind,
        lines: []
      }
      continue
    }

    if (!current) {
      current = {
        id: createId('section'),
        title: '基本信息',
        detectedKind: 'basic',
        lines: []
      }
    }

    if (current.detectedKind === 'basic' && belongsToBasicSection(line.text)) {
      current.lines.push(line.text)
      continue
    }

    if (heading && current.lines.length >= 2) {
      pushCurrent()
      current = {
        id: createId('section'),
        title: stripBullet(line.text),
        detectedKind: 'custom',
        lines: []
      }
      continue
    }

    current.lines.push(line.text)
  }

  pushCurrent()

  const normalized = drafts.filter((draft) => draft.lines.length || draft.title)
  if (!normalized.length) {
    return [
      {
        id: createId('section'),
        title: '未识别内容',
        detectedKind: 'unclassified',
        lines: []
      }
    ]
  }

  return normalized
}
