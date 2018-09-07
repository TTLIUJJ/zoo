# ArrayBlockingQueue

从源码中我们是可以分析ArrayBlockingQueue的工作原理：

- Object的数组：底层数据结构，该数组的作为生产者和消费者的队列

- takeIndex：消费者将会使用的数组元素下标

- putIndex：生产者将会使用的数组元素下标

- ReentrantLock：ArrayBlockingQueue线程安全的保障机制

- Condition变量：通知对方可进行一定的操作

- 构造函数的fair参数：ReentrantLock是否为公平锁

几个函数的区别：

- add：底层实现其实是offer操作，可能抛出两个异常
	
	- IllegalStateException：队列已满
	
	- NullPointerException：插入null元素

- put/take：阻塞调用，可响应中断

- offer/poll：非阻塞调用，分为立即返回和非立即返回

	- 立即返回true或者false
	
	- 非立即返回，等待超时之后返回false，并且可响应中断

	


```java
public class ArrayBlockingQueue 
	extends AbstractQueue<E> 
	implements BlockingQueue<E>, java.io.Serializable {
	
	/** ArrayBlockingQueue的底层实现，数组 */
	final Object []items;
	
	/** 下一个 take poll remove 元素的下标 */
	int takeIndex;
	
	/** 下一个 put offer add 元素的下标 */
	int putIndex;
	
	/** 队列中的元素个数 */
	int count;
	
	final ReentrantLock lock;
	private final Condiditon notEmpty;
	private final Condiditon notFull;
	
	/**
	 * 往当前的putIndex插入一个元素
	 * 并且发送队列未空的信息（前提是在获取lock的情况下）
	 */
	private void enqueue(E x) {
		// aseert lock.getHoldCount() == 1 && items[putIndex] == null
		final Object []items = this.items;
		items[putIndex] = x;
		if(++putIndex == items.length)
			putIndex = 0;
		count++;
		notEmpty.signal();
	}
	
	/**
	 * 往当前的takeIndex取出元素
	 * 并且发送队列未满的信息（前提是在获取lock的情况下）
	 */
	private E dequeue() {
		// assert lock.getHoldCount() == 1 && items[takeIndex] != null
		final Object []items = this.items;
		E x = (E)items[takeIndex];
		if(++takeIndex == items.length)
			takeIndex = 0;
		// itrs ...
		notFull.signal();
		return x;
	}
	
	/** 
	 * 删除removeIndex下标对应的元素
	 */
	void removeAt(final int removeIndex) {
		final Object[] items = this.items;
		if (removeIndex == takeIndex) {
			/** 删除的元素恰好是下一个takeIndex */
			items[takeIndex] = null;
			if(++takeIndex == items.length)
				takeIndex = 0;
			count--;
			// itrs ...
		}
		else {
			/** 所有元素节点往后挪动 直至遇到putIndex */
			final int putIndex = this.putIndex;
			for(int i = removeIndex; ; ) {
				int next = i + 1;
				if(next == items.length)
					next = 0;
				if(next != putIndex) {
					items[i] = items[next];
					i = next;
				}		
				else {
					items[i] = null;
					this.putIndex = i;
					break;
				}
			}
		}
		count--;
		// itrs ...
		notFull.signal();
	}
	
	
	public ArrayBlockingQueue(int capacity) {
		this(capacity, false);
	}
	
	/**
	 * @Param capacity 队列的容量，即数组items的数组大小
	 * @Param fair     是否为公平锁
	 */
	public ArrayBlockingQueue(int capacity, boolean fair) {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		this.items = new Obejct[capacity];
		lock = new ReentrantLock(fair);
		notEmpty = lock.newCondition();
		notFull  = lock.newCondidtion();
	}
	
	/** 
	 * @throws IlleagalStateException 当队列已满
	 * @throws NullPointerException   e为null
	 */ 
	public boolean add(E e) {
		return super.add(e);
	}
	
	/**
	 * 立刻返回
	 * @return 成功插入返回true， 否则返回false
	 * 与add不同的是，当队列已满并不会抛出异常
	 * 而是直接返回false
	 */
	public boolean offer(E e) {
		checkNotNull(e);
		final ReentrantLock lock = this.lock;
		lock.lock()
		try{
			if(count == items.length)
				return false;
			else {
				enqueue(e);
				return true;
			}
		} finally {
			lock.unlock();
		}
	}
	
	/** 
	 * 与offer不同的是，当队列已满不会直接返回
	 * 而是等待队列未满，并且可中断
	 */
	public void put(E e) thorows InterruptedException {
		checkNotNull(e);
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while(count == item.length)
				notFull.await();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * put方法的加强版
	 * 支持可超时地加入元素，并且可中断
	 */
	public boolean offer(E e, long timeout, TimeUnit unit) 
		throws InterruptedException {
		
		checkNotNull(e);
		long nanos = unit.toNanos(timeout);
		final ReentrantLock lock = this.lock();
		try {
			while (count == items.length) {
				if(nanos <= 0)
					return false;
				nanos = notFull.awaitNanos(nanos);
			}
			enqueue(e);
			return true;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 立刻返回
	 * 当队列未空时，直接返回null
	 */
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return (count == 0) ? null : deuque();
		} finally {
			lock.unlock();
		}
	}
	
	/** 
	 * 当队列为空时，阻塞等待
	 * 响应中断
	 */
	public E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try {
			while (count == 0) {
				notEmpty.await();
			return dequeue();
			}
		} finally {
			lock.unlock();
		}
	}
	/**
	 * 超时等待的取出元素
	 * 并且响应中断
	 */
	public E poll(long timeout, TimeUnit unit) throws InterruptedExcetion {
		long nanos = unit.toNanos(timeout);
		final RenentrantLock = this.lock();
		try {
			while (count == 0) {
				if(nanos <= 0)
					return null;
				nanos = notEmpty.awaitNanos(nanos);
			}
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * 下一个出队的元素
	 */
	public E peek() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return itemsAt(takeIndex);
		} finally {
			lock.unlock();
		}
	}	
	
	public int size() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return count;
		} finally {
			locl.unlock();
		}
	}
	
	public int remainingCapacity() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try {
			return items.length = count;
		} finally {
			lock.unlock();
		}
	}
	
	public boolean remove(Object o) {
		if(o == null)
			return false;
		final Object []items = this.items;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try{
			if (count > 0) {
				final int putIndex = this.putIndex;
				int i = takeIndex;
				do {
					if(o.equals.(items[i])) {
						removeAt(i);
						return true;
					}
					if(++i == items.length)
						i = 0;
				} while ( i != putIndex);
			}
		} finally {
			lock.unlock();
		}
	}
}
```