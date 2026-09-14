export function homeMedia(published:unknown,slot:'hero'|'machining'|'workshop'|'products'){
 const valid=typeof published==='string'&&(/^https?:\/\//i.test(published)||/^\/(?!\/)/.test(published))&&!published.includes('\\')
 return {src:valid?published as string:`/images/ai-preview/${slot}.png`,illustrative:!valid}
}
