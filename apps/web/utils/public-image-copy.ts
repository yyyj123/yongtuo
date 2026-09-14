// Remove legacy image captions from public copy without changing stored records.
const imageCopy = [
  ['配图为 AI 通用示意，不代表该条目的实际外观；', ''],
  ['配图为 AI 生成的通用示意图，不代表本条目的实际外观。', ''],
  ['配图为 AI 生成的通用示意图。', ''],
  ['展示示例 · AI 配图。', '展示示例。'],
  ['AI 生成示意图 · 非实拍', ''],
  ['AI illustration · not an actual photograph', ''],
] as const

export function cleanPublicImageCopy<T>(value: T): T {
  if (typeof value === 'string') {
    return imageCopy.reduce<string>((text, [from, to]) => text.split(from).join(to), value) as T
  }
  if (Array.isArray(value)) return value.map(cleanPublicImageCopy) as T
  if (value !== null && typeof value === 'object') {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, cleanPublicImageCopy(item)])) as T
  }
  return value
}
