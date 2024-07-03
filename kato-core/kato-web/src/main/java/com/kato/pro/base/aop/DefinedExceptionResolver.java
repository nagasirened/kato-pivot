package com.kato.pro.base.aop;

import com.kato.pro.base.entity.CommonCode;
import com.kato.pro.base.entity.Result;
import com.kato.pro.base.exception.KatoServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestControllerAdvice
public class DefinedExceptionResolver {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String msg = fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.joining(","));
        return Result.build(CommonCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(KatoServiceException.class)
    public Result<Object> handleException(KatoServiceException e) {
        print(e);
        return Result.build(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        print(e);
        return Result.build(CommonCode.SYSTEM_ERROR.getCode(), e.getMessage());
    }

    public void print(Exception e) {
        log.error("ExceptionResolver, type: {}, info: {}", e.getClass().getSimpleName(), e.getMessage(), e);
    }
}
