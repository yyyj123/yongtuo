type Query = Record<string, any>
const allowed = (key: string) => ['category', 'keyword', 'page', 'sort'].includes(key) || /^attr\.[a-zA-Z0-9_]+$/.test(key)
export function filterQuery(query: Query) {
  const params = new URLSearchParams()
  for (const [key, values] of Object.entries(query)) if (allowed(key)) {
    for (const value of [values].flat()) if (value != null && String(value).trim()) params.append(key, String(value))
  }
  params.set('pageSize', '24')
  return params.toString()
}
export function updateFilter(query: Query, key: string, value: any): Query {
  const next = { ...query }
  delete next.page
  if (value == null || value === '' || (Array.isArray(value) && !value.length)) delete next[key]
  else next[key] = value
  return next
}
export function filterChips(query: Query) {
  return Object.entries(query).filter(([key]) => key.startsWith('attr.')).flatMap(([key, value]) =>
    [value].flat().filter(Boolean).map(value => ({ key, value: String(value) })))
}
