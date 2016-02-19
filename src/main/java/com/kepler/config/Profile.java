package com.kepler.config;

import java.util.HashMap;
import java.util.Map;

import com.kepler.org.apache.commons.lang.StringUtils;
import com.kepler.service.Service;

/**
 * 偏好
 * 
 * @author kim 2015年11月16日
 */
public class Profile {

	/**
	 * 是否开启偏好
	 */
	private final static boolean ENABLED = PropertiesUtils.get(Profile.class.getName().toLowerCase() + ".enabled", false);

	private final Map<Service, String> profiles = new HashMap<Service, String>();

	public Profile add(Service service, String profile) {
		this.profiles.put(service, StringUtils.defaultString(profile, Service.DEF_PROFILE));
		return this;
	}

	public String profile(Service service) {
		return Profile.ENABLED ? this.profiles.get(service) : null;
	}
}
