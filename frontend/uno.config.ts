import { defineConfig, presetUno } from 'unocss'
import presetIcons from '@unocss/preset-icons'

export default defineConfig({
  presets: [
    presetUno(),
    presetIcons()
  ],
  theme: {
    colors: {
      brand: 'var(--color-primary)',
      accent: 'var(--color-accent)',
      surface: 'var(--color-surface-solid)',
      line: 'var(--color-border-soft)'
    }
  }
})
