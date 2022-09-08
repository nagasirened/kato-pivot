package com.kato.pro.base.exception;

/**
 * Token处理异常
 */
public class TokenException extends RuntimeException {

	private static final long serialVersionUID = -109638013567525177L;

	public TokenException(String message) {
		super(message);
	}
}