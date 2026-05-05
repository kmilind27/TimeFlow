package com.company.authservice.exception;

public class InvalidRoleChangeException extends RuntimeException {
	
	public InvalidRoleChangeException(String message) {
		super(message);
	}
}
