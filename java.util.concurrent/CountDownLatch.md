# CountDownLatch

闭锁的实现主要依赖队列同步器的实现，共享锁模式。



```java
public class CountDownLatch{
	private final Sync sync;
	
	public CountDownLatch(int count){
		if(count < 0)
			throw new IlleagalArgumentException("count < 0");
		this.sync = new Sync(count);
	}
	
	/**
	 * 等待state的状态为0
	 */
	public void await() throw InterruptedException{
		sync.acquireSharedInterruptilbly(1);
	}
	
	public boolean await(long timeout, TimeUnit unit) throw InterruptedException{
		return sync.tryAcquiredShareNanos(1, unit.toNanos(timeout));
	}
	
	/**
	 * 更新AQS中的state -= 1
	 */
	public void countDown(){
		sync.releaseShared(1);
	}
	
	public long getCount(){
		return sync.getCount();
	}
	
	
	
	private static final Class Sync extends AbstractQueuedSynchronizer {
		Sync(int count) { setState(count); }
		int getCount() { getState(); }
		protected int tryAcquireShared(int acquires) {
			return (getState() == 0) ? 1 : -1;
		}
		/**
		 * CAS操作更新共享锁目前的线程数
		 * 如果已经没有线程占有锁 返回true
		 */
		protected boolean tryReleaseShared(int releases) {
			for( ; ; ){
				int c = getState();
				if(c == 0)
					return false;
				int nextc = c - 1;
				if(compareAndSetState(c, nextc))
					return nextc == 0;
			}
		}
```

```java
public class AbstractQueueSychronizer
	extends AbstractOwnableSynchronizer 
	implements java.io.Serializable {
	
	static final Class Node {
		static final Node SHARED	= new Node();
		static final Ndoe EXCLUSIVE	= null;
		
		static final int CANCELLED	=  1;
		static final int SIGNAL		= -1;
		static final int CONDITION	= -2;
		static final int PROPAGATE	= -3; 
		
		/**
		 * 节点（即等待线程）的状态只能是以下之一：
		 *  CANCELLED： 节点可能由于超时或者打断被取消
		 *              一旦被设置为CANCELLED就不可以被改变
		 *              一个处于取消状态的线程不会再次更新状态
		 *  SIGNAL：    处于唤醒状态
		 *              只要前继节点释放锁 就会通知标识状态为SIGNAL的后继节点
		 *  CONDITION： 当前节点处于某个等待队列中
		 *              当其他线程调用了Condition.signal()之后
		 *              节点从等待队列转移到同步队列中
		 *  PROPAGATE： 与共享模式相关 
		 *              在共享模式中 处于该状态的线程处于可运行状态
		 *   0：        不是以上的任一状态
		 */
		volatile int waiteStatus; 
		volatile Node prev;
		volatile Node next;
		volatile Thread thread;
		
		/**
		 * 指向等待队列中的下一个节点，或者是代表一个特殊的值(SHARED)
		 * 注意：等待队列只有一种模式，即独占锁模式
		 * 等待队列中的节点通过re-acqure之类的方法重新加入同步队列
		 */
		Node nextWaiter;
		
		Node() {} // 用来初始化head节点，或者是SHARED模式下的标志
		Node(Thread thread, Node mode) {	// used by addWaiter
			this.nextWaiter = mode;
			this.thread = thread;
		}
		Node(Thread thread, int waitStatus) {	// used by Condition
			this.waiteStatus = waitStatus;
			this.thread = thread;
		}
	}
	
	/** AQS中的头节点 */
	private transient volatile Node head;	
	/** AQS中的尾节点 */
	private transietn volatile Node tail;
	
	/** 同步状态, 一个十分关键的变量 */
	private volatile int state;
	
	/**
	 * getState、setState和compareAndSwapInt
	 * 是AQS中读取和更新state状态的三个基本方法
	 */
	protected final int getState() { return state; }
	protected fianl void setState(int newState) { state = newState; }
	protected final boolean compareAndSetState(int expect, int update){
		return unsafe.compareAndSwapInt(this, stateOffset, expect, update);
	}
	
	/** 
	 * 快速自旋而不是使用time park之类的方法
	 * 有效的提高在非常短timeout中的反应
	 * 在超时等待获取锁的doAcquireNanos方法中可以看见
	 */
	static final long spinForTimeoutThreadhold = 1000L; // 单位：纳秒
	
	/**
	 * AQS中的head和tail属于懒加载模式
	 * 初始化AQS之后，head节点为空
	 * 当一次插入节点的时候，head节点（辅助节点）才会被初始化
	 * 而tail节点无论何时都是队列中的最后一个节点（有线程的实体节点）
	 */
	private Node enq(final Node node) {
		for(;;) {
			Node t = tail;
			if(t == null){
				if(compareAndSetHead(new Node())){
					tail = head;
				}
			}
			else{
				node.prev = t;
				if(compareAndSetTail(t, node)){
					t.next = node;
					return t;
				}
			}
		}
	}
	
	/** 
	 * 为当前线程创造并且加入AQS队列
	 * @Param mode: SHARED    共享模式
	 *              EXCLUSIVE 独占模式
	 */
	private Node addWaiter(Node mode) {
		Node node = new Node(Thread.currentThread(), mode);
		Node pred = tail;
		
		if(pred != null){
			node.prev = pred;
			if(compareAndSetTail(pred, node)){
				pred.next = node;
				return node;
			}	
		}
		enq(node);
		return node;
	}	
	
	private void setHead(Node node){
		head = node;
		node.thread = null;
		node.prev = null;
	}
	
	/** 唤醒node节点的后继节点 */
	private void unparkSuccessor(Node node) {
		int ws = node.waitStaus;
		if(ws < 0)
			compareAndSetWaitStatus(node, ws, 0);
			
		/*
		 * 大部分情况下，当前线程唤醒它的下一个节点
		 * 但如果下一个节点已经被取消了或者是null
		 * 就必须从队列尾端往前找，找到一个 non-cancelled successor 节点
		 * waitStatus   0    CANCELLED  0     0    0
		 *             node      s1     s2    s3  tail
		 * 如上所示，此时tail节点会被唤醒
		 * 可以发现，唤醒节点的顺序已经倒序了
		 */ 
		Node s = node.next;
		if(s == null || s.waitStatus > 0){
			s = null;
			for(Node t = tail, t != null && t != node; t = t.prev)
				if(t.waitStatus <= 0)
					s = t;
		}
		if(s != null)
			LockSupport.unpark(s.thread);	//唤醒node的后继节点
	}	
	
	/**
	 * 共享锁模式下释放锁的操作
	 * 通知后继者并且确保消息的传播
	 * 在独占锁模式下，只是通知第一个后继者
	 */
	private void doReleaseShared() {
		/*
		 * ？？？？
		 */
		for(;;){
			Node h = head;
			if(h != null && h != tail) {
				int ws = h.waitStatus;
				if (ws == Node.SIGNAL) {
					if(!compareAndSetWaitStatus(h, Node.SIGNAL, 0))
						continue;
					unparkSuccessor(h);
				}
				else if(ws == 0 && !compareAndSetWaitStatus(h, 0, Node.PROPAGATE))
					continue;
			}
			if(h == head)
				break;
		}
	}
	
	/**
	 * 设置node为AQS的head节点
	 * 检查node的后继节点是否处于共享模式下等待状态
	 */
	private void setHeadAndPropagate(Node node, int propagate) {
		Node h = head;
		setHead(node);
		
		if(propagate > 0 || h == null || h.waitStatus < 0 ||
		   (h = head) == null || h.waitStatus < 0) {
			Node s = node.next;
			if(s == null || s.isShared())
				doReleaseShared();
		}
	}

	
	
	
	
	
	
	
	
	
	
	/**
	 * 共享模式下的释放锁
	 * 在完成释放一个或者多个线程之后 返回true
	 */
	public final boolean tryAcquireSharedNanos(int arg){
		if(tryReleaseShared(arg)){
			doReleaseShared();
			return true;
		}
		return false;
	}
	
	/**
	 * tryReleaseShared方法需要自定义 
	 * 否则再调用的时候会抛出异常
	 * CountDownLatch实现了该方法
	 */ 
	protected boolean tryReleaseShared(int arg) {
		throw new UnsupportedOperationException();
	}
	

}
```