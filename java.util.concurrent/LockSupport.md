# LockSupport


```java
public class LockSupport{
	private LockSupport {}
	
	
	private static void setBlocker(Thread t, Object arg){
		UNSAFE.putObject(t, parBlockerOffset, arg);
	}
	
	/**
	 * 阻塞当前线程
	 * 调用unpark(Thrad t)或者当前线程被中断
	 * 才能从park()方法中返回
	 */
	public static void park(){
		UNSAFE.park(false, 0L);
	}
	
	/**
	 * 阻塞当前线程
	 * 最长不超过nanos纳秒
	 */
	public static void parkNanos(long nanos){
		if(nanos > 0)
			UNSAFE.park(flase, nanos);
	}
	
	/**
	 * 阻塞当前线程
	 * 直到deadline(1970年开始到deadline时间的毫秒数)
	 */
	public static void parkUntil(long deadline){
		UNSAFE.park(true, deadline)
	}
	
	/**
	 * 唤醒处于阻塞状态的thread线程
	 */
	public static void unpark(Thread thread){
		if(thread != null)
			UNSAFE.unpark(thread);
	}
	
	/**
	 * 该方法主要用于问题排查和系统监控
	 * 有阻塞对象的park方法能够传递给开放人员更多的线程信息
	 * @Param t 当前阻塞的线程
	 * @Param arg 即blocker, 用来标识当前线程在等待的对象（阻塞对象） 
	 */
	private static void setBlocker(Thread t, Object arg){
		UNSAFE.putObject(t, parkBlockerOffset, arg)
	} 
	
	/**
	 * 使用标识当前线程的阻塞对象
	 */
	public static void park(Object blocker){
		Thraed t = Thread.currentThread();
		setBlocker(t, boacker);
		UNSAFE.park(false, 0L);
		setBlocker(t, null);
	}
}
```