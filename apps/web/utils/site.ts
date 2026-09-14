export const navigation=[['/','首页','Home'],['/products','产品中心','Products'],['/cnc-machining','CNC 定制加工','CNC machining'],['/capabilities','生产能力','Capabilities'],['/cases','应用案例','Applications'],['/articles','新闻资讯','News'],['/about','关于我们','About'],['/contact','联系我们','Contact']]
export const localPath=(path:string,locale:string)=>locale==='en'?'/en'+(path==='/'?'/':path):path
export const safeUrl=(url:unknown)=>typeof url==='string'&&(/^(https?:\/\/|mailto:|tel:)/i.test(url)||/^\/(?!\/)/.test(url))?url:undefined
