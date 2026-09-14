package com.yongtuo.site.product;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class ProductBusinessException extends BusinessException {
    private ProductBusinessException(int code, String message, HttpStatus status) {
        super(code, message, status);
    }

    static ProductBusinessException notFound() {
        return new ProductBusinessException(20001, "Product not found", HttpStatus.NOT_FOUND);
    }

    static ProductBusinessException duplicateCode() {
        return new ProductBusinessException(20002, "Product code already exists", HttpStatus.CONFLICT);
    }

    static ProductBusinessException duplicateSlug() {
        return new ProductBusinessException(20003, "Product slug already exists", HttpStatus.CONFLICT);
    }

    static ProductBusinessException invalidCategory() {
        return new ProductBusinessException(20004, "Product category is invalid", HttpStatus.BAD_REQUEST);
    }

    static ProductBusinessException constraintViolation() {
        return new ProductBusinessException(20005, "Product constraint violation", HttpStatus.CONFLICT);
    }

    static ProductBusinessException compositionViolation() {
        return new ProductBusinessException(20005, "Product attributes or variants are invalid", HttpStatus.BAD_REQUEST);
    }
}
