// Synthetic filter metadata for browser interaction tests; all other reads use the local public API.
import {createServer} from 'node:http'
createServer(async (request,response)=>{
  try {
    if(request.url.startsWith('/api/v1/public/redirects')&&decodeURIComponent(request.url).includes('synthetic-old')){response.setHeader('Content-Type','application/json');response.end(JSON.stringify({code:0,data:{newPath:'/products'}}));return}
    if(request.url.startsWith('/api/v1/public/product-filters')){
      response.setHeader('Content-Type','application/json')
      response.end(JSON.stringify({code:0,data:[{code:'material',name:'测试材质',dataType:'SELECT',options:[{value:'steel',label:'测试钢'}]},{code:'diameter',name:'测试直径',dataType:'NUMBER',options:[]}]}));return
    }
    const result=await fetch('http://127.0.0.1:8084'+request.url,{headers:{'Accept-Language':request.headers['accept-language']||'zh-CN'}})
    response.writeHead(result.status,{'Content-Type':result.headers.get('content-type')||'application/json'});response.end(await result.text())
  }catch{response.writeHead(502);response.end('{}')}
}).listen(3006,'127.0.0.1',()=>console.log('Fixture API: 3006'))
