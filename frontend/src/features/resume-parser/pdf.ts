import { GlobalWorkerOptions, getDocument } from 'pdfjs-dist'
import workerSrc from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { normalizeLines } from './normalize'
import type { RawLine } from './types'

GlobalWorkerOptions.workerSrc = workerSrc

export async function extractPdfLines(file: File) {
  const buffer = await file.arrayBuffer()
  const loadingTask = getDocument({ data: buffer, useWorkerFetch: false })
  const pdf = await loadingTask.promise
  const rawLines: RawLine[] = []

  for (let pageIndex = 1; pageIndex <= pdf.numPages; pageIndex += 1) {
    const page = await pdf.getPage(pageIndex)
    const content = await page.getTextContent()
    let lastY = Number.NaN
    let currentText = ''

    for (const item of content.items as Array<{ str?: string; transform?: number[] }>) {
      const text = item.str?.trim()
      if (!text) {
        continue
      }
      const y = item.transform?.[5] ?? 0
      if (!Number.isNaN(lastY) && Math.abs(lastY - y) > 2 && currentText) {
        rawLines.push({ page: pageIndex, y: lastY, text: currentText })
        currentText = text
      } else {
        currentText = currentText ? `${currentText} ${text}` : text
      }
      lastY = y
    }

    if (currentText) {
      rawLines.push({ page: pageIndex, y: lastY, text: currentText })
    }
  }

  return normalizeLines(rawLines)
}
