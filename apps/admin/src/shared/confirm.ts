import { ElMessageBox } from 'element-plus'
export async function confirmAction(message: string, title='确认操作', confirmButtonText='确认') {
 try { await ElMessageBox.confirm(message,title,{confirmButtonText,cancelButtonText:'取消',type:'warning',distinguishCancelAndClose:true,closeOnClickModal:false}); return true }
 catch { return false }
}
