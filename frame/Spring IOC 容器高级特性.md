# Spring IOC 容器高级特性



```java
public void refresh() throws BeansException, IllegalStateException{
	synchronized(this.startupShutdownMonitor){
		prepareRefresh();
		ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();
		prepareBeanFactory(beanFactory);
		
		try{
			postProcessBeanFactory(beanFactory);
			invokeBeanFactoryPostProcessors(beanFactory);
			registerBeanPostProcessors(beanFactory);
			initMessageSource();
			initApplicationEventMulticaster();
			onRefresh();
			registerListeners();
			finishBeanFactoryInitialization(beanFactory);
			finishRefresh();
		}catch(BeanException ex){
			destroyBeans();
			cancelRefresh(ex);
			throw ex;
		}
	}
}
```



## 5. Spring IOC容器autowiring实现原理：

Spring IOC容器提供了两种管理Bean依赖关系的方式：

- 显示管理：通过BeanDeinition的属性值和构造方法实现Bean依赖关系管理
- 自动装配：只需配置好autowiring属性，IOC容器会自动使用反射查找属性的类型和名称，然后基于属性的类型或者名称来自动匹配容器中管理的Bean，从而完成自动地完成依赖注入。

#### AbstractAutoWireCapableBeanFactory对Bean实例进行属性依赖注入

应用第一次通过getBean方法（配置了lazy-init预实例化属性的除外）向IOC容器索取Bean时，容器创建Bean的实例对象，并且对Bean实例对象进行属性依赖注入。

- AbstractAutoWireCapableFactory的populateBean方法就是实现Bean属性依赖注入的功能


```java
protected void populateBean(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw){
	PropertyValue pvs = mbd.getPropertyValues();
	...
	if(mbd.getResolveAutowiredMode() == RootBeanDefinition.AUTOWIRED_BY_NAME || bd.getResolvedAutowiredMode() == RootBeanDefiniton.AUTOWIRE_BY_TYPE){
		MutablePropertyValues newPvs = new MutalbePropertyValues(pvs);
		if(mbd.getResolvedAutowiredMode() == Root.Definiton.AUTOWIRE_BY_NAME){
			autowireByName(beanName, mbd, bw, newPvs);
		}
		if(mbd.getResolvedAutowireMode() == RootBeanDefnition.AUTOWIRE_BY_TYPE){
			autowireByType(beanName, mbd, bw, newPvs);
		}
	}
	...
}	
```

- Spring IOC容器根据Bean名称或者类型进行自动装配依赖注入

```java
protected void autowireByName(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues pvs){
	String []propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
	for(String propertyName : propertyNames){
		if(containsBean(propertyName)){
			Object bean = getBean(propertyName);
			pvs.add(propertyName, bean);
			registerDependentBean(propertyName, beanNane);
		}
	}
}

protected void autowireByType(String beanName, AbstractBeanDefinition mbd, BeanWrapper bw, MutablePropertyValues, pvs){
	TypeConverter converter = getCustomTypeConverter();
	if(converter == null){
		converter = bw;
	}
	Set<String> autowiredBeanNames = new LinkedHashSet<String>(4);
	String[] propertyNames = unsatisfiedNonSimpleProperties(mbd, bw);
	for(Strng propertyName : propertyNames){
		try{
			PropertyDescriptor pd = bw.getPropoertyDescriptor(propertyName);
			if(!Obeject.class.equals(pd.getPropertyType())){
				MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
				boolean eager = !PriorityOrdered.class.isAssignableFrom(bw.getWrappedClass());
				DependencyDescriptor desc = new AutowiredByTypeDependencyDescirptor(methodParam, eager);
				Object autowiredArgument = resolveDependeny(desc, beanName, autowiredBeanNames, converter);
				if(autowiredArgument != null){
					pvs.add(propertyName, autowiredArgument);
				}
				for(String autowiredBeanName : autowiredBeanNames){
					registerDependentBean(autowiredBeanName, beanName);
				}
				autowiredBeanNames.clear();
			}
		}catch(BeansException ex){
			throws new UnsatisfiedDependencyException(mbd.getResourceDescriptionn(), beanName, propertyName, ex);
		}
	}
}

```

- DefaultSingletonBeanRegistry的registerDependentBean方法对属性注入

```java
public void registerDependentBean(String beanName, String dependentBeanName){
	String canonicalName = canonicalName(beanName);
	synchronized(this.dependentBeanMap){
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if(dependentBeans == null){
			dependentBeans = new LinkedHashSet<String>(8);
			dependentBeanMap.put(canonicalName, dependentBeans);
		}
		dependentBeans.add(dependentBeanNames);
	}
}
```


