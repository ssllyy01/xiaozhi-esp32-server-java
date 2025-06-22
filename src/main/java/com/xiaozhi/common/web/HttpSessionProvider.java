package com.xiaozhi.common.web;

import java.io.Serializable;



import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * HttpSession提供类
 */
@Service
public class HttpSessionProvider implements SessionProvider {

	public Serializable getAttribute(HttpServletRequest request, String name) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			return (Serializable) session.getAttribute(name);
		} else {
			return null;
		}
	}

	public void setAttribute(HttpServletRequest request, HttpServletResponse response, String name,
			Serializable value) {
		System.out.println(name);
		HttpSession session = request.getSession();
		session.setAttribute(name, value);
	}

	public String getSessionId(HttpServletRequest request, HttpServletResponse response) {
		return request.getSession().getId();
	}

	public void logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
	}

    @Override
	public void removeAttribute(HttpServletRequest request, String name) {
		HttpSession session = request.getSession();
		session.removeAttribute(name);
	}

}
