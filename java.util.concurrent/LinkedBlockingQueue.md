# LinkedBlockingQueue


```java
public class LinkedBlockingQueue<E>
	extends AbstractQueue<E>
	implements BlockingQueue<E>, java.io.Serializable {
	
	static class Node<E> {
		E item;
		/**
		 * next节点有三个含义：
		 * 1. 作为node的后继节点
		 * 2. 作为head的后继节点，那么next为队列的第一个节点
		 * 3. null，表示node作为队列的最后一个节点
		 */
		Node<E> next;
		Node(E x) { item = x; }
	}
	
	private final int capacity;
	private final AtomicInteger count = new AtomicInteger();	
	
	transient Node<E> head;
	private transient Node<E> last;
	private final ReentranLock takeLock = new ReentrantLock();
	private final Condition notEmpty = takeLock.newCondition();
	private final ReentrantLock putLock = new ReentrantLock();
	private final Condition notFull = putLock.newConditon();
	
	/**
	 * 
	 */
	private void signalNotEmpty() {
		final ReentrantLock takeLock = this.takeLock;
		takeLock.lock();
		try {
			notEmpty.signal();
		} finally {
			takeLock.unlock();
		}
	}
	
	privat void signalNotFull() {
		final ReentrantLock putLock = this.putLock;
		putLock.lock();
		try{
			notFull.signal();
		} finally {
			putLock.lock();
		}
	}
	
	private void enqueue(Node<E> node) {
		/**
		 * assert putLock.isHeldByCurrentThread 
		 * assert last.next == null
		 */
		last = last.next = node;
	}
	
	/**
	 * head -> node1 -> node2 -> node3
	 *   h     first  ==> after dequeue()
	 *          head -> node2 -> node3
	 */
	private E dequeue() {
		/**
		 * assert takeLock.isHeldByCurrentThread
		 * assert head.item == null
		 */
		Node<E> h = head;
		Node<E> first = h.next;
		h.next = h; // help GC
		head = first;
		E x = first.item;
		first.item = null;
		return x;
	}
	
	void fullyLock() {
		putLock.lock();
		takeLock.lock();
	}
	
	void fullyUnlock() {
		putLock.unlock();
		takeLock.unlock();
	}
	
	/** 
	 * 无界阻塞队列是由上界的
	 * 最大值为Integer.MAX_VALUE
	 */
	public LinkedBlockingQueue() { 
		this(Integer.MAX_VALUE); 
	}
	
	public LinkedBlockingQueue(int capacity) {
		if(capacity <= 0)
			throw new IllegalArgumentException();
		this.capacity = capacity;
		last = head = new Node<E>(null);
	}
	
	public int size() { return count.get(); }
	
	/**
	 * 等待队列非满的情况，往队尾插入元素，
	 * 等待可中断
	 */
	public void put(E e) throws InterruptedException {
		if(e == null)
			throw new NullPointerException();
			
		int c = -1;
		Node<E> node = new Node<E>(e);
		final ReentrantLock putLock = this.putLock;
		final AtomicInteger count = this.count;
		putLock.lockInterruptibly();
		try{
			
			while(count.get() == capacity)
				notFull.await();
			enqueue(node);
			c = count.getAndIncrement();
			if(c + 1 < capacity)
				notFull.signal();
		} finally {
			putLock.unlock();
		}
		if(c == 0)
			signalNotEmpty();
	}
}
```