package com.kepler.host.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.kepler.KeplerLocalException;
import com.kepler.KeplerRoutingException;
import com.kepler.config.Profile;
import com.kepler.config.PropertiesUtils;
import com.kepler.host.Host;
import com.kepler.host.HostLocks;
import com.kepler.host.HostState;
import com.kepler.host.Hosts;
import com.kepler.protocol.Request;
import com.kepler.router.Routing;
import com.kepler.router.filter.HostFilter;
import com.kepler.router.routing.Routings;
import com.kepler.service.Service;

/**
 * @author kim 2015年12月30日
 */
public class DefaultHosts implements Hosts {

	private final static String ROUTING_KEY = DefaultHosts.class.getName().toLowerCase() + ".routing";

	/**
	 * 默认路由策略
	 */
	private final static String ROUTING_DEF = PropertiesUtils.get(DefaultHosts.ROUTING_KEY, Routing.NAME);

	private final static List<Host> EMPTY = Collections.unmodifiableList(new ArrayList<Host>());

	private final static Log LOGGER = LogFactory.getLog(DefaultHosts.class);

	/**
	 * Address维度映射
	 */
	private final Map<String, Host> address = new HashMap<String, Host>();

	/**
	 * 所有主机(所有状态)
	 */
	private final List<Host> hosts = new ArrayList<Host>();

	private final Set<Host> waiting = new HashSet<Host>();

	private final Set<Host> bans = new HashSet<Host>();

	private final HostLocks locks = new SegmentLocks();

	private final Tags tags = new Tags();

	private final Service service;

	private final Profile profile;

	private final Routings routings;

	private final HostFilter filter;

	public DefaultHosts(Service service, HostFilter filter, Profile profile, Routings routings) {
		super();
		this.filter = filter;
		this.service = service;
		this.profile = profile;
		this.routings = routings;
	}

	private String detail(Host host, String action) {
		return new StringBuffer().append("Host: ").append(host).append(") ").append(action).append(" ... (").append(this.service).append(") ").toString();
	}

	/**
	 * (任意状态)是否含指定Host, 调用者加锁
	 * 
	 * @param host
	 * @return
	 */
	private boolean contain(Host host) {
		return this.hosts.contains(host) || this.bans.contains(host) || this.waiting.contains(host);
	}

	public void remove(Host host) {
		synchronized (this.locks.get(host)) {
			// 从Host&&Tag&Address删除(运行时Host)或从Ban||Waiting删除(待连接Host)
			if ((this.hosts.remove(host) && this.tags.remove(host) && (this.address.remove(host.address()) != null)) || (this.bans.remove(host) || this.waiting.remove(host))) {
				DefaultHosts.LOGGER.warn(this.detail(host, "removed"));
			}
		}
	}

	public void wait(Host host) {
		synchronized (this.locks.get(host)) {
			// 不在任意列表
			if (!this.contain(host)) {
				this.waiting.add(host);
				DefaultHosts.LOGGER.warn(this.detail(host, "waiting"));
			}
		}
	}

	public void active(Host host) {
		synchronized (this.locks.get(host)) {
			// 从Ban&&Waiting(待连接Host)移除并加入到Tags&&Hosts&&Address(运行时Host)
			if (this.bans.remove(host) || this.waiting.remove(host)) {
				this.tags.put(host);
				this.hosts.add(host);
				this.address.put(host.address(), host);
				DefaultHosts.LOGGER.warn(this.detail(host, "active"));
			}
		}
	}

	public void replace(Host current, Host newone) {
		synchronized (this.locks.get(current)) {
			this.remove(current);
			this.tags.put(newone);
			this.hosts.add(newone);
			this.address.put(newone.address(), newone);
			DefaultHosts.LOGGER.warn(this.detail(newone, "replace"));
		}
	}

	public boolean ban(Host host) {
		synchronized (this.locks.get(host)) {
			// 从Hosts&&Tags&Address移除或从Waiting(运行时Host)移除
			if ((this.hosts.remove(host) && this.tags.remove(host) && (this.address.remove(host.address()) != null)) || this.waiting.remove(host)) {
				this.bans.add(host);
				DefaultHosts.LOGGER.warn(this.detail(host, "baned"));
				return true;
			}
			// 处理已在Ban队列中的Ban操作(如重连失败)
			return this.bans.contains(host);
		}
	}

	public Host host(Request request) {
		// PropertiesUtils.profile(DefaultHosts.this.profiles.profile(this.service), DefaultHosts.ROUTING_KEY, DefaultHosts.ROUTING_DEF), 当前Service对应负载策略(可变)
		return this.routings.get(PropertiesUtils.profile(DefaultHosts.this.profile.profile(this.service), DefaultHosts.ROUTING_KEY, DefaultHosts.ROUTING_DEF)).route(request, this.hosts(request));
	}

	// 只读(协商)
	public List<Host> hosts(Request request) {
		// Request.header(Host.TAG_KEY, Host.TAG_DEF)), 获取Tag, 如果不存在则使用默认""
		// 获取Tag对应Host集合, 如果不存在则使用Main集合
		List<Host> hosts = this.tags(request.get(Host.TAG_KEY, Host.TAG_DEF));
		return this.valid(request, this.filter.filter(request, hosts.isEmpty() ? this.main() : hosts));
	}

	@Override
	public Host select(String address) {
		return this.address.get(address);
	}

	// 只读(协商)
	public Collection<Host> select(HostState state) {
		switch (state) {
		case WAITING:
			return this.waiting;
		case ACTIVE:
			return this.hosts;
		case BAN:
			return this.bans;
		default:
			throw new KeplerLocalException("Unvalid state for " + state);
		}
	}

	private List<Host> main() {
		return this.tags.get(Host.TAG_DEF);
	}

	private List<Host> tags(String tag) {
		return this.tags.get(tag);
	}

	private List<Host> valid(Request request, List<Host> hosts) {
		if (hosts.isEmpty()) {
			throw new KeplerRoutingException("None service for " + request.service());
		}
		return hosts;
	}

	private class Tags {

		private final Map<String, List<Host>> tags = new HashMap<String, List<Host>>();

		public List<Host> get(String tag) {
			List<Host> hosts = this.tags.get(tag);
			return hosts != null ? hosts : DefaultHosts.EMPTY;
		}

		/**
		 * 由外部调用进行同步安全
		 * 
		 * @param host
		 * @return
		 */
		public Tags put(Host host) {
			List<Host> hosts = this.get(host.tag());
			// 不存在则创建
			(hosts = hosts != DefaultHosts.EMPTY ? hosts : new ArrayList<Host>()).add(host);
			this.tags.put(host.tag(), hosts);
			return this;
		}

		/**
		 * 由外部调用进行同步安全
		 * 
		 * @param host
		 * @return
		 */
		public boolean remove(Host host) {
			List<Host> hosts = this.get(host.tag());
			// InvokerHandler.channelInactive回调此方法. 禁止对DefaultHosts.EMPTY调用Remove
			return hosts != DefaultHosts.EMPTY ? hosts.remove(host) : false;
		}
	}
}
