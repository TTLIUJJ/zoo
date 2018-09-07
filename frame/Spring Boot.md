# Spring Boot


SpringBoot的启动过程，主要使用了SpringBootApplication注解和SpringApplicaton.run()来运行我们的应用程序。

```java
@SpringBootApplication
public class CelticsApplication {
	public static void main(String []args) {
		SpringApplication.run(CelticsApplication.class, args);
	}
}
```


## SpringBootApplication 注解

基于 SpringBoot 1.3.5 版本

```java
@Target({ElementType.Type})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Configuration
@EnableAutoConfiguration
@ComponentScan
public @interface SpringBootApplication {
	Class<?>[] exclude() default {};
	String[] excluedName() default {};
	
	@AliasFor(
		annotation = ComponentScan.class,
		attribute = "basePackages"
	)
	String[] scanBasePackages() default {};
	
	@AliasFor(
		annotation = ComponentScan.class,
		attribute = "basePackageClasses"
	)
	Class<?>[] scanBasePackageClasses() default {};
}
```

可以看到，SpringBootApplication又有三个重要的注解类

- Configuration: 表示这是一个JavaConfig配置类，可以在这个类中自定义Bean及其依赖关系

- EnableAutoConfiguration：借助@import的帮助，将所有符合自动配置条件的bean定义加载到IOC容器（建议将其放在项目根目录下，这样可以扫描子包和类）

- ComponentScan：Spring的自动扫描注解，可定义扫描范围，加载到IOC容器



### EnableAutoConfiguration 注解

```java
@Target({ElementType.Type})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@AutoConfigurationPackage
@import({EnableAutoConfigurationImportSelector.class})
public @interfac EnableAutoConfiguration {
	Class<?>[] exclude() default {};
	String[] excludeName() default {};
}
```


```java
public class EnableAutoConfigurationImportSelector
	implements DeferredImportSelector, BeanClassloaderAwar,
				ResourceLoaderAware, BeanFactoryAware, 
				EnvironmentAware {
	
	// ...
	public String[] selectImports(AnnotationMetadata metadata) {
		try {
			/** 获取注解属性 */
			AnnotationAttributes attributes = this.getAttributes(metadata);
			/** 获取自动配置类 */
			List<String> configurations = this.getCandidateConfigurations(metadata, attributes);
			/** 移除重复的自动配置类 */
			configurations = this.removeDuplicates(configurations);
			/** 移除需要排除的自动配置类 */
			configurations.removeAll(exclusions);
			configurations = this.sort(configurations);
			this.recordWithConditionEvaluationReport(configuration, exclusions);
			return (String [])configurations.toArray(new String[configurations.size()]);	
		} catch (IOException var5) {
			throw new IllegalStateExcepton(var5);
		}
	}				
}
```