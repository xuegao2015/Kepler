package com.kepler.host.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kepler.KeplerLocalException;
import com.kepler.config.PropertiesUtils;
import com.kepler.host.Host;
import com.kepler.main.Pid;

/**
 * @author zhangjiehao 2015年7月8日
 */
public class ServerHost implements Serializable, Host {

	private final static long serialVersionUID = 1L;

	private final static Log LOGGER = LogFactory.getLog(ServerHost.class);

	private final static int PORT = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".port", 9876);

	/**
	 * 本地端口嗅探范围
	 */
	private final static int RANGE = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".range", 1000);

	/**
	 * 本地端口嗅探间隔
	 */
	private final static int INTERVAL = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".interval", 500);

	/**
	 * 是否使用固定端口
	 */
	private final static boolean STABLE = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".stable", false);

	/**
	 * 网卡名称模式
	 */
	private final static String PATTERN = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".pattern", ".*");

	/**
	 * 服务唯一ID
	 */
	private final static String SID = PropertiesUtils.get(ServerHost.class.getName().toLowerCase() + ".sid", UUID.randomUUID().toString());

	private final String sid;

	private final Host local;

	private ServerHost(Host host, String sid) {
		this.local = host;
		this.sid = sid;
	}

	public ServerHost(Pid pid) throws Exception {
		this.local = new DefaultHost(Host.GROUP, Host.TOKEN_VAL, Host.TAG_VAL, pid.pid(), this.ip(), ServerHost.STABLE ? ServerHost.PORT : this.available(), Host.PRIORITY_DEF);
		this.sid = ServerHost.SID;
	}

	private String ip() throws Exception {
		Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
		while (interfaces.hasMoreElements()) {
			NetworkInterface intr = interfaces.nextElement();
			String intrName = intr.getName();
			// 网卡名称是否符合
			if (Pattern.matches(ServerHost.PATTERN, intrName)) {
				Enumeration<InetAddress> addresses = intr.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress address = addresses.nextElement();
					if (address.isSiteLocalAddress() && !address.isLoopbackAddress()) {
						return address.getHostAddress();
					}
				}
			}
			continue;
		}
		ServerHost.LOGGER.warn("Using localhost mode for current service ... ");
		return Host.LOOP;
	}

	/**
	 * 扫描端口
	 * 
	 * @return
	 * @throws Exception
	 */
	private int available() throws Exception {
		for (int index = ServerHost.PORT; index < ServerHost.PORT + ServerHost.RANGE; index++) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(InetAddress.getByName(Host.LOOP), index), ServerHost.INTERVAL);
			} catch (IOException e) {
				ServerHost.LOGGER.debug("Port " + index + " used ... ");
				return index;
			}
		}
		throw new KeplerLocalException("Cannot allocate port for current service");
	}

	public String sid() {
		return this.sid;
	}

	@Override
	public int port() {
		return this.local.port();
	}

	@Override
	public String pid() {
		return this.local.pid();
	}

	@Override
	public String tag() {
		return this.local.tag();
	}

	@Override
	public String host() {
		return this.local.host();
	}

	@Override
	public String token() {
		return this.local.token();
	}

	@Override
	public String group() {
		return this.local.group();
	}

	public String address() {
		return this.local.address();
	}

	@Override
	public int priority() {
		return this.local.priority();
	}

	@Override
	public boolean loop(Host host) {
		return this.local.loop(host);
	}

	@Override
	public boolean loop(String host) {
		return this.local.loop(host);
	}

	public int hashCode() {
		return this.local.hashCode();
	}

	public boolean equals(Object ob) {
		// Not null point security
		return this.local.equals(ob);
	}

	public String toString() {
		return this.local.toString();
	}

	public static class Builder {

		private String tag;

		private String sid;

		private String pid;

		private String host;

		private String token;

		private String group;

		private int port;

		private int priority;

		public Builder(ServerHost that) {
			this.setGroup(that.group()).setToken(that.token()).setHost(that.host()).setPid(that.pid()).setPort(that.port()).setPriority(that.priority()).setSid(that.sid()).setTag(that.tag());
		}

		public Builder setPriority(int priority) {
			this.priority = priority;
			return this;
		}

		public Builder setGroup(String group) {
			this.group = group;
			return this;
		}

		public Builder setToken(String token) {
			this.token = token;
			return this;
		}

		public Builder setHost(String host) {
			this.host = host;
			return this;
		}

		public Builder setTag(String tag) {
			this.tag = tag;
			return this;
		}

		public Builder setPid(String pid) {
			this.pid = pid;
			return this;
		}

		public Builder setSid(String sid) {
			this.sid = sid;
			return this;
		}

		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		public ServerHost toServerHost() {
			return new ServerHost(new DefaultHost(this.group, this.token, this.tag, this.pid, this.host, this.port, this.priority), this.sid);
		}
	}
}
