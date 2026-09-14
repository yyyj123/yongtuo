import {api} from '../auth/client'
export async function uploadMedia(client:Pick<typeof api,'post'>,file:File){const data=new FormData();data.append('file',file);return client.post('/media',data)}
export function moveImage<T>(rows:T[],index:number,direction:number){const next=index+direction;if(next<0||next>=rows.length)return;const [row]=rows.splice(index,1);rows.splice(next,0,row)}
export function attachmentPayload(row:any,index:number){return {mediaId:row.mediaId,displayNameZh:row.displayNameZh||'',displayNameEn:row.displayNameEn||'',isPublic:Boolean(row.isPublic),allowDownload:Boolean(row.allowDownload),sortOrder:index}}
