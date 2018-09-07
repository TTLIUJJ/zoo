# IOC容器的依赖注入

## 1. 依赖注入发生的时间

当Spring IOC容器完成了Bean定义资源的定位、载入和解析注册之后，IOC容器已经管理类Bean定义的相关数据，但是此时IOC容器还没有对所管理的Bean进行依赖注入，依赖注入在以下两种情况发生：

- 用户第一次通过getBean方法向IOC容器获取Bean
- 当用户在Bean定义资源中为\<Bean>元素配置了lazy-init属性，即让容器在解析注册Bean定义时进行预实例化，触发依赖注入。

BeanFactory接口定义了IOC容器的基本功能规范。BeanFactory定义了几个getBean方法，就是用户向IOC容器索取管理的Bean，通过分析其子类动的具体实现，理解Spring IOC容器在用户索取Bean时如何完成依赖注入。

![](http://owj98yrme.bkt.clouddn.com/172312283627249.x-png)


getBean(String beanName)函数，它的具体实现在AbstractBeanFactory中。


## 2. AbstractBeanFactory实现getBean函数


```java
public Object getBean(String name) throws BeansException{
	return doGetBean(name, null, null, false);
}

public <T> T getBean(String name, Class<T> requiredType){
	return doGetBean(name, requiredType, null, null);
}

public Object getBean(String name, Object... args){
	return doGetBean(name, null, args, null);
}

public <T> T getBean(String name, Class<T> requiredType, Object... args){
	return doGetBean(name, requriedType, args, null);
}

@SuppressWarnings("unchecked")
protected <T> T doGetBean(final String name, final Class<T> requiredType, final Object[] args, boolean typeCheckOnly){
	final String beanName = transformeBeanName(name);
	Object bean;
	Object sharedInstance = getSingleton(beanName);
	if(sharedInstance != null && args != null){
		bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
	}
	else{
		if(isPrototypeCurrentlyInCreation(beanName)){
			throw new BeanCurrentlyInCreationException(beanName);
		}
		BeanFactory parentBeanFactory = getParentBeanFactory();
		if(parentBeanFactory != null && !containsBeanDefiniton(beanName)){
			String nameToLookup = originalBeanName(name);
			if(args != null){
				return (T) parentBeanFactory.getBean(nameToLookup, args);
			}
			else{
				return parentBeanFactory,getBean(nameToLookup, requreidType);
			}
		}
		if(!typeCheckOnly){
			markBeanAsCreated(beanName);
		}
		final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
		checkMergedBeanDefiniton(mdb, beanName, args);
		String dependsOn = mdb.getBeans();
		if(dependsOn != null){
			for(String dependOnBean: dependsOn){
				getBean(dependsOnBean);
				registerDepentBean(dependsOnBean, beanName);
			}
		}
		if(mbd.isSingleTon()){
			shreadInstance = getSingleton(beanName, new ObejctFactory(){
				public Object getObject() throws BeansException{
					try{
						return createBean(beanName, mbd, args);
					}catch(BeansException e){
						destroySingleton(beanName);
						throw e;
					}
				}
			});
			bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
		}
		else if(mbd.isPrototype()){
			Object prototypeInstance = null;
			try{
				beforePrototypeCreation(beanName);
				prototypeInstance = createBean(beanName, mbd, args);
			}finally{
				afterPrototypeCreation(beanName);	
			}
			bean = getObejctForBeanInstance(prototypeInstance, name, beanName, mbd);
		}
		else{
			String scopeName = mbd.getScope();
			final Scope scope = this.scopes.get(scopeName);
			if(scope == null){
				throw new IllegalStateException("No Scope registed for scope '" + scopeName + "'");
			}	
			try{
				Obejct scopeInstance = scope.get(beanName, new ObjectFactory{
					public Object getObject() throws Exception{
						beforePrototypeCreation(beanName);
						try{
							return createBean(beanName, mbd, args);
						}finally{
							afterPrototypeCreation(beanName);
						}
					}
				});
				baen = getObejctForBeanInstance(scopedInstance, name, beanName, mbd);
			}catch(IllegalStateException ex){
				throw new BeanCreationException(beanName, "...");
			}
		}
	}
	if(requiredType != null && bean != null && !requiredType.isAssignableFrom(bean.getClass())){
		throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
	}
	return (T) bean;
}
```


## AbstractAutowiredCapableBeanFacotry

```java
protected Obejct creteBean(final String beanName, final RootBeanDefinition mbd, final Object []args) throws BeanCreationException{
	resolveBeanClass(mbd, beanName);
	try{
		mbd.parpareMthodOverrides();
	}catch(BeanDefinitionValidationException e){
		throw new BeanDefinitionStoreException(mbd.getResourceDescription(), beanName, "Validation of method overrides failed", e);
	}
	try{
		Obejct bean = resolveBeforeInstantiation(beanName, mbd);
		if(bean != null){
			return bean;
		}	
	}catch(Throwable e){
		throw new BeanCreationException(mbd.getResourceDescription(), beanName, "BeanPostProcessor before instantiation of bean failed" e);
	}
	Object beanInstance = doCreateBean(beanName, mbd, args);
	return beanInstance;
}

protected Object doCreateBean(final String beanName, final Root BeanDefinition mbd, final Object []args){
	BeanWrapper instanceWrapper = null;
	if(mbd.isSingleton()){
		instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
	}
	if(instanceWrapper == null){
		instanceWrapper = createBeanInstance(beanName, mbd, args);
	}
	final Object bean = (instanceWrapper != null) ? instanceWrapper.getWrappedInstance() : null;
	class beanType = (instancelWrapper != null) ? instanceWrapper.getWrapperClass() : null;
	synchronized(mbd.postProcessingLock){
		if(!mbd.postProcessed){
			applyMergedBenDefinitionPostProcessors(mbd, beanType, beanName);
			mbd.postProcessed = true;
		}
	}
	boolean earlySingletonEcposure = (mbd.isSingleton() && this.allowCircularReference && isSingletonCurrentlyInCreation(beanName));
	if(earylySingletonExposure){
		addSinletonFactory(beanName, new ObjectFactory(){
			public Object getObject(){
				return getEarlyBeanReference(beanName, mbd, bean);
			}
		});
	}
	Object exposedObject = bean;
	try{
		populateBean(beanName, mbd, instanceWrapper);
	}catch(Throwable ex){
		if(ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())){
			throw (BeanCreationException) ex;
		}
		else{
			throw new BeanCreationException(mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
		}
	}
	if(earlySingletonExposure){
		Object earylySingletonReference = getSingleton(beanName, false);
		if(earylySingletonReference != null){
			if(exposedObejct == bean){
				exposedObject = earlySingletonReference;
			}
			else if(!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)){
				String []dependentBeans = getDepentBeans(beanName);
				Set<String> actualDependentBeans = new LinkedHashSet<String>(dependentBeans.length);
				for(String dependentBean: dependentBeans){
					if(!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)){
						actualDependentBeans.add(dependentBean);
					}
				}
				if(!actualDependentBeans.isEmpty()){
					throw new BeanCurrentInCreationException(...)
				}
			}
		}
	}
	try{
		registerDisposableBeanIfNecessary(beanName, bean, mbd);
	}catch(BeanDefinitionValidationException ex){
		thorw new BeanDefinitonException(mbd.getResourceDescription(), beanName, "Invalid destruction singature");
	}
	return exposedObejct;
}
```

## createBeanInstance 方法创建Bean的java实例


```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, Obejcrt []args){
	Class beanClass = resolveBeanClass(mbd, beanName);
	if(beanClass != null && !Modifier.isPublic(beanClass.getModifiers()) && !mbd.isNonPublicAccessAllowed()){
		thorw new BeanCreationException(mbd.getResourceDescription(), beanName, "Bean Class isn't public, and non-public access not allowed: " + beanClass.getName());
	}
	if(mbd.getFactoryMethodName() != null){
		return instantiateUsingFactoryMthod(beanName, mbd, args);
	}
	boolean resolved = false;
	booealn autowiredNecessary = false;
	if(args == null){
		synchronized(mbd.constructorArgumentLoc){
			if(mbd.resolvedConstructorOrFactoryMethod != null){
				resoved = true;
				autowiredNecessary = mbd.constructorArgumentResovled;
			}
		}
	}
	if(resolved){
		if(autowiredNecessay){
			return autowiredConstructor(beanName, mbd, null, null);
		}
		else{
			return instantiateBean(beanName, mbd);
		}
	}
	Constructor []ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
	if(ctors != null || mbd.getResolvedAutowiredMode() == RootBeanDefinitionAUTOWIRED_CONSTRUCTOR || mbd.hasConstrucotrArgumentValues() || !ObjectUtils.isEmpty(args)){
		return autowiredConstructor(beanName, mbd, ctors, args);
	}
	return instantiateBean(beanName, mbd);
}

protected BeanWarpper instantiateBean(final String beanName, final RootBeanDefinition mbd){
	try{
		Object beanInstance;
		final BeanFactory parent = null;
		if(System.getSecurityManager() != null){
			beanInstance = AccessController.doPrivileged(new PrivilegeAction<Object>(){
				public Object run(){
					return getInstantiationStrategy().instantiate(mbd, beanName, parent)
				}
			}, getAccessControlContext());
		}
		else{
			beanInstance = getInstantiationStrategy().instantiate(mbd, beanName, parent);
		}
		BeanWrapper bw = new BeanWrapperImpl(beanInstance);
		initBeanWrapper(bw);
		return bw;
	}catch(Throwable ex){
		throw new BeanCreationException(mbd.getResourceDescirption(), beanName, "Instantiation of bean failed");
	}
}
```

## SimpleInstantiationStrategy 

