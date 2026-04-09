import * as assert from 'node:assert/strict'

import { buildResumeDocumentFromText } from '../src/features/resume-parser/to-document'

function getBasicSection(sourceText: string) {
  const result = buildResumeDocumentFromText(sourceText, 'modern', 'parser-regression.pdf')
  const basic = result.document.sections.find((section) => section.kind === 'basic')
  assert.ok(basic, '应识别出基本信息模块')
  return basic
}

function getFieldValue(sourceText: string, key: string) {
  const basic = getBasicSection(sourceText)
  return basic.fields.find((field) => field.key === key)?.value ?? ''
}

{
  const sourceText = ['张三', '求职意向: Java后端开发工程师 上海 一周内到岗', '13800138000', 'zhangsan@example.com'].join('\n')

  const result = buildResumeDocumentFromText(sourceText, 'modern', 'intent-composite.pdf')
  const basic = result.document.sections[0]

  assert.equal(basic.kind, 'basic', '求职意向行不应被误切成新模块')
  assert.equal(getFieldValue(sourceText, 'intent'), 'Java后端开发工程师')
  assert.equal(getFieldValue(sourceText, 'city'), '上海')
  assert.equal(getFieldValue(sourceText, 'arrival'), '一周内到岗')
}

{
  const sourceText = [
    '李四',
    '求职意向: 前端开发工程师 杭州 随时到岗',
    '1998年 女',
    '13900139000 | lisi@example.com',
    '杭州电子科技大学 计算机科学与技术 本科 2017.09-2021.06'
  ].join('\n')

  const result = buildResumeDocumentFromText(sourceText, 'classic', 'basic-order.pdf')
  const basic = result.document.sections[0]

  assert.equal(basic.kind, 'basic', '首段应保持为基本信息')
  assert.equal(result.document.sections.length, 1, '无标题基础信息不应被误切成额外模块')
  assert.equal(getFieldValue(sourceText, 'intent'), '前端开发工程师')
  assert.equal(getFieldValue(sourceText, 'city'), '杭州')
  assert.equal(getFieldValue(sourceText, 'arrival'), '随时到岗')
  assert.equal(getFieldValue(sourceText, 'gender'), '女')
  assert.equal(getFieldValue(sourceText, 'phone'), '13900139000')
  assert.equal(getFieldValue(sourceText, 'email'), 'lisi@example.com')
  assert.equal(getFieldValue(sourceText, 'school'), '杭州电子科技大学')
  assert.equal(getFieldValue(sourceText, 'major'), '计算机科学与技术')
  assert.equal(getFieldValue(sourceText, 'education'), '本科')
  assert.equal(getFieldValue(sourceText, 'educationTime'), '2017.09-2021.06')
}

console.log('resume-parser regression checks passed')
