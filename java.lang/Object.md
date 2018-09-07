# Object



Object对象一共有12个方法，可以分为以下几类：

- 本地方法：由其他语言编写的，编译和处理器相关的机器代码，保存在动态链接库中，供其他java方法调用的接口。
	
	- registerNatives 注册Object中的其他本地方法，即将其映射到对应的C语言方法。该方法的私有方法，并且在对象新建时就会运行。

	- getClass 获取对象的Class类

	- clone 只有实现了Cloneable接口的类才能调用该函数，否则会抛出CloneNotSupportedException。默认的实现是浅拷贝，如果自定义对象需要深拷贝，需要重写clone方法
	
	- notify和wait与synchronized配合，可以完成等待通知机制
	
	- hashCode 默认实现为返回保存对象的地址，如果两个引用不是指向同一个地址，那么hashCode必定不相等。一般需要比较的对象，都会重写该方法。
	
- Java方法：直接编译成字节码，保存在Class文件中。
 
	- equals 默认实现为比较两个对象是否指向同一个地址，一般与hashCode配合使用，先比较两个对象的hashCode是否相等，如果是（因为存在hash碰撞），再比较其内部私有属性。如果不重写equals方法，两个指向不同地址的引用，必定不相等。
	
	- toString 打印对应的Class类名和地址

```java
package java.lang

public class Object {
	private static native void registerNatives();
	static {
		registerNatives();
	}
	
	public final native Class<?> getClass();
	public native int hashCode();
	public boolean equals(Object) {
		return (this == obj);
	}
	protected navtive clone() throws CloneNotSupportedException;
	public String toString() {
		return getClass().getName() + "@" + Integer.toHexString(hashCode());
	}	
	public final native void notify();
	public final native void notifyAll();
	public final native void wait(long timeout) throws InterruptedException;
	public final void wait(long timeout, int nanos) throws InterruptedException {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout value is negative");
		}
		if (nanos < 0 || nanos > 999999) {
			throw new IllegalArgumentException("nanosecond timeout value out of range");
		}
		if (nanos > 0) {
			timeout++;
		}
		wait(timeout);
	}
	public final void wait() throws InterruptedException {
		wait(0);
	}
	protected void finaliz() Throwable {}
}
```