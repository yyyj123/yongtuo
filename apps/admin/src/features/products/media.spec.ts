import {describe,it,expect,vi} from 'vitest'
import {uploadMedia,moveImage,attachmentPayload} from './media'
describe('product media controls',()=>{
 it('uploads a file before attaching it and keeps explicit gallery order',async()=>{
  const post=vi.fn().mockResolvedValue({id:7,publicUrl:'https://example.com/test.jpg'})
  const result=await uploadMedia({post} as any,new File(['test'],'synthetic.jpg',{type:'image/jpeg'}))
  expect(post.mock.calls[0][1]).toBeInstanceOf(FormData);expect(result.id).toBe(7)
  const rows=[{mediaId:1},{mediaId:2}];moveImage(rows,1,-1);expect(rows.map(r=>r.mediaId)).toEqual([2,1])
 })
 it('does not make an attachment downloadable merely because it is public',()=>{
  expect(attachmentPayload({mediaId:1,isPublic:true,allowDownload:false},0)).toMatchObject({isPublic:true,allowDownload:false})
 })
})
