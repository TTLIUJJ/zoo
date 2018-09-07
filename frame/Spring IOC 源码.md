# Spring IOC 源码

**参考博文：**

1. [Spring：源码解读Spring IOC原理](https://www.cnblogs.com/ITtangtang/p/3978349.html)
2. [Spring源码分析——BeanFactory体系之接口详细分析](https://www.cnblogs.com/zrtqsk/p/4028453.html)

- 什么是IOC/DI
- Spring IOC体系结构
- IOC容器的初始化
- IOC容器的依赖注入
- IOC容器的高级特性


## 什么是IOC/DI

IOC容器：主要是完成了对象的创建和管理依赖注入

所谓控制反转，就是把原先我们代码里买呢需要实现的对象创建，反转给容器来帮忙。那么就需要创建一个容器，同时需要一种描述来让容器知道需要创建的对象与其他对象的关系，这个描述的具体表现就是可配置的文件。

对象之间的表示：xml和properties文件等语义化配置文件表示。

描述对象关系的文件存放在：classpath、filesystem、URL网络资源和servletContext等。

对配置文件进行解析：不同的配置文件对对象的描述不一样，如何做到统一？Spring需要有一个特定的统一的关于对象的定义，所有来自不同配置文件的描述都必须转为统一的描述定义，那么对于不同的配置文件语法，采用不同的解析器。


## Spring IOC 体系结构

### BeanFactory

Spring Bean的创建是典型的工厂模式，这一系列的Bean工厂，也是IOC容器管理对象之间的依赖的基础，在Spring中有许多的IOC容器的实现供用户选择和使用。

![](http://p5s0bbd0l.bkt.clouddn.com/172219470349285.x-png)

BeanFactory作为最顶层的一个接口类，定义了IOC容器的基本功能规范，它的三个子类分别是ListableBeanFactory、HierarchicalBeanFactory和AutowireCapableBeanFactory。在上图中，最终的默认实现类是DefaultLisableBeanFactory，它实现了所有的接口。这么多层次的接口，每个都有它使用的场景，目的是是了区分在Spring内部操作过程中对象的传递和转化过程，对对象的数据访问所做的限制，比如
	
- ListableBeanFactory接口表示这些Bean是可列表的，提供所有Bean实例的枚举，不再需要客户端通过一个个bean's name查找。
- HierarchicalBeanFactory接口提供父容器的访问功能。
- AutowireCapableBeanFactory接口定了Bean的自动装配规则，根据类定义BeanDefinition装配Bean、执行前、执行后处理器等。

这些Factory共同定义了Bean的集合、Bean之间的关系以及Bean的行为。

#### BeanFactory

最基本的IOC容器接口BeanFactory

```java
public interface BeanFactory{
	//对FactoryBean的转义定义
	//如果使用bean's name检索FactoryBean得到的对象是工厂生成的对象
	//如果需要得到工厂本身，需要转义
	String Factory_Bean_PREFIX = "&";
	
	//根据bean's name获取在IOC容器中的bean实例
	Object getBean(String name) throws BeansException;

	//根据bean's name及其Class' name获取bean实例，增加类型安全验证机制
	Object getBean(String name, Class requiredType) thorws BeansException;
	
	boolean containsBean(String name);
	boolean isSingleton(String name) thows NoSuchBeanDefinitionException;
	Class getType(String name) throws NoSuchBeanDefinitionException;
	
	//得到bean的别名，若根据别名调用方法，会返回其原名
	String[] getAliases(String name);
}
```

在BeanFactory里，只对IOC容器的基本行为作了定义，没有定义任何关于Bean是如何加载的。正如工厂模式，客户只关心得能不能得到想要的产品，至于工厂如何生产这些对象的，BeanFactory这个基本接口不关心。


至于要知道工厂是如何生产对象的，需要看具体的IOC容器实现，比如如下两个典型IOC容器：

- XmlBeanFactory：最基本的IOC容器的具体实现，可以读取XML文件定义的BeanDefinition（XML文件中对Bean的描述）。

- ClassPathXmlApplicationContext：Spring提供的一个高级IOC容器，除了提供IOC容器的基本功能外，还为用户提供了如下附加服务：
	
	- 支持信息源，可以实现国际化（实现MessageSource接口）
	- 访问资源（实现ResoucePatternResolver接口）
	- 支持应用事件（实现ApplicationEventPublisher接口）

#### BeanDefinition

Spring IOC容器管理各种Bean对象及其之间的相互关系，Bean对象在SPring中的实现通过BeanDefinition来描述。

Bean的解析过程非常复杂，功能被分的很细，因为需要被扩展的地方很多，需要保证有足够的灵活性，以应对可能的各种变化。Bean的解析主要是对Spring配置文件的解析。


## IOC容器的初始化

IOC容器的初始化包括BeanDefinition的Resource定位、载入和注册这三个基本的过程。以ApplicationContext这个最常使用的类来分析，继承图如下：

![](http://p5s0bbd0l.bkt.clouddn.com/172222438935854.x-png)


ApplicationContext允许上下文嵌套，通过保上级的上下文可以维持一个上下文体系。对于Bean的查找可以在这个上下文体系中发生，首先检查当前上下文，其次是父上下文，逐级向上查找，这样可以为不同的Spring应用提供一个共享的Bean定义环境。


### XmlBeanFactory

在详看ApplicationContext之前，先了解一下XmlBeanFactory的容器创建过程。

```java
public class XmlBeanFactory extends DefaultListableFactory{
	private final XmlBeanDefinitionReader reader;
	
	public XmlBeanFactory(Resource resource) thorows BeansException{
		this(resource, null);
	}
	
	public XmlBeanFactory(Resource resource, BeanFactory parentBeanFactory) throws BeansException{
		super(parentBeanFactory);
		this.reader = new XmlBeanDedinitionReader(this);
		this.reader.loadBeanDefinitons(resource);
	}
	
}
```

### FileSystemXmlApplicationContext

FileSystemXmlApplicationContext容器的初始化

```java
	ApplicationContext context = new FileSystemXmlApplicationContext("my_application.xml");
```

#### 1. 构造函数

```java
public FileSystemXmlApplicationContext(String... configLocations) throws BeansException(){
	this(configLocations, true, null);
}

public FileSystemXmlApplicationContext(String []configLocations, boolean refresh, ApplicationContext parent) throws BeansException{
	super(parent);
	setConfigLocation(configLocations);
	if(refresh){
		refresh();
	}
}
```

构造函数做了两点工作，设置资源加载器和资源定位
：

- 通过父类容器的构造方法(super)，为当前容器设置Bean的资源加载器
- 调用父类AbstractRefreshableConfigApplicationContext的setConfigLocations方法设置Bean定义资源文件的路径


#### 2. 父类AbstractApplicationContext的工作如源码所示：

- 设置资源加载器

```java
public abstract class AbstractApplicationContext extends DefaultResourceLoader implements ConfigurableApplicationContext, DisposableBean{
	static{
		//避免应用程序出现类加载异常
		//AbstractApplicationContext确保加载IOC容器关闭事件
		ContextCloseEvent.class.getName();
	}
	
 	public AbstractApplicationContext(ApplicationContext paretn){
		this.parent = parent;
		this.resourcePatternResolver = getResourcePatternResolver(); 
	}	
	
	//获取一个Spring resource的加载器用于读入Srping Bean的资源定义文件
	protected ResourcePatternResolver getResourcePatternResolver(){
		return new PathMatchingResourcePatternResolver(this);
	}

}
```

```java
public PathMatchingResourcePatternResolver(ResourceLoader resourceLoader){
	Assert.notNull(resource Loader, "ResourceLoader must not be null");
	this.resourceLoader = resourceLoader;
}
```

- 资源定位

在设置容器的资源加载器之后，接下来FileSystemXmlApplicationContext执行setConfigLocations方法通过setConfigLocations方法通过调用其父类AbstractRefreshableConfigApplicationContext的方法进行对资源文件的定位，源码如下：

```java
public void setConfigLocation(String location){
	//CONFIG_LOCATION_DELIMITERS = ",;/t/n"
	//即多个资源文件路径之间使用",;/t/n"分隔，解析为数组形式
	setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
}

public void setConfigLocation(String []locations){
	if(locations != null){
		Assert.notNullElements(locations, "Config locations must not be null");
		this.configLocations = new String[locations.length];
		for(int i = 0; i < locations.length; ++i){
			//resolvePath将字符串解析为路径
			this.configLocations[i] = resolvePath(locations[i].trim());
		}
	}
	else{
		this.configLocations = null;
	}
}
```

从源码看出，可以使用一个字符串来配置多个Srping Bean定义的资源文件，也可以使用字符串数组。

```java
// 1. 字符串
ClasspathResource res1 = new ClassPathResource("a.xml,b.xml,c.xml");

// 2. 数组
ClasspathResource res2 = new ClasspathResource(new String[]{"a.xml,"b.xml","c.xml"});

```

至此，Spring IOC容器在初始化时将定位到配置的Bean资源文件，并将其转化可以使用的Resource对象


#### 3. AbstractApplicationContext的refresh函数

Spring IOC容器对Bean对资源的载入是从refresh()函数开始的，refresh()是一个模板方法，其作用是：在创建IOC容器之前，如果容器已经存在，则需要将其关闭并销毁。


FileSystemXmlApplicationContext通过调用其父类AbstractApplicationContext的refresh()函数启动整个IOC容器对Bean定义的载入过程：

```java
public void refresh() throws BeansException, IllegaStateException{
	synchronized(this.startupShutdwonMonitor){
		prepareRefresh();
		ConfigurableListableFactory beanFactory = obtainFreshBeanFactory();
		prepareBeanFactory(beanFactory);
		try{
			postProcessBeanFactory(beanFactory);
			invokeBeanFactoryPostProcessors(beanFactory);
			registerBeanPostProcessors(beanFactory);
			initNessageSource();
			initApplicationEventMulticaster();
			onRefresh();
			registerLiseners();
			finishBeanFactoryInitialization(beanFactory);
			finishRefresh();
		}cathc(BeanException e){
			destroyBeans();
			cancelRefresh(e);
			throw e;
		}
	}
}
``` 

refresh()方法主要为IOC容器的生命周期管理提供了条件，Spring IOC容器载入Bean资源文件从其子类容器的refreshBeanFactory()方法启动，所以整个refresh()方法中的载入过程就是从“ConfigurableListableFactory beanFactory = obtainFreshBeanFactory();”这行代码开始启动的。


#### 4. AbstractApplicationContext子类的refreshBeanFactory()

AbstractApplicationContext类只抽象定义了refreshBeanFactory()方法，容器真正调用的是其子类AbstractRefreshableApplicationContext实现的refreshBeanFactory()方法

```java
protected final void refreshBeanFactory() throws BeansException{
	//如果已有容器，销毁容器中的所有Bean，并且关闭容器
	if(hasBeanFactory()){
		destroyBeans();
		closeBeanFactory();
	}
	
	try{
		DefaultListableBeanFactory beanFactory = createBeanFactory();
		beanFactory.setSerializationId(getId());
		//IOC容器的定制化，比如设置启动参数，开启注解的自动装配等
		customizeBeanFactory(beanFactory);
		loadBeanDefinitions(beanFactory);
		synchronized(this.beanFactoryMonitor){
			this.beanFactory = beanFactory;
		}		
	}catch(IOException e){
		throw new ApplicationContextException("I/O error parsing bean definition source for " + getDisplayName(), e);
	}
}
```

在这个方法中，先判断BeanFactory是否存在，是则销毁Beans并关闭容器，接着创建DefaultListableBeanFactory，并调用loadBeanDefinitions(beanFactory)装载Bean的定义。

#### 5. AbstractRefreshableApplicationContext子类的loadBeanDefinitions()

同样的，AbstractRefreshApplicationContext中只定义了loadBeanDefinitions()，真正调用的是其子类的AbstractXmlApplicationContexr对该方法的实现。

```java
public abstract class AbstractXmlApplicationContext extends AbstractRefreshableConfigApplicationContext{
	@Override
	protected void loadBeanDefinition(DefaultListablBeanFactory beanFactory) throws BeanException, IOExcetpion{
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.setResourceLoader(this);
		reader.EntityResolver(new ResourceEntityResolver(this));
		initBeanDefinitionReader(reader);
		loadBeanDefinitions(reader);	
	}
	
	protected void loadBeanDefinitions(XmlBeanDefinitionReader reader) throws BeansException, IOException{
		Resource[] configResources = getConfigResources();
		if(configResoures != null){
			reader.loadBeanDefinitions(configResources);
		}
		String []configLocations = getConfigLocations();
		if(configLocations != null){
			reader.loadBeanDefinitions(configLocations);
		}
	}
	
	//这里又使用了委托模式，调用子类获取Bean的资源定位方法
	//该方法在ClassPathXmlApplicationContext中进行实现
	//对于FileSystemXmlApplicationContext没有使用该方法
	protected Resourece[] getConfigResources(){
		return null;
	}
}
```

XmlBean读取器(XmlBeanDefinitonReader)调用其父类AbstractBeanDefinitionReader的reader.loadBeanDefinitions(...)读取Bean资源文件。

#### 6. 读取定义Bean的资源文件

在org.springframework.beans.factory.support包可以看到BeanDefinitionReader结构

![](http://p5s0bbd0l.bkt.clouddn.com/172239426289723.x-png)

- 在抽象父类AbstractBeanDefinitionReader中定义了载入过程

	- XmlBeanDefinitionReader通过调用父类DefaultResourceLoader的getResource()读取要加载的资源

	- 子类XmlBeanDefinitionReader的loadBeanDefinitions是真正执行加载功能方法

抽象父类AbstractBeanDefinitionReader中定义了载入的过程


```java
public int loadBeanDefinition(String location) throws BeanDefinitionsStoreException{
	return loadBeanDefinitions(location, null);
}

public int loadBeandefinitions(String location, Set<Resource> actualResources) throws BeanDefinitionStroeException{
	ResourceLoader resourceLoader = getResouceLoader();
	if(resourceLoader == null){
		throw new BeanDefinitionStoreException(
		"Can't import bean definitions from location [" + location + "]: no ResourceLoader available" );
	}
	if(resourceLoader instanceof ResourcePatternResolver){
		try{
			Resource[] resouces = ((ResourcePatternResolver)resourceLoader).getResources(location);
			int loadCount = loadBeanDefinition(resoureces);
			if(actualResources != null){
				for(Resource resource : resource){
					actualResource.add(resource);
				}
			}
			return loadCount;	
		}catch(IOException e){
			thorw new BeanDefinitionStoreException(
			"Cant't resolve bean definition resource [" + location + "]", e);
		}
	}
	else{
		Resource resource = resourceLoader.getResource(location);
		int loadCount = loadBeanDefinitions(resource);
		if(acutalResouces != null){
			actualResouces.add(resouce);
		}
		return loadCount;
	}
}

public int loadBeanDefinitions(String.. locations) throws BeanDefinitionStroeException{
	Assert.notNull(locations, "Location array must be not null");
	int counter = 0;
	for(String location: locations){
		counter += loadBeanDefinitions(location);
	}
	return counter;
}
```

XmlBeanDefinitionReader通过调用父类的DefaultResourceLoader的getResource方法获取要加载的资源


- classpath:com/myapp/config.xml从classpath中加载
- file:/data/config.xml 作为URL从文件系统中加载
- http://server/log.png 作为URL从加载
- /data/config.xml 根据ApplicationContext进行判断


```java
public Resource getResource(String laoction){
	Assert.notNull(location, "Location must not be null");
	//使用classpathResource来得到Bean资源文件
	if(location.startsWith(CLASSPATH_URL_PREFIX)){
		return new ClassPathResource(Location.substring(CLASSPATH_URL_PREFIX.length()), getCloassLoader());
	}	
	try{
		//使用UrlResource作为Bean文件的资源对象
		URL url = new URL(location);
		return new UrlResource(url);
	}catch(MalformedURLException e){
		// ...
	}
	return getResourceByPath(location);
}

//FileSystemXmlApplicationContext容器提供了getResouceByPath方法
//用于处理既不是通过classpath表示，又不是URL表示的Resouce定位的情况。
protected Resource getResourceByPath(String path){
	if(path != null && path.startWith("/")){
		path = path.substring(1);
	}
	//使用文件系统资源对象来得到Bean资源文件
	return new FileSystemResource(path);
}
```

在取得Bean定义的资源文件后，XmlBeandefinitionReader的laodBeanDefinitions(Resource resource)

```java
public int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException{
	//EncodeResource将读入的Xml资源进行特殊的编码处理
	return loadBeanDefinitions(new EncodeResource(resource));
}
	
public int loadBeanDefinitions(EncodeResource encodeResource) throws BeanDefinitionStoreException{
	// ... 跳过部分代码
	InputStream inputStream = encodeResource.getResource.getInputStream();
	try{
		//从InputStream中得到经过编码后的Xml解析源
		InputSource inputSource = new InputSource(inputStream);
		if(encodedResource.getEncoding() != null){
			inputSource.setEncoding(encodeResource.getEncoding());
		}
		//这里是具体的读取被编码过的资源文件的过程
		return doLoadBeanDefinitions(inputSource, encodedResource.getResources());
	}finnally{
		//关闭IO流
		inputStream.close();
	}	
}

//从被编码过的Xml资源文件中实际载入Bean的定义
protected int doLoadBeanDefinitions(InputSource inputSource, Resource resource) throws BeanDefinitionStoreException{
	try{
		int validationMode = getValidationModeForResource(resource);
		//将资源文件转化为DOM对象，解析过程由documentLoader实现
		Document document = this.documentLoader.loadDocument(
		inputSource, this.entityResolver, this.errorHandler, validationMode, this.namespaceAware);
		//启动对Bean定义解析的详细过程
		//该过程会用到Spring的Bean配置规则
		return registerBeanDefinitions(document, resource);
	}catch(){
		// ...
	}finnally{
		// ...
	}
}

```

载入Bean定义资源文件的最后一步是将资源文件转化为Document对象，该过程由documentLoader实现。

#### 7. DocumentLoader将Bean定义资源转换为Document对象


```java
public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler, int validationMode, boolean namespaceAware) throws Exceptiopn{
	DocumentBuilderFactory factory = createDocumentBuilderFactory(validationMode, namespaceAware);
	
	DocumentBuilder builder = createDocumentBuilder(factory, entityResolver, errorHadler);
	return builder.parse(inputSource);
}
```

```java
public int registerBeanDefinitions(Document doc, Resource resource) throws BeanDefinitionStroeException{
	BeanDefinitionDocumentReader documnetReader = createBeanDefinitionDocumentReader();
	int countBefore = getRegistry().getBeanDefinitionCount();
	documentReader.registerBeanDefinitions(doc, createReaderContext(resource));
	return getRegistry().getBeanDefinitionCount() - countBefore();
}
```


Bean 定义资源的载入解析分为两个过程：

- 首先，通过XML解析器将Bean定义资源文件转换得到Document对象，但是这些Document对象并没有按照Spring Bean规则进行解析，这仅仅是载入的过程。


```java
public void registerBeanDefinitions(Document doc, XmlReaderContext readerContext){
	this.readerContext = readerContext;
	Element root = doc.getDocumentElement();
	BeanDefinitionParseDelegate delegate = createHeader(readerContext, root);
	preProcessXml(root, delegate);
	parseBeanDefinitions(root, delegate);
	postProcessXml(root);
}

protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root){
	BeanDefinitionParserDelegate delegate = new BeanDefinitionParserDelegate(readerContext);
	delegate.initDefaults(root);
	return delegate;
}

protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate){
	if(delegate.isDefaultNamespace(root)){
		NodeList nl = root.getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i){
			Node node = nl.item(i);
			if(node instanceof Element){
				Element ele = (Element) node;
				if(delegate.isDefaultNamespace(ele)){
					parseDefaultElement(ele, delegate);
				}
				else{
					delegate.parseCustomElement(ele);
				}
			}
		}
	}
	else{
		delegate.parseCustomElement(root);
	}
}

private void parseDefaultElement(Element ele, BeandefinitionParserDelegate delegate){
	if(delegate.nodeNamespaceEquals(ele, IMPORT_ELEMENT)){
		importBeanDefinitonResource(ele);
	}
	else if(delegate.nodeNamespaceEquals(ele, ALIAS_ELEMENT)){
		processAliasRegistration(ele);
	}
	else if(delegate.nodeNamespaceEquals(ele, BEAN_ELEMENT)){
		processBeanDefinition(ele, delegate);
	}
}

protectd void importBeanDefinitionResource(Element ele){
	String location = ele.getAttribute(RESOURCE_ATTRIBUTE);
	if(!StringUtils.hasText(location)){
		getReaderContext.error("Resource location must not empty", ele);
	}
	location = SystemPropertyUtils.resolvePlaceholders(location);
	Set<Resource> actualResources = new LinkdHashSet<Resource>(4);
	boolean absoluteLocation = false;
	try{
		absoluteLocation = ResourcePathPatternUtils.isUrl(locaton) || ResourceUtils.toURI(location).isAbsolute();
	}catch(URISyntaxException e){
		// ...
	}
	
	if(absoluteLocation){
		try{
			int importCount = getReaderContext.getRader().loadBeanDefinitons(location, actualResources);
		}catch(BeanDefinitionStoreException e){
			// ...error log
		}
	}
	else{
		try{
			int importCount;
			Resource relateiveResouce = getReaderContext().getResource().createRelative(location);
			if(relativeResource.exist()){
				importCount = getReaderContext().getReader().loadBeanDefinitons(relativeResource);
				actualResources.add(relativeResource);
			}
			else{
				importCount = getReaderContext().getReader().loadBeanDefinition(StringUtils.applyRelativePath(BaseLocation, loaction), actualResources);
			}
		}catch(Exception e){
			// ...error log
		}
	}
	Resource []actResArray = acutalResources.toArray(New Resource[actualResources.size()]);
}

protected void processAliasRegistration(Element ele){
	String name = ele.getAttribute(NAME_ATTRIBUTE);
	Stirng alias = ele.getAttribute(ALIAS_ATTRIBUTE);
	boolean valid = true;
	if(!StringUtils.hasText(name)){
		getReaderContext().error("Name must not be empty", ele);
		valid = false;
	}
	if(!StringUtils.hasText(alias)){
		getReaderContext().error("Alias must be be empty", ele);
		valid = false;
	}
	if(valid){
			getReaderContext.Registry().registerAlias(name, alias);
	}
	getReaderContext().fireAliasRegistered(name, alias, extractSource(ele));
}

protected void processBeanDefinition(Element ele, BeanDefinitionParseDelegate delegate){
	BaenDefinitionHolder bholder = delegate.parseBenDefinitionELement(ele);
	if(bHoder != null){
		bHolder = delegate.decorateBenDefinitionIfRequired(ele, bHolder);
		BeanDefinitionReaderUtils.registerBeanDefiniton(bHodler, getReaderContext().getRegistry());
	}
	getRaderContext().fireComponentRegistered(new BeanComponentDefinition(bHolder));
}
```

#### BeanDefinitionParseDelegate解析Bean定义资源文件中的\<Bean>元素：

```java
public BeanDefinitionHolder parseBenDefinitionElement(Element ele){
	return parseBeanDeinitionElement(ele, null);
}

public BeanDeinitionHolder ParseBenDefinitionElement(Element ele, BeanDefiniton containing Bean){
	String id = ele.getAttribute(ID_ATTRIBUTE);
	String nameAttr = ele.getAttribute(Name_ATTRIBUTE);
	List<String> alases = new ArrayList<>();
	if(StringUtils.hasLength(nameAttr)){
		String []nameArr = String.tokennizToStringArray(NameAttr, BEAN_NAME_DELIMITERS);
		aliases.addAll(Arrays.asList(nameArr));
	}	
	
	String beanName = id;
	if(!String.Utils.hasText(beanName) && !aliases.isEmpty()){
		beanName = aliases.remove(0);
	}
	if(containingBean == null){
		checkNameUniqueness(beanName, aliases, ele);
	}
	AbstractBeanDeinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
	if(beanDefinition != null){
		if(!StrigUtils.hasText(beanName)){
			try{
				if(beanName != null){
					beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefiniton, this.readerContext.Registry(), true);
				}
				else{
					beanName = this.readerContext.generateBeanName(beanDefiniton);
					String beanClassName = beanDefinition.getBeanClassName();
					if(beanClassName != null && beanName.startsWith(beanClassName) && beanName.length() > beanClassName.length() && !this.readerContext.getRegistry().isBeanNameInUse(beanClasName)){
						aliases.add(beanClassName);
					}
				}
			}catch(exception e){
				// ..erro log
				return null;
			}
		}
		String []aliasesArray = StringUtils.toStringArray(aliases);
		return new BeanDeinitionHolder(beanDefinition, beanName, aliasesArray);
	}
	
	return null;
}

public AbstractBeanDeinition parseBeanDefinitionElement(Element ele, String beanName, BeanDefinton containingBean){
	this.parseState.push(new BeanEntry(beanName));
	String calssName = null;
	if(ele.hasAttribute(CLASS_ATTRIBUTE)){
		className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
	}
	try{
		String parent = null;
		if(ele.hasAttribute(PARENT_ATTRIBUE)){
			parent = ele.getAttribute(PARENT_ATTRIBUE);
		}
		AbstractBeanDEfinition bd = createBeanDefiniton(className, parent);
		parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
		parseMetaElements(ele, bd);
		parseLookupOverrideSubElements(ele, bd.getMthodOverrides());
		parseReplacedMethodSubElements(ele, bd.getMethodOverrides());
		parseConstructorArgElements(ele, bd);
		parsePropertyElements(ele, bd);
		parseQualifierElements(ele, bd);
		bd.setResource(this.readerContext.getResource());
		bd.setResource(extractSource(ele));
		return bd;
	}catch(Exception e){
		// ...error log
	}finally{
		this.parseState.pop();
	}
	return null;
}	
```

#### 解析property

```java
public void parsePropertyElements(Element beanEle, BeanDefinition bd){
	NodeList nl = beanEle.getChildNodes();
	for(int i = 0; i < nl.getLength(); ++i){
		Node node = nl.item(i);
		if(isCandidateElement(node) && nodeNameEquals(node, PROPERTY_ELEMENT)){
			parsePropertyELement((Element)node, bd);
		}
	}
}

public void parsePropertyElement(Element ele, BenDefinition bd){
	String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
	if(!StrigUtils.hasLength(propertyName)){
		// ...error log
		return;
	}
	this.parseState.push(new PropertyEntry(propertyName));
	try{
		if(bd.getPropertyValues.contains(propertyName)){
			// ...error log
			return;
		}
		Object val = parsePropertyValue(ele, bd, propertyName);
		PropertyValue pv = new PropertyValue(propertyName, val);
		parseMetaElements(ele, pv);
		pv.setSource(extractSource(ele));
		bd.getPropertyValues().addProperty(pv);
	}finally{
		this.parseStat.pop();
	}
}

public Object parsePropertyValue(Element ele, BeanDefiniton bd, String propertyName){
	String elementName = (propertyName != null) ? "<property> element for property '" + propertyName + "'" : "<constructor-arg> element";
	NodeLsit nl = ele.getChildNodes();
	Element subElement = null;
	for(int i = 0; i < nl.getLength(); ++i){
		Node node = nl.item(i);
		if(node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT) && !nodeNameEquals(node, META_ELEMENT)){
			if(subElement != null){
				error(elementName + "must not contain more than one sub-element", ele);
			}
			else{
				subElement = (Element) node;
			}	
		}
	}
	boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
	boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
	if((hasRefAttribute && hasValueAttribute) || ((hasRefAttribute || hasValueAttribute) && subElement != null)){
		error(elementName + " si only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
	}
	if(hasRefAttribute){
		String refName = ele.getAttribute(REF_ATTRIBUTE);
		if(!StringUtils.hasText(refName)){
			error(elementName + " contains empty 'ref' attribute", ele);
		}
		RuntimeBeanRefence ref = new RuntimeBeanReference(refName);
		ref.setSource(extractSource(ele));
		return ref;
	}
	else if(hasValueAttribue){
		TypeStringValue valueHolder = new TypeStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
		valueHolder.setSource(extractSource(ele));
		return valueHolder;
	}
	else if(subElement != null){
		return parsePropertySubElement(subElement, bd);
	}
	else{
		error(elementName + " must specify a ref or value", ele);
		return null;
	}
}
```

#### 解析 property 元素的子元素


```java
public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultValueType){
	if(!isDefaultNamespace(ele)){
		return parseNesteCustomElement(ele, bd);
	}
	else if(nodeNameEquals(ele, BEAN_ELEMENT)){
		BeanDefinitonHolder nestedBd = parseBeanDefinitonElement(ele, bd);
		if(nestedBd != null){
			nestedBd = decorateBeanDefinitonIfRequired(ele, nestedBd, bd);	
		}
		return nestedBd;
	}
	else if(nodeNameEquals(ele, REF_ELEMENT)){
		String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
		boolean toParent = false;
		if(!StringUtils.hasLength(refNaame)){
			refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
			toParent = true;
			if(!StringUtils.hasLength(refName)){
				error("'bean', 'local', or 'parent' is required for <ref> element", ele);
				return null;
			}
		}
		if(!StringUtils.hasText(refName)){
			error("<ref> element contains empty target attribute", ele);
			return null;
		}
		RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
		ref.setSource(extractSource(ref));
		return ref;	
	}
	else if(nodeEquals(ele, IDREF_ELEMENT)){
		return parseIdRefElement(ele);
	}
	else if(nodeNameEquals(ele, VALUE_ELEMENT)){
		return parseValueElement(ele, defautlValueType);
	}
	else if(nodeNameEquals(ele, NULL_ELEMENT)){
		TypeStringValue nullHolder = new TypeStringValue(null);
		nullHolder.setSource(extractSource(ele));
		return nullHolder;
	}
	else if(nodenameEquals(ele, ARRAY_ELEMENT)){
		return parseArrayElement(ele, bd);
	}
	else if(nodeNameEquals(ele, LIST_ELEMENT)){
		return parseListElement(ele, bd);
	}
	else if(nodeNameEquals(ele, SET_ELEMENT)){
		return parseSetElement(ele, bd);
	}
	else if(nodeNameEquals(ele, MAP_ELEMENT)){
		return parseMapElement(ele, bd);
	}
	else if(nodeNameEquals(ele, PROPS_ELEMENT)){
		return parsePropsElement(ele);
	}
	else{
		error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
		return null;
	}
}
```

#### 解析 <list> 子元素

```java
public List parseListElement(Element collectionEle, Beandefiniton bd){
	String defaultElementType = collectionEle.getAttribue(VALUE_TYPE_ATTRIBUE);
	NodeList nl = collectionEle.getChildNodes();
	ManageList<Object> target = new ManagedList<Object>(nl.getLength());
	target.setSource(extractSource(collectionEle));
	target.setElementTypeName(defaultElementType);
	target.setMergeEnabled(parseMergeAttribute(collectionEle));
	parseCollectionElement(nl, target, bd, defaultElementType);
	return target;
}

protected vod parseCollectionElements(NodeList nl, Collection<Objct> target, BeanDefiniton bd, String defaultElementType){
	for(int i = 0; i < elementNodes.getLength(); ++i){
		Node node = elementNodes.item(i);
		if(node instacneof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)){
			target.add(parsePropertySubElement((Element)node, bd, defaultElementType));
		}
	}
}

```

####  注册

```java
public static void registerBeanDefiniton(BeanDefninitonHodler definitionHolder, BeanDefinitonRegistry registry) throws BeanDefinitonStroeException{
	String beanName = definitonHolder.getBeanName();
	registry.registerBeanDeifnition(beanName, defninitionHodler.getBeanDefinition());
	String []aliases = definitionHolder.getAliases();
	if(aliases != null){
		for(String aliase: aliases){
			registry.registerAlias(beanName, aliase);
		}
	}
}
```


```java
private Map<String, BeanDefiniton> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefiniton>();
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) throws BeanDefinitionException{
	Assert.hasText(beanName, "Bean name must not be empty");
	Assert.notNull(beanDefiniton, "BeanDefiniton must be not be null");
	if(beanDefinition instanceof AbstractBeanDefiniton){
		try{
			((AbstractBeanDefinition)beanDefinition).validate();
		}catch(BeanDefinitionValidateException e){
			throw new BeanDefinitionStroeException(beanDefiniton.getResourceDescription(), beanName, "Validation of bean definition faild", e);
		}
	}
	synchronized(this.beanDefinitionMap){
		Object oldBeanDefiniton = this.beanDefinitionMap.get(beanName);
		if(oldBeanDefiniton != null){
			if(!this.allowBeanDefinitionOverriding){
				thorow new BeanDefinitionStroeException(beanDefiniton.getResourceDescription(), beanName, "Can not register bean definiton [" + beanDefiniton + "] for beam '" + beanName + "': There is already [" + oldBeanDefinition + "] bound.");
			}
		}	
		else {
			this.beanDefinitionName.add(beanName);
			thi.forzenBeanDefinitonNames = null;
		}
		this.beanDefinitionMap.put(beanName, beanDefinition);
		resetBeanDefiniton(beanName);
	}
}
```