import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const relativeLuminance = (hex: string) => {
  const channels = hex.match(/[a-f\d]{2}/gi)?.map((value) => Number.parseInt(value, 16) / 255) ?? []
  const linear = channels.map((value) => value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4)
  return 0.2126 * linear[0] + 0.7152 * linear[1] + 0.0722 * linear[2]
}

const contrastRatio = (foreground: string, background: string) => {
  const light = Math.max(relativeLuminance(foreground), relativeLuminance(background))
  const dark = Math.min(relativeLuminance(foreground), relativeLuminance(background))
  return (light + 0.05) / (dark + 0.05)
}

describe('admin theme accessibility', () => {
  it('uses a primary button color with at least 4.5:1 white-text contrast', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/styles/theme.css'), 'utf8')
    const primary = css.match(/--el-color-primary:\s*(#[a-f\d]{6})/i)?.[1]

    expect(primary).toBeDefined()
    expect(contrastRatio(primary!, '#ffffff')).toBeGreaterThanOrEqual(4.5)
  })

  it('defines an explicit keyboard focus-visible indicator', () => {
    const css = readFileSync(resolve(process.cwd(), 'src/styles/theme.css'), 'utf8')

    expect(css).toMatch(/\.login-submit:focus-visible\s*\{[^}]*outline:/s)
  })
})
