# Lock实现机制

## LOCK接口

使用synchronized关键字会隐式获取锁，但是它将锁获取和释放固化了，也就是先获取再释放。而并发包中的Lock接口显示地获取和释放锁的可操作性、可中断的获取锁以及超时获取锁等多种synchronized关键字不具备的同步特性。

```
	Lock lock = new ReentrantLock();
	lock.lock();
	try{
		//lock.lock()不要放在tyr语句块
	}finally{
		lock.unlock();	//锁释放要放在finally语句中
	}
```


实现Lock接口的锁提供Synchronized不具备的特性

- 非阻塞地获取锁
 	- 尝试获取锁
	- 能被中断地获取锁
	- 超时获取锁
- 提供公平锁机制
- 提供多个Condition条件

Lock接口的API

方法名称|描述
 :---: | :---: 
void lock()|阻塞获取锁
void locakInterruptibly()|阻塞获取锁，可以响应中断
boolean tryLock()|非阻塞获取锁，立马返回，获取成功返回true
boolean tryLock(long, TimeUnit) throws InterruptedException |超时获取锁，可以响应中断
Condition newCondition()|获取等待通知组件

Lock接口的实现（比如ReentrantLock），基本通过聚合了一个同步器（AbstractQueueSynchronizer）的子类来完成线程的访问控制。



## AbstractQueueSynchronizer

队列同步器（简称AQS）：

- 使用一个int变量表示同步状态
- 通过内置的FIFO队列来完成线程同步的排队工作

AQS是抽象类，子类通常继承AQS并且实现它的同步方法来管理同步状态。

同步器的实现是基于模板方法模式的，即使用者需要继承同步器并且重写同步器指定的方法。重写同步器指定的方法时，需要使用同步器提供的如下3个方法来访问或修改同步状态：

- getState(): 获取当前同步状态
- setState(): 设置当前同步状态
- compareAndSetState(int expect, int update): 使用CAS设置当前状态


队列同步器的实现分析，包括以下内容：

- 同步队列
- 独占式同步状态的获取与释放
- 共享式同步状态的获取与释放
- 超时获取同步状态



### 同步队列

如下图所示，同步器拥有首节点（head）和尾节点（tail），没有成功获取同步状态的线程将会成为节点加入该队列的尾部。加入队列的过程必须保证是线程安全的，同步器提供一个基于CAS的设置尾节点的方法：compareAndSetTail(Node expect, Node update)。

![](http://p5s0bbd0l.bkt.clouddn.com/aqs1.png)


同步队列遵循FIFO，首节点是获取同步状态成功的节点，首节点的线程在释放同步状态时，将会唤醒后继节点，而后继节点将会在获取同步状态的成功之后，将自己设置为首节点。由于只有一个线程能够获取到同步状态，因此设置首节点的方法并不需要使用CAS来保证。


### 独占式同步状态的获取与释放

首先调用自定义的tryAcquire，如果同步状态获取失败，则构造同步节点，加入队列的尾部，最后调用acquireQueued方法，使得该节点以“死循环”的方式获取同步状态。

```java
 piublic final void acquire(int arg){
 	//该方法对中断不敏感
 	//线程获取同步状态失败后，进入同步队列中，后续线程进行中断操作，线程不会移除同步队列
 	if(!tryAcqure(arg) && acquireQueued(addWaiter(Node.EXCLUSIVE), arg)){
 		selefInterrupt();
 	}
 }
 
private Node addWaiter(Node mode){
	Node node = new Node(Thread.currentThread(), mode);
	// 快速尝试在队列尾部添加新节点
	// 这里有可能会CAS失败，需要enq重新入队
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

// 保证节点能够线程安全地入队
// 同步器通过“死循环”来保证节点的正确添加
private Node enq(final Node node){
	for( ; ; ){
		Node t = tail;
		//队列同步器的head和tail还没初始化
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

// 节点进入同步队列之后，每个节点（即线程）都在自我观察
// 当条件满足之后，获取到了同步状态，就可以从自旋过程中退出
final boolean acquireQueued(final Node node, int arg){
	boolean failed = ture;
	try{
		boolean interrupted = false;
		// 只有前驱节点是头节点才能够尝试获取同步状态
		for( ; ; ){
			final Node p = node.predecessor();
			if(p == head && tryAcquire(arg)){
				setHead(node);
				p.next = null;	// help GC
				fialed = false;
				return interrupted;
			}
			// 如果有中断发生，记录下来，但是不响应中断
			if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
				interrupted = true;
			}
		}
	}finally{
		// 不是获取同步状态之后结束的
		if(failed){
			cancelAcquire(node);
		}
	}
}
```

![](http://p5s0bbd0l.bkt.clouddn.com/aqs2.png)

当同步状态获取成功之后，当前线程从acquire(int arg)返回，代表着当前线程获取了锁。

当前线程执行逻辑代码之后，需要释放同步状态，使得后续节点能够继续获取同步状态。


```java
// 在释放同步状态之后，会唤醒后继节点，进而使后继节点重新尝试获取同步状态
public final boolean release(int arg){
	if(tryRealse(arg)){
		Node h = head;	// 同步器的head指针
		if(h != null && h.waitStatus != 0){
			//unpark方法用于唤醒处于等待状态的线程
			unparkSuccessor(h);
		}
		return true;
	}
	return false;
}
```


#### 小结： 

在获取同步状态时，同步器维护一个同步队列，获取状态失败的线程都会加入到队列之中，并且进行自旋；移出队列（或者停止自旋）的条件是前驱节点为头节点，并且成功获取了同步状态。在释放同步状态时，同步器调用tryRelease方法释放同步状态，然后唤醒头节点的后继节点。



### 共享式同步状态的获取与释放

共享式与独占式最主要的区别是同一个时刻是否有多个线程可以同时获取到同步状态。如读写文件，允许多个线程读文件，只允许一个线程独占文件进行写。

共享式释放同步状态与独占式释放同步状态的主要区别在于，必须通过CAS来保证安全释放同步状态，因为同一时刻有多个线程在释放资源。

![](http://p5s0bbd0l.bkt.clouddn.com/aqs4.png)


```java
public final void acquireShared(int arg){
	// 当返回值大于等于0时，表示能够获取到同步状态
	if(tryAcqureShared(arg) < 0){
		doAcqureShared(arg);
	}
}

private void doAcqureShared(int arg){
	final Node node = addWaiter(Node.SHARED);
	boolean failed = true;
	try{
		boolean interrupted = false;
		// 与独占式获取同步状态一样，队列中节点不断自旋
		// 如果当前节点的前驱节点为头节点，尝试获取同步状态
		for( ; ; ){
			final Node p = node.predecessor();
			if(p == head){
				int r = tryAcquireShared(arg);
				if(r >= 0){
					setHeadAndPropagate(node, r);
					p.next = null;
					if(interrupted){
						selfInterrupt();
					}
					failed = false;
					return;
				}
			}
			if(shouldParkAfterFailedAcquire(p, node) && parkAndCheckInterrupt()){
				interrupted = true;
			}
		}
	}finally{
		if(failed){
			cancelAcquire(node);
		}
	}	
}

public final boolean releaseShared(int arg){
	if(tryReleaseShared(arg)){
		doRealeaseShared();
		return true;
	}
	return false;
}
```

共享式释放同步状态的操作会同时来自多个线程，需要通过循环和CAS操作来保证线程安全。


### 独占式超时获取同步状态

```java
private boolean doAcquireNanos(int arg, long nanosTimeout) throws InterruptedException{
	long lastTime = System.nanoTime();
	final Node node = addWaiter(Node.EXCLUSIVE);
	boolean failed = true;
	try{
		for( ; ; ){
			final Node p = node.predecessor();
			if(p == head && tryAcquire(arg)){
				setHead(node);
				p.next = null;
				failed = fasle;
				return true;
			}
			if(nanosTimeout <= 0){
				return false;
			}
			// 如果nanosTimeout时间小于spinForTimeout（1000ns）
			// 那么不会使线程超时等待，而是进入快速的自旋过程
			if(shouldParkAfterFailedAcquire(p, node) && nanosTimeout > spinForTimeoutThreshold){
				LockSupport.parkNanos(this, nanosTimeout);
			}
			long now = System.nanoTime();
			nanosTimeout -= now - lastTime;
			lastTime = now;
			// 超时获取同步状态 响应中断
			if(Thread.interrupted()){
				throw new InterruptedException();
			}
		}
	}finally{
		if(failed){
			cancelAcquire(node);
		}
	}
}
```

独占式超时获取同步状态和独占式获取同步状态在流程上十分相似，主要区别在于未获取到同步状态时的逻辑处理。acquire方法在未获取到同步状态时，将会使当前线程一直处于等待状态，而doAcquireNanos方法会使当前线程等待nanos纳秒，如果当前线程在nanosTimeout纳秒内没有获取到同步状态，将会自从等待逻辑中自动返回。