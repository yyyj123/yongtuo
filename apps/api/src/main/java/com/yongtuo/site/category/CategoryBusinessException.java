package com.yongtuo.site.category;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class CategoryBusinessException extends BusinessException {
    private CategoryBusinessException(int code, String message, HttpStatus status) {
        super(code, message, status);
    }

    static CategoryBusinessException notFound() {
        return new CategoryBusinessException(21001, "Category not found", HttpStatus.NOT_FOUND);
    }

    static CategoryBusinessException notEmpty() {
        return new CategoryBusinessException(21002, "Category is not empty", HttpStatus.CONFLICT);
    }

    static CategoryBusinessException duplicateSlug() {
        return new CategoryBusinessException(21003, "Category slug already exists", HttpStatus.CONFLICT);
    }

    static CategoryBusinessException invalidParent() {
        return new CategoryBusinessException(21004, "Category parent is invalid", HttpStatus.BAD_REQUEST);
    }

    static CategoryBusinessException cycle() {
        return new CategoryBusinessException(21005, "Category hierarchy contains a cycle", HttpStatus.BAD_REQUEST);
    }
}
