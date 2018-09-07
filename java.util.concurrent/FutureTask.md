# FutureTask

FutureTask实现了RunnableFuture接口，而RunnbaleFuture实现了于Runnbale和Future。因此：FutureTask可以交给Excutor执行，也可以由调用线程直接执行futureTask.run()。


```java
public interface RunnableFuture<V> extends Runnbale, Future<V>{
	void run();
}
```

```java

public class FutureTask implements RunnbaleFuture<V> {
	
}
```

根据futureTask.run()启动的时机，futureTask可以处于以下3种状态:

- 未启动：即futureTask.run()方法还没有被执行之前。
- 已启动：即futureTask.run()方法正在被执行过程中。
- 已完成：即futureTask.run()方法处于已完成状态，可能出于以下的原因
	- 正常执行完毕
	- 被取消futureTask.cancel()
	- 抛出异常

## get() 和 cancel()

![](http://owj98yrme.bkt.clouddn.com/111111.png)

## FutureTask源码

```java
public class FutrueTask<V> implements RunnableFuture<V> {
	/**
	 * 任务运行状态，初始化状态为NEW
	 * 设置终止的状态只会在set、setException和cacel方法中
	 * During completion, state may take on transient values of COMPLETING (while outcome is being set) 
	 * or INTERRUPTING (only while interrupting the runner to satisfy a cancel(true)).
	 *  
	 *  可能的状态转变
	 *  NEW -> COMPLETING -> NORMAL
	 *  NEW -> COMPLETING -> EXCEPTIONAL
	 *  NEW -> CANCELLED
	 *  NEW -> INTERRUPTING -> INTERRUPTED
	 */
	private volatile int state;
	private static final int NEW 			= 0;
	private static final int COMPLETING 	= 1;
	private static final int NORMAL 		= 2;
	private static final int EXCEPTIONAL	= 3;
	private static final int CANCELLED		= 4;
	private static final int INTERRUPTING	= 5;
	private static final int INTERRUPTED 	= 6;
	
	private Callable<V> callable;
	private Object outcome;
	// 执行callable的线程 CAS操作
	private volatie Thread runner;
	// 无锁栈，存放线程
	private volatie WaitNode waiters;
	
	
	public void run(){
		// 判断FurtureTask必须为初始化状态
		// 并且将FutureTask的启动线程必须为当前的null，设置为当前先前
		// 这也表示了 FutrueTask只会被运行一次
		if(state != NEW || !UNSAFE.compareAndSwapObject(this, runnerOffset, null, Thread.currentThread())){
			return;
		}
		try{
			Callable<V> c = callable;
			if(c != null && state == NEW){
				V result;
				boolean ran;
				try{
					result = c.call();
					ran = true;
				}catch(Throwable ex){
					result = null;
					ran = false;
					setException(ex);
				}
				if(ran)
					set(result);
			}
		}finally{
			runner = null;
			int s = state;
			// state在工作线程结束工作之后
			// 必须进行判断，避免遗漏处理中断操作
			if(x >= INTERRUPTING)
				handlePossibleCancelationInterrupt(s);
		}
	}
	/**
	 *设置furtureTask的计算结果
	 * 除非已经被设置或者被取消
	 */
	protected void set(V v){
		if(UNSAFE.compareAndSwapInt(this, stateOffset, NEW, COMPLETING)){
			outcome = v;
			UNSAFE.putOrderInt(this, stateOffset, NORMAL);
			finishCompletetion();
		}
	}
	/**
	 * 移除并且通知所有等待FutrueTask的线程
	 */
	private void finishCompletion(){
		for(WaitNode q; (q = waiters) != null; ){
			if(UNSAFE.compareAndSwapObject(this, waiterOffset, q, null)){
				for( ; ; ){
					Thread t = q.thread;
					if(t != null){
						q.thread= null;
						LockSupport.unpark(t);
					}
					WaitNode next = q.next;
					if(next == null)
						break;
					q.next = null;
					q = next;
				}
				break;
			}
		}
		done();
		
		callable = null;
	}
	
	public V get() throws InterruptedException, ExecutionException {
		int s = state;
		if(s <= COMPLETING)
			s = awaitDone(false, 0L);
		return report(s);
	}
	
	/**
	 * Awaits completion or aborts on interrupt or timeout
	 * @param timed true if use timed waits
	 * @Param nanos time to wait if timed
	 * @Return state upon completion
	 */
	private awaitDone(boolean timed, long nanos) throws InterruptedException{
		final long deadline = timed ? System.nanoTime() + nanos : 0L;
		WaitNode q = null;
		boolean queued = false;
		for( ; ; ){
			// 线程响应中断，移除所有的等待节点
			if(Thread.interrupted){
				removeWaiter(q);
				throw new InterruptedException();
			}
			int s = state;
			if(s > COMPLETING){
				if(q != null)
					q.thread = null;
				return s;
			}
			else if(s == COMPLETING)	// 超时时间未到
				Thread.yield();
			else if(q == null)
				q = new WaiteNode();
			else if(!ququed)
				//入队操作, 并设置q为当前等待节点
				queued = UNSAFE.compareAndSwapObject(this, waiterOffset, q.next = waiters, q)
			else if(timed){
				nonos = deadline - System.nanoTime();
				if(nanos <= 0L){
					removeWaiter(q);
					return state;
				}
				LockSupport.parkNonos(this. nanos);
			}
			else{
				LockSupport.park(this);
			}
		}
	}

	private void removeWaiter(WaitNode node){
		if(node != null){
			node.thread = null;
			retry:
			for( ; ; ){
				// restart on removeWaiter race
				// 将q设置为null即退出
				for(WaitNode pred = null, q = waiters, s; q != null; q= s){
					s = q.next;
					if(q.thread != null){
						pred = q;
					}
					else if(pred != null){
						pred.next = s;
						// check for race
						if(pred.thread == null){
							continue retry;
						}
						else if(!UNSAFE.compareAndSwapObject(this, waiterOffset, q, s))
							continue;
					}	
				}
				break;
			}
		}
	}
	
	public boolean cancel(boolean mayInterruptIfRunning){
		if(!(state == NEW && UNSAFE.compareAndSwapInt(this, stateOffset, NEW, mayInterruptIfRuning ? INTERRUPTING : CANCELLED)))
			return false;
		try{
			if(mayInterruptIfRunning){
				try{
					Thread t = runner;
					if(t != null)
						t.interrupt();	
				}finally{
					UNSAFE.putOrderInt(this, stateOffset, INTRRUPTED)
				}
			}
		}finally{
			finishCompletion();
		}
	}
	
	static final class WaitNode{
		volatile Thread thread;
		volatile WaitNode next;
		WaitNode(){
			thread = Thread.currentThread();
		}
	} 
}
```
