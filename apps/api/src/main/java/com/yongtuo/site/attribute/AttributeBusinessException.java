package com.yongtuo.site.attribute;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

public final class AttributeBusinessException extends BusinessException {
    private AttributeBusinessException(int code, String message, HttpStatus status) {
        super(code, message, status);
    }

    static AttributeBusinessException notFound() { return new AttributeBusinessException(22001, "Attribute not found", HttpStatus.NOT_FOUND); }
    static AttributeBusinessException duplicateCode() { return new AttributeBusinessException(22002, "Attribute code already exists", HttpStatus.CONFLICT); }
    static AttributeBusinessException duplicateOption() { return new AttributeBusinessException(22003, "Attribute option value already exists", HttpStatus.CONFLICT); }
    static AttributeBusinessException duplicateBinding() { return new AttributeBusinessException(22004, "Category attribute binding already exists", HttpStatus.CONFLICT); }
    static AttributeBusinessException optionsNotAllowed() { return new AttributeBusinessException(22005, "Options are not allowed for this attribute type", HttpStatus.BAD_REQUEST); }
    static AttributeBusinessException invalidReference() { return new AttributeBusinessException(22006, "Category or attribute is inactive", HttpStatus.BAD_REQUEST); }
    static AttributeBusinessException inUse() { return new AttributeBusinessException(22007, "Attribute is in use and cannot be deleted", HttpStatus.CONFLICT); }
    static AttributeBusinessException typeChangeNotAllowed() { return new AttributeBusinessException(22008, "Attribute type cannot change while options exist", HttpStatus.CONFLICT); }
    static AttributeBusinessException invalidOption() { return new AttributeBusinessException(22009, "Attribute option is invalid", HttpStatus.BAD_REQUEST); }
}
