export interface Field {key:string;label:string;type?:string;required?:boolean;max?:number;min?:number;pattern?:string;hint?:string;options?:{value:any;label:string}[]}
export const activeOptions=[{value:'ACTIVE',label:'启用'},{value:'INACTIVE',label:'停用'}]
export const publicationOptions=[{value:'DRAFT',label:'草稿'},{value:'PUBLISHED',label:'发布'},{value:'OFFLINE',label:'下架'}]
