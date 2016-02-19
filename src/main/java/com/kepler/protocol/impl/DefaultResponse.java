package com.kepler.protocol.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kepler.org.apache.commons.lang.builder.ToStringBuilder;
import com.kepler.protocol.Response;

/**
 * @author kim
 *
 * 2016年2月14日
 */
public class DefaultResponse implements Response {

	private final static long serialVersionUID = 1L;

	private final byte serial;

	private final Integer ack;

	private final Object response;

	private final Throwable throwable;

	public DefaultResponse(byte serial, Integer ack, Object response) {
		this(serial, ack, response, null);
	}

	public DefaultResponse(byte serial, Integer ack, Throwable throwable) {
		this(serial, ack, null, throwable);
	}

	private DefaultResponse(@JsonProperty("serial") byte serial, @JsonProperty("ack") Integer ack, @JsonProperty("respones") Object response, @JsonProperty("throwable") Throwable throwable) {
		super();
		this.ack = ack;
		this.serial = serial;
		this.response = response;
		this.throwable = throwable;
	}

	public byte serial() {
		return this.serial;
	}

	@Override
	public Integer ack() {
		return this.ack;
	}

	@Override
	public Object response() {
		return this.response;
	}

	public Throwable throwable() {
		return this.throwable;
	}

	public boolean valid() {
		return this.throwable() == null;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}