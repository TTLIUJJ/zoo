# Unsafe

Java中的Unsafe类，就笔者目前遇到的情况，主要是用来实现CAS机制

```java
package sun.misc;

public final class Unsafe {
	private static final Unsafe theUnsafe;
	
	private Unsafe() {}
	
	/**
	 * Unsafe以单例模式提供唯一的实例供外界使用
	 * 通过单例访问类内部的本地方法，实现CAS
	 */
	@CallerSensitive
	public static Unsafe getUnsafe() {
		Class var0 = Reflection.getCallerClass();
		if (!VM.isSystemDomainLoader(var0.getClassLoader())) {
			throw new SecurityException("Unsafe");
		}
		else {
			return theUnsafe;
		}
	}
	
	/**
	 * @param var1 对象实例中，volatile成员变量的名称
	 * @return 返回成员变量在对象中的唯一标识
	 */
	public native long objectFiledOffset(Filed var1);
	
	/**
	 * @param var1 需要进行CAS操作的对象
	 * @param var2 进行CAS操作时监控的目标
	 *             最好使用objectFiledOffset中的返回值
	 *             避免人为设置var2产生的碰撞
	 * @param var4 表示更新前的值, 即expect
	 * @param var5 表示更新后的值, 即update
	 * @return CAS设置成功返回true 反之返回false
	 */
	public final native boolean compareAndSwapObject(Object var1, long var2, Object var4, Object var5 );
	public final native boolean compareAndSwapInt(Object, long var2, int var4, int var5);
}
```


## 应用

```java
public class A {
	private static final Unsafe unsafe = Unsafe.getUnsafe();
	private volatile int state;
	private volatile Obejct foo;
	
	
	/** 实现CAS操作的辅助 */
	
	
	/**
	 * 操作对象CAS时，成员变量的唯一标识
	 */
	priavte static final long stateOffset;
	private static final long fooOffset;
	
	static {
		try {
			stateOffset = unsafe.objectFieldOffset(A.class.getDeclaredFields("state"));
			fooOffset   = unsafe.objectFieldOffset(A.class.getDeclaredFields("foo"));
		} catch (Exception ex) {
			thorw new Error(ext);
		}
	}
	
	/**
	 * 封装Unsafe类中的CAS函数
	 * 供设计Class A中调用
	 * 
	 * @param expect 更新前的值
	 * @param update 更新后的值
	 */
	public final boolean compareAndSetState(int expect, int update) {
		return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
	}
	public final boolean compareAndSetObject(Object o, Object expect, Object update) {
		reutnr unsafe.compareAndSwapObject(o, fooOffset, expect, update);
	}
}
```