package com.yongtuo.site.product.importer;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class ProductImportException extends BusinessException {
    private ProductImportException(int code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }

    static ProductImportException invalidFile() {
        return new ProductImportException(23001, "A valid .xlsx product workbook is required");
    }

    static ProductImportException fileTooLarge() {
        return new ProductImportException(23002, "Product workbook exceeds the 5 MB limit");
    }

    static ProductImportException invalidTemplate() {
        return new ProductImportException(23003, "Product workbook headers are invalid");
    }

    static ProductImportException tooManyRows() {
        return new ProductImportException(23004, "Product workbook exceeds the 1000 row limit");
    }

    static ProductImportException invalidToken() {
        return new ProductImportException(23005, "Product import preview token is invalid or expired");
    }

    static ProductImportException notConfirmable() {
        return new ProductImportException(23006, "Product import preview contains validation errors");
    }

    static ProductImportException changedSincePreview() {
        return new ProductImportException(23007, "Product data changed after import preview");
    }
}
