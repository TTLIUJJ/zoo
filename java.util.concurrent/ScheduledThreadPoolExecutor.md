# ScheduledThreadPoolExecutor


ScheduledThreadPoolExecutor，他主要用来在给定的延迟之后运行任务，或者定期运行任务。ScheduledThreadPoolExecutor的功能与Timer类似，但是ScheduledThreadPoolExecutor可以在构造函数中指定多个对应的后台线程，Timer对应的是单个后台线程。


```java
public class ScheduledThreadPoolExecutor 
	extends ThreadPoolExecutor
	implements ScheduledExecutorService{
		/**
		 * 
		 */
		private Thread leader = null;
		
		/**
		 *
		 */
		private final Condition available = lock.newCondition();
				
		public RunnableSchduledFuture<?> take() throws Interruption{
			final ReentrantLock = this.lock();
			lock.lockInterruptly();
			try{
				for( ; ; ){
					RunnaleScheduledFuture<?> first = queue[0];
					if(first == null)
						avaiable.await();
					else{
						long delay = fist.getDelay(NANOSECONDS);
						if(delay <= 0)
							return finishPoll(first);
						first = null; // 等待期间不要持有任务
						if(leader != null)
							available.await();
						else{
							Thread thisThread = Thread.currentThread();
							leader = thisThread;
							try{
								available.awaitNanos(delay);
							}finally{
								if(leader == thisThread)
									leader = null;
							}
						}
							
					}
				}
			}finally{
				if(leader == null && queue[0] != null)
					available.singal();
				lock.unlock();
			}
		}
		
		private class SechduledFutureTask<V>
			extends FutureTask<V>
			implements RunnableScheduledFuture<V>{
			
			/** 该任务被添加到ScheduledThreadPoolExecutor中的序号 */
			private final long 	sequenceNumber;
			/** 该任务被执行的具体时间 */
			private long time;
			/** 
			 * 任务执行的间隔周期
			 *   - period > 0：表示固定的执行时间 
			 *   - period < 0：表示固定延迟时间
			 *   - period = 0: 非重复非延迟的模式 
			 */
			private final long period;					
			
			/**
			 * DelayQueue封装了一个PriorityQueue 
			 * 优先队列会对线程池中的任务进行排序
			 */
			public int compareTo(Delayed other){
				if(other == this)
					return 0;
				/** 更精确执行顺序的判断，依赖于加入pool的顺序 */
				if(other instanceof SechduledFutureTask){
					SechduledFutureTask<?> x = (ScheduledFutureTask<?>) other;
					long diff = time - x.time;
					if(diff < 0)
						return -1;
					else if(diff > 0)
						return 1;
					else if(sequenceNumber < x.sequenceNumber)
						reutnr -1;
					else
						return 1;
				}
				long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);
				return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
			}
			
			/** 返回执行任务的时间 */
			public long getDelay(TimeUnit unit){
				return unit.convert(time - now(), NANOSECONDS);	
			}
		}
}
```


## ScheduledThreadPoolExecutor运行机制

ScheduledThreadPoolExecutor的工作队列是DelayQueue，它是一个无界的队列，可能会造成内存积压太多请求，造成OOM。

执行主要分为两步：

- 当调用ScheduledThreadPoolExecutor的scheduleAtFixedRate()和scheduleWithFixedDelay()方法，会向ScheduledThreadPoolExecutor的DelayQueue添加一个ScheduledFutureTask任务。

- 线程池中的线程从DelayQueue获取ScheduledFutureTask，然后执行任务。


  