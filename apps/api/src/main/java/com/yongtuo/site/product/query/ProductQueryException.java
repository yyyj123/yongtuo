package com.yongtuo.site.product.query;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class ProductQueryException extends BusinessException {
    private ProductQueryException(int code, String message, HttpStatus status) {
        super(code, message, status);
    }

    public static ProductQueryException invalidFilter() {
        return new ProductQueryException(22001, "Invalid product filter", HttpStatus.BAD_REQUEST);
    }

    public static ProductQueryException notFound() {
        return new ProductQueryException(22002, "Product not found", HttpStatus.NOT_FOUND);
    }
}
