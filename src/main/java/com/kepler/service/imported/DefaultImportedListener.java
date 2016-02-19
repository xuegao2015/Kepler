package com.kepler.service.imported;

import com.kepler.config.PropertiesUtils;
import com.kepler.connection.Connect;
import com.kepler.host.HostsContext;
import com.kepler.service.ImportedListener;
import com.kepler.service.Service;
import com.kepler.service.ServiceInstance;

/**
 * @author 张皆浩 2015年9月11日
 */
public class DefaultImportedListener implements ImportedListener {

	private final static boolean REPAIR = PropertiesUtils.get(DefaultImportedListener.class.getName().toLowerCase() + ".repair", true);

	private final HostsContext context;

	private final Connect connect;

	public DefaultImportedListener(HostsContext context, Connect connect) {
		super();
		this.context = context;
		this.connect = connect;
	}

	@Override
	public void add(ServiceInstance instance) throws Exception {
		this.context.getOrCreate(new Service(instance.service(), instance.version(), instance.catalog())).wait(instance.host());
		this.connect.connect(instance.host());
	}

	@Override
	public void delete(ServiceInstance instance) throws Exception {
		this.context.remove(instance.host(), new Service(instance.service(), instance.version(), instance.catalog()));
	}

	@Override
	public void change(ServiceInstance current, ServiceInstance newInstance) throws Exception {
		this.context.getOrCreate(new Service(current.service(), current.version(), current.catalog())).replace(current.host(), newInstance.host());
		this.repair(newInstance);
	}

	private void repair(ServiceInstance newInstance) throws Exception {
		// 故障恢复 (If necessory)
		if (DefaultImportedListener.REPAIR) {
			this.connect.connect(newInstance.host());
		}
	}
}
