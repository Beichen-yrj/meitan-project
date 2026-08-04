package com.meitan.exception;

/** Python 科学计算服务无法访问或返回异常。 */
public class PythonServiceUnavailableException extends RuntimeException {
    public PythonServiceUnavailableException(String message) {
        super(message);
    }

    public PythonServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
