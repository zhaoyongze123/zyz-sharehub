import { defineConfig, presetUno } from 'unocss'
import presetIcons from '@unocss/preset-icons'
import presetTypography from '@unocss/preset-typography'

export default defineConfig({
  presets: [
    presetUno(),
    presetIcons(),
    presetTypography()
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
