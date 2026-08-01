package com.jinfu.common.result;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "Success"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    INTERNAL_ERROR(500, "Internal Server Error"),

    // Business errors
    USER_NOT_FOUND(1001, "User Not Found"),
    BAD_CREDENTIALS(1002, "Bad Credentials"),
    USER_DISABLED(1003, "User Disabled"),
    TOKEN_EXPIRED(1004, "Token Expired"),
    TOKEN_INVALID(1005, "Token Invalid"),
    DUPLICATE_KEY(1006, "Duplicate Key"),
    DATA_NOT_EXIST(1007, "Data Not Exist"),
    FILE_UPLOAD_FAILED(1008, "File Upload Failed"),
    PARAM_INVALID(1009, "Parameter Invalid"),

    // Flowable errors
    PROCESS_DEF_NOT_FOUND(2001, "Process Definition Not Found"),
    PROCESS_INST_NOT_FOUND(2002, "Process Instance Not Found"),
    TASK_NOT_FOUND(2003, "Task Not Found"),
    TASK_NOT_ASSIGNED(2004, "Task Not Assigned To You"),
    ILLEGAL_OPERATION(2005, "Illegal Operation"),
    DEPLOY_FAILED(2006, "Deploy Failed"),

    // Form errors
    FORM_DEF_NOT_FOUND(3001, "Form Definition Not Found"),
    FORM_SCHEMA_INVALID(3002, "Form Schema Invalid");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
