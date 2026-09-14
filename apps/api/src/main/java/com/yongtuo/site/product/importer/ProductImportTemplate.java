package com.yongtuo.site.product.importer;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
final class ProductImportTemplate {
 private ProductImportTemplate(){}
 static byte[] create(){try(var workbook=new XSSFWorkbook();var output=new ByteArrayOutputStream()){
  var sheet=workbook.createSheet("Products");var header=sheet.createRow(0);var style=workbook.createCellStyle();var font=workbook.createFont();font.setBold(true);style.setFont(font);
  String[] columns={"productCode","slug","categorySlug","nameZh","status"};
  for(int i=0;i<columns.length;i++){var cell=header.createCell(i);cell.setCellValue(columns[i]);cell.setCellStyle(style);sheet.setColumnWidth(i,28*256);}sheet.createFreezePane(0,1);
  var notes=workbook.createSheet("填写说明");String[] lines={"请保持 Products 页表头不变，从第二行开始填写。","productCode：唯一产品编号；slug：小写字母、数字、连字符。","categorySlug：使用后台分类管理显示的网址标识，不是分类名称。","nameZh：中文产品名称；status：DRAFT / PUBLISHED / OFFLINE。","首次整理资料建议使用 DRAFT。不得填写未经确认的产品事实。","上传仅预检；确认导入后才写入产品。最多1000行、5MB。"};
  for(int i=0;i<lines.length;i++)notes.createRow(i).createCell(0).setCellValue(lines[i]);notes.setColumnWidth(0,100*256);workbook.write(output);return output.toByteArray();
 }catch(java.io.IOException e){throw new IllegalStateException("Template generation failed");}}
}
