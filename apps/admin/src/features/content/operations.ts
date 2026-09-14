export const resourceLabel=(key:string)=>({products:'产品',categories:'分类',attributes:'参数',articles:'新闻文章','article-categories':'文章分类',cases:'应用案例',certificates:'资质证书',catalogs:'产品目录',home:'首页',contact:'联系方式',site:'网站配置',translation:'英文审核',media:'媒体',auth:'账号'} as Record<string,string>)[key]||'内容'
export function operationLabel(operation:string){
 if(operation.includes('/import/preview'))return '预检 Excel'
 if(operation.includes('/import/confirm'))return '确认导入产品'
 if(operation.includes('/translation/draft'))return '生成英文初稿'
 if(operation.includes('/translation/confirm'))return '确认英文内容'
 if(operation.includes('/duplicate'))return '复制产品'
 if(operation.includes('/primary'))return '切换主目录'
 if(operation.includes('/batch'))return '批量上传图片'
 return operation.startsWith('DELETE')?'删除内容':operation.startsWith('POST')?'新增或执行操作':'更新内容'
}
