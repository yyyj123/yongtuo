import {flattenCategories} from '../products/model'
export function parentOptions(tree:any[],id?:number){const flat=flattenCategories(tree);const blocked=new Set<number>();function block(node:any){blocked.add(node.id);(node.children||[]).forEach(block)}const current=flat.find(c=>c.id===id);if(current)block(current);return flat.filter(c=>!blocked.has(c.id))}
export function bindingPayload(rows:any[]){return rows.filter(r=>r.selected).map((r,index)=>({attributeId:r.attributeId,isRequired:!!r.isRequired,isFilterable:!!r.isFilterable,showInDetail:!!r.showInDetail,sortOrder:r.sortOrder??index}))}
