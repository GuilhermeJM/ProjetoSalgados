package br.com.salgados.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException(String code, String message) {
        super(code, message);
    }
}