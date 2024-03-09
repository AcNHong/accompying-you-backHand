package com.jun.usercenter.excption;

import com.jun.usercenter.common.ErrorCodeEnum;
import com.jun.usercenter.model.domain.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常捕获
 */
@Slf4j
@RestControllerAdvice
public class GlobalException {

    /**
     * BusinessException
     * @return
     */
    @ExceptionHandler(BusinessException.class)
    public Result businessExceptionHandler(BusinessException e){
      log.info("businessException:{}.{}",e.getMessage(),e);
      return Result.error(e.getCode(),e.getMessage(),e.getDescription());
    }

    /**
     * RuntimeException捕获
     * @param e
     * @return Result
     */
    @ExceptionHandler(RuntimeException.class)
    public Result runtimeExceptionHandler(RuntimeException e){
        log.info("runtimeException:{}.{}",e.getMessage(),e);
        return Result.error(ErrorCodeEnum.SYSTEM_ERROR);
    }

}
