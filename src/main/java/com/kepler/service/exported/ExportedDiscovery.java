package com.kepler.service.exported;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import com.kepler.KeplerLocalException;
import com.kepler.annotation.Autowired;
import com.kepler.annotation.Service;
import com.kepler.config.Profile;
import com.kepler.service.Exported;

/**
 * @Service scan
 * 
 * @author kim 2015年8月19日
 */
public class ExportedDiscovery implements BeanPostProcessor {

	private final Exported exported;

	private final Profile profile;

	public ExportedDiscovery(Profile profile, Exported exported) {
		super();
		this.exported = exported;
		this.profile = profile;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		Autowired autowired = AnnotationUtils.findAnnotation(Advised.class.isAssignableFrom(bean.getClass()) ? Advised.class.cast(bean).getTargetClass() : bean.getClass(), Autowired.class);
		// 标记@Autowired表示自动发布
		if (autowired != null) {
			this.exported(bean, autowired.version(), autowired.profile());
		}
		return bean;
	}

	// 如果@Autowire定义了Version则覆盖@Service
	private void exported(Object bean, String version, String profile) {
		// 迭代所有定义@Service的接口
		for (Class<?> each : this.services(new ArrayList<Class<?>>(), bean.getClass())) {
			try {
				Service exported = AnnotationUtils.findAnnotation(each, Service.class);
				com.kepler.service.Service service = new com.kepler.service.Service(each, StringUtils.hasText(version) ? version : exported.version(), exported.catalog());
				// 追加Profile(默认为Class全名小写)
				this.profile.add(service, (profile == null || profile.trim().isEmpty()) ? service.service().getName().toLowerCase() : profile);
				this.exported.exported(service, bean);
			} catch (Exception e) {
				throw new KeplerLocalException(e);
			}
		}
	}

	private Collection<Class<?>> services(Collection<Class<?>> exported, Class<?> clazz) {
		this.recursive(exported, clazz);
		this.interfaces(exported, clazz);
		return exported;
	}

	/**
	 * 向上查找父类
	 * 
	 * @param exported
	 * @param clazz
	 */
	private void recursive(Collection<Class<?>> exported, Class<?> clazz) {
		if (clazz.getSuperclass() != null) {
			this.services(exported, clazz.getSuperclass());
		}
	}

	/**
	 * 获取所有标记@Service的接口
	 * 
	 * @param exported
	 * @param clazz
	 */
	private void interfaces(Collection<Class<?>> exported, Class<?> clazz) {
		for (Class<?> each : clazz.getInterfaces()) {
			if (AnnotationUtils.findAnnotation(each, Service.class) != null) {
				exported.add(each);
			}
		}
	}
}
