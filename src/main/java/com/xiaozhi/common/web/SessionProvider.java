package com.xiaozhi.common.web;

import java.io.Serializable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Session提供者
 */
public interface SessionProvider {
	public Serializable getAttribute(HttpServletRequest request, String name);

	public void setAttribute(HttpServletRequest request, HttpServletResponse response, String name, Serializable value);

	public String getSessionId(HttpServletRequest request, HttpServletResponse response);

	public void logout(HttpServletRequest request, HttpServletResponse response);

	public void removeAttribute(HttpServletRequest request, String name);
}
