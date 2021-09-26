package com.ariescat.seckill.exception;

import com.ariescat.seckill.result.CodeMsg;

/**
 * 自定义全局异常类
 */
public class GlobalException extends RuntimeException {

    private final CodeMsg codeMsg;

    /**
     * 接收CodeMsg
     */
    public GlobalException(CodeMsg codeMsg) {
        super(codeMsg.toString());
        this.codeMsg = codeMsg;
    }

    public CodeMsg getCodeMsg() {
        return codeMsg;
    }
}
