package com.kepler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 是否自动发布
 * 
 * @author kim
 *
 * 2016年2月18日
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Autowired {

	/**
	 * 实际发布版本, 覆盖@Service
	 * 
	 * @return
	 */
	String version() default "";

	/**
	 * Profile逻辑名
	 * 
	 * @return
	 */
	String profile() default "";

	/**
	 * 版本兼容性
	 * 
	 * @return
	 */
	String compatible() default "";
}
