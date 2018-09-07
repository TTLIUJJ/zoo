# SpringBoot启动流程

基于 Spring Boot 1.3.5 版本

```java
public class SpringApplication {
	public static ConfigurableApplicationContext run(Object source, String... args) {
		return run(new Object[]{source}, args);
	}

	public static ConfigurableApplicationContext run(Object Source, String[] args) {
		return (new StringApplication(source)).run(args);
	}
}
```

SpringBoot的启动流程大概可以分成两部分

- new

- run

下面会介绍自动配置一些相关的源码


## new 阶段

```java
package org.springframework.boot;

public Class SpringApplication {
	public static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context.annotation.AnnotationConfigApplicationContext";
	public static final String DEFAULT_WEB_CONTEXT_CLASS = "org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext";
	private List<ApplicationContextInitilaizer<?>> initailizers;
	private List<ApplicationListener<?>> listeners;
		
	public StringApplication(Object... sources) {
		this.bannerMode = Mode.CONSOLE;
		this.logStartup = true;
		this.addCommandLinProperties = true;
		this.headless = true;
		this.registerShutdownHook = true;
		this.additionalProfiles = new HashSet();
		this.initialize(sources);
	}
	
	/**
	 * 初始化操作:
	 *   1. webEnvironment
	 *   2. 判断两个Class是否在web环境中
	 *      javax.servelet.Servlet以及
	 *      org.springframework.web.context.ConfigurableWebApplicationContext
	 *   3. 从spring.factories文件中找出所有应用程序的初始化器和监听器
	 *      并且分别在实例化分别赋予SpringApplication中的两个成员变量
	 *      initializers和listeners
	 *   4. mainApplicationClass          
	 */
	private void initialize(Object sources) {
		if(sources != null && sources.length > 0) {
			this.sources.addAll(Arrays.asList(sources));
		}
		this.webEnvironmetn = this.deduceEnvironment();
		this.setInitialzers(this.getSpringFactoriesInstances(ApplicationContextInitializer.class));
		this.setListeners(this.getSpringFactoriesInstances(ApplicationListner.class));
		this.mainApplicationClass = this.deduceMainApplicationClass();
	}
	
	private <T> Collection<? extends T> getSpringFactoriesInstances(Class<T> type) {
		return this.getSpringFactoriesInstances(type, new Class[0]);
	}
	/**
	 * 在spring.factories文件中找寻基础工厂接口
	 * 并且返回实现了该接口的工厂的对象实例
	 * @param type 接口，包含：
	 *                   ApplicationContextInitializer
	 *                   ApplicationListener
	 * @return 返回实现了type接口的工厂类
	 */
	private <T> Collections<? extends T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Set<String> names = new LinkedHashSet(SrpingFactoriesLoader.loadFactoryNames(type, classLoader));
		List<T> instances = this.createSpringFactoriesInstances(type, parameterTypes, classLoader, args, naems);
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}
	
	/**
	 * @param type 基础接口
	 * @names names 实现了type接口的类名的集合
	 * @return 返回实现了type接口的类实例
	 */
	private <T> List<T> createSpringFactoriesInstances(Class<T> type, 
		Class<?>[] paramterTypes,
		ClassLoader classLoader,
		Object[] args, 
		Set<String> names) {
		
		List<T> instances = new ArrayList(names.size());
		Iterator var7 = names.iterator();
		
		while(var7.hasNext()) {
			String name = (String)var7.next();
			try {
				Class<?> instanceClass = ClassUtils.forName(name, classLoader);
				Assert.isAssginable(type, instanceClass);
				/** 可以使用非默认构造函数初始化对象 */
				Constructor<?> constructor = instanceClass.getConstructor(paramterTypes);
				T instance = constructor.newInstance(args);
				instances.add(instance);
			} catch (Throwable var12) {
				throw new IllegallArgumentException(
					"Cannot instance " + type + " : " + name, var12);
			}
		}
	}
	
	/* ******* 非StringApplication类中函数 ******** */
	 
	 
	/**
	 * 类: SpringFactoriesLoader
	 * 在系统下的spring.factory查找实现了factoryClass的类
	 * @param factoryClass 工厂类的基本接口
	 * @param classLoader 类加载器
	 * @return 返回实现类名称的集合
	 */ 
	public static List<String> loadFactoryName(Class<?> factoryClass, ClassLoader classLoader) {
		String factoryClassName = factoryClass.getName();
		try {
			Enumeration<URL> urls = classLoader != null ?
				classLoader.getResources("META-INF/spring.factories") : 
				ClassLoader.getSystemResources("META-INF/spring.factories");
				
				ArrayList result = new ArrayList();
				while(urls.hasMoreElements()) {
					URL url = (URL)urls.nextElement();
					Properties properties = PropertiesLoaderUtils.loadProperties(new UrlReource(url));
					String factoryClassNames = properties.getProperty(factoryClassName);
					result.addAll(Arrays.asList(StringUtils.commaDelimitedListToStringArray(factoryClassNames)));
				}
				return result;
		}catch (IOException var8) {
			throw new IllegalArgumentException("Unable to load [" +
			 	factoryClass.getName() + "] factorys from location [" + 
			 	"META-INF/spring.factories]", var8);
		}
	}
}
```

### AppicationContextIniitalizer

在setIniitalizers方法中被实例化的initialzer类有5个

```java
"org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer"
"org.springframework.boot.context.ContextIdApplicationContextInitializer"
"org.springframework.boot.context.config.DelegatingApplicationContextInitializer"
"org.springframework.boot.context.web.ServerPortInfoApplicationContextInitializer"
"org.springframework.boot.autoconfigure.logging.AutoConfigurationReportLoggingInitializer"
```

这5个Initialzer都实现了ApplicationContextInitialzer。即和程序上下文有关的初始化器

### ApplicationListener

在setListeners方法中被实例化的listener类有9个

```java
"org.springframework.boot.builder.ParentContextCloserApplicationListener"
"org.springframework.boot.context.FileEncodingApplicationListener"
"org.springframework.boot.context.config.AnsiOutputApplicationListener"
"org.springframework.boot.context.config.ConfigFileApplicationListener"
"org.springframework.boot.context.config.DelegatingApplicationListener"
"org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener"
"org.springframework.boot.logging.ClasspathLoggingApplicationListener"
"org.springframework.boot.logging.LoggingApplicationListener"
"org.springframework.boot.autoconfigure.BackgroundPreinitializer"
```


这9个Listener都实现了ApplicationListener接口。即表示监听对应的Application事件





## Run 阶段

在SpringBoot实例化依赖的注解类之后，现在进入了run阶段，其实根据函数名，我们很容易推测出每一步进行了什么操作。

```java
public class SpringApplication {
	// ...
	
	/**
	 * run阶段的入口
	 * 1. stopWatch 监控SpringBoot的启动和关闭操作
	 * 2.listeners 
	 */	
	public ConfigurationApplicationContext run(String.. args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfiguraableApplicationContext context = null;
		this.configureHeadlessProperty();
		SpringApplicationRunListeners listeners = this.getRunListeners(args);
		listeners.started();
		
		try {
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
			context = this.createAndRefreshContext(listeners, applicationArguments);
			listeners.finished(context, (Throwable)null);
			stopWatch.stop();
			if(this.logStartupInfo) {
				(new StartupInfoLogger(this.mainApplicationClass))
					.logStarted(this.getApplicationLog(), stopWatch);
			}
			
			return context;
		} catch (Throwable var6) {
			this.handleRunFailure(context, listeners, var6);
			throw new IllegalStateException(var6);
		}	
	}
	
	/**
	 * 与初始化阶段的ApplicationListener不同
	 * 传入getSpringFactoriesInstacne中的参数是SpringApplicationRunListener
	 * @return SpringApplicationRunListeners
	 */
	private SpringApplicationRunListeners getRunListeners(String []args) {
		Class<?> []types = new Class[] { SpringApplication.class, String[].class }
		return new SpringApplicationRunListeners(logger,
		 	this.getSpringFactoryiesInstances(SpringApplicationRunListener.class,types, this, args)
		 );
	}
	
	/**
	 * 初始化应用程序的上下文
	 * @return DEFAULT_WEB_CONTEXT_CLASS 或者 DEFAULT_CONTEXT_CLASS
	 *         笔者这里返回的是前者
	 */
	private ConfigurableApplicationContext createAndRefreshContext(
		SpringApplicatonRunListeners listeners,
		ApplicationArguments applicationArguments) {
		
		ConfigurableEnvironment environment = this.getOrcreateEnvironment();
		this.configureEnvironment(environment, applicationArguments.getSourceArgs());
		listeners.environmentPrepared(environment);
		if(this.isWebEnvironment(environment) && !this.webEnvironment) {
			environment = this.convertToStandardEnvironment(environment);
		}
		
		/**
		 * Mode: OFF
		 *       CONSOLE
		 *       LOG
		 * 除了系统处于OFF模式，不然会打印出SpringBoot的启动图标
		 */
		if(this.bannerMode != Mode.OFF) {
			this.printBanner(environment);
		}
		
		ConfigurableApplicationContext context = this.createApplicationContext();
		context.setEnvironment(environment);
		this.postProcessApplicationcContext(context);
		this.applyInitializers(context);
		listeners.contextPrepared(context);
		if(this.logStartupInfo) {
			this.logStartupInfo(context.getParent == null);
			this.logstartupProfileInfo(context);
		}
		
		/** 
		 * 1. 把应用程序的参数持有类 注册到Spring容器中 并且是一个单例
		 * 2. sources在笔者的程序中就是一个MyApp类
		 * 3. load函数加
		 * 4. listeners.contextLoaded进行广播
		 * 5. Spring容器的刷新操作
		 */
		context.getBeanFactory().
			registerSingleton("springApplicationArguments", applicationArguments);
		Set<Object> sources = this.getSources();
		Assert.notEmpty(sources, "Sources must not be empty");
		this.load(context, sources.toArray(new Object[sources.size()]));
		listeners.contextLoaded(context);
		this.refresh(context);
		if(this.registerShutdownHook) {
			try {
				context.registerShutdownHook();
			} catch (AccessControlException var7) {
				;
			}
		}		
	}
	
	/** 
	  * 实际上调用了AbstractApplicationContext类
	  * 并调用了refresh方法进行刷新
	  */
	protected void refresh(ApplicationContext context) {
		Assert.isInstanceOf(AbstractApplicationContext.class, context);
		((AbstractApplicationContext)context).refresh();
	}
	
	/**
	 * 获取ConfigurableEnvironment配置环境
	 * 笔者这里返回的是新建的StardardServletEnvironment
	 */
	private ConfigurableEnvironment getOrCreateEnvironment() {
		if (this.environment != null) {
			return this.environment;
		}
		else {
			return (ConfigurableEnvironment)(this.webEnvironment ? 
				new StandardServletEnvironment() : new StardEnvironment());
		}
	}
	
	/**
	 * 由于笔者没有设置自定义的applicationContextClass
	 * 所以返回的是SpringBoot创建的AnnotationConfigEmbeddedWebApplicationContext
	 */
	protected ConfigurableApplicationContext createApplicationContext() {
		Class<?> contextClass = this.applicationContextClass;
		if(contextClass == null) {
			try {
				contextClass = Class.forName(this.webEnvironment ?
					"org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext" :
					"org.springframework.context.annotation.AnnotationConfigApplicationContext");
			} catch (ClassNotFoundException var3) {
				throw new IllegalStateException("Unable crate a default ApplicationContext, please specify an ApplicationContextClass", var3);
			}	
		}
		return (ConfigurableApplicatonContext)BeanUtils.instantkate(contextClass);
	}
}
```

### run阶段的监听事件


笔者查看的是SpringBoot 1.3.5版本，目前只有EventPublishingRunListener类实现了SpringSpringApplicationListener接口




```java
package org.springframework.boot;

class SpringApplicationRunListeners {
	private final Log log;
	private final List<SpringApplocationRunListener> listeners;
	
	
	/* listeners集合中只有EventPublishingRunListener一个实例 */
	SpringApplicatonRunListeners(Log log, 
		Collection<? extends SpringApplicationRunListener> listeners) {
		
		this.log = log;
		this.listeners = new ArrayList(listeners);
	}
	/**
	 * 从该函数中，我们对于SpringApplicationRunListeners的作用一目了然
	 * 由于listeners保存了SpringApplicationRunListener的所有实现类
	 * 所以可以调用所有实现类的started方法
	 */
	public void started() {
		Iterator var1 = this.listeners.iterator();
		
		while(var1.hasNext()) {
			SpringApplicationRunListener listener = (SpringApplicationRunListener)var1.next();
			listener.strated();
		}
	}
	
	/**
	 * 以下几个方法的实现与started实现了相同的功能
	 */
	public void environmentPrepared(ConfigurableEnvironment environment) { ... }
	public void contextPrepared(ConfigurableApplicationContext context) { ... }
	public void contextLoaded(ConfigurableApplicatonContext context) { ... }
	public void finished(ConfigurableApplicationContext context) { ... }
}
```

### 创建配置环境

![](http://owj98yrme.bkt.clouddn.com/StandardServletEnvironment.png)

由于SpringBoot判断笔者的环境是webEnvironment，所以创建了StardardServletEnvironment，该类还创建了Log...//TODO


## 创建WebApplicationContext

![](http://owj98yrme.bkt.clouddn.com/AnnotationConfigEmbeddedWebApplicationContext.png)


### AbstractApplicatonContext

```java
public abstract class AbstractApplicationContext 
	extends DefualtResourceLoader
	implements ConfigurableApplicationContext, DisposableBen {
	
	// ...
	
	publi void refresh() throws BeanException, IlleagalStateException {
		Obejct var1 = this.startShutdownMonitor;
		synchronized(this.startupShutdownMonitor) {
			this.prepareRefresh();
			ConfigurableListableBeanFactory beanFactory = this.obtainFreshBeanFactory();
			this.prepareBeanFactory(beanFactory);
			
			try {
				this.postProcessBeanFactory(beanFactory);
				this.invokeBeanFactoryPostProcessors(beanFactory);
				this.registerBeanPostProcessors(beanFactory);
				this.initMessageSource();
				this.initApplicationEventMulticaster();
				this.onRefresh();
				this.registerListeners();
				this.finishBeanFactoryInitialization(beanFactory);
				this.finishRefresh();
			} catch (BeansException var9) {
				if(this.logger.isWarnEnabled()) {
					this.logger.warn("Exception encountered during context iniitalization - canceling refresh attempt: ", var9);
				}
				this.destroyBeans();
				this.cancelRefresh(var9);
				throw var9;
			} finally {
				this.resetCommonCaches();
			}
		}
	}
}
```