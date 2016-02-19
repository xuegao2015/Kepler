package com.kepler.host;

import java.util.Collection;

import com.kepler.protocol.Request;

/**
 * @author kim 2015年7月9日
 */
public interface Hosts {

	public void wait(Host host);

	public void active(Host host);

	public void remove(Host host);

	public void replace(Host current, Host newone);

	public boolean ban(Host host);

	public Host host(Request request);

	public Collection<Host> hosts(Request request);

	/**
	 * 获取指定地址的主机
	 * 
	 * @param address
	 * @return
	 */
	public Host select(String address);

	/**
	 * 获取指定状态的主机
	 * 
	 * @param state
	 * @return
	 */
	public Collection<Host> select(HostState state);
}
