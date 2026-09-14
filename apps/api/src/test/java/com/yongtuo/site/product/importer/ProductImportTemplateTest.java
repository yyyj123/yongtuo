package com.yongtuo.site.product.importer;
import static org.assertj.core.api.Assertions.assertThat;
import java.io.ByteArrayInputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
class ProductImportTemplateTest {
 @Test void templateUsesExactImportHeadersAndContainsNoInventedProducts() throws Exception {
  try(var workbook=new XSSFWorkbook(new ByteArrayInputStream(ProductImportTemplate.create()))){
   var sheet=workbook.getSheetAt(0);
   assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("productCode");
   assertThat(sheet.getRow(0).getCell(2).getStringCellValue()).isEqualTo("categorySlug");
   assertThat(sheet.getRow(0).getCell(4).getStringCellValue()).isEqualTo("status");
   assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(1);
  }
 }
}
