# ConcurrentHashMap

**以下是JDK 1.7版本**

ConcurrentHashMap是由Segment数组结构和HashEntry数组结构组成的。

- Segment数组基于ReentrantLock实现的可重入锁

- HashEntry用于存储键值对数据


![](http://owj98yrme.bkt.clouddn.com/28c8edde3d61a0411511d3b1866f0636.png)


Segment数组的结构和HashMap相似，是数组+链表结构的元素，当对HashEntry数组中的元素进行修改时，必须首先获得它对应的Segment锁。


## ConcurrentHashMap 初始化

使用initialCapacity、loadFactor和concurrencyLevel参数来初始化ConcurrentHashMap

```java
public ConcurrentHashMap(
	int initialCapacity, 
	float loadFactor, 
	int concurrencyLevel){
	 	...
	 	// concurrencyLevel的MAX_SEGMENTS最大值为65535，
	 	// 这意味着segments的数组长度最大值为2的16次方
	 	if(concurrencyLevel > MAX_SEGMENTS)
	 		concurrencyLevel = MAX_SEGMENTS
	 	
	 	int sshift = 0;
	 	int ssize = 1;
	 	while(ssize < concurrencyLevel){
	 		++sshift;
	 		ssize <<= 1;
	 	}
	 	segmentShift = 32 - sshift;
	 	segmentMask = ssize - 1;
	 	this.segments = Segment.newArray(ssize);
	 	
	 	if(initialCapacity > MAXIMUM_CAPACITY)
	 		initialCapacity = MAXIMUM_CAPACITY
	 	int c = initialCapacity / ssize;
	 	if(c * ssize < initialCapacity)
	 		++c;
	 	int cap = 1;
	 	while(cap < c)
	 		cap <<= 1;
	 	for(int i = 0; i < this.segments.length; ++i)
	 		this.segments[i] = new Segment<K, V>(cap, loadFactory);
	 		
	 	...
	}
```

- ssize 即segemnt数组的大小，大小为2的n次方，默认为16

- segmentShift 段偏移大小，用于定位参与散列运算的位数

- segmentMask 散列运算的掩码

- cap 即segement里HashEntry数组的长度，它等于initialCapacity除以ssize的倍数c，cap的值为2的n次方（至少为1）。

- loadFactor 扩容的阈值threshold = (int)cap*loadFactor，默认值为0.75

注意：initialCapacity是整个ConcurrentHashMap的大小，而cap是segment中HashEntry的大小。默认情况下initialCapacity的大小为16，loadFactor为0.75，通过运算可得cap大小为1，threshold大小为0。


## 定位 Segment

ConcurrentHashMap使用hash算法对元素的hashCode进行一次再散列，能够让hash数字值的每一位都参加到散列运算当中。

```java
private static int hash(int h){
	h += (h << 15) ^ 0xffffcd7d;
	h ^= (h >>> 10);
	h += (h << 3);
	h ^= (h >>> 6);
	h += (h << 2) + (h << 14);
	return h ^ (h >>> 16);
}
```
在散列的目的：减少散列冲突，为了更均匀的将元素分布在不同的segment数组内，提高容器的并发效率。


```java
//  一个散列很差的例子
//  ssize的大小为16,sshiftMask为15
    System.out.println(Interger.ParseInt("00001111", 2) & 15);
    System.out.println(Interger.ParseInt("00011111", 2) & 15);
    System.out.println(Interger.ParseInt("00111111", 2) & 15);
    System.out.println(Interger.ParseInt("01111111", 2) & 15);
```

计算后输出的值全部为15，即所有的元素定位到同一个Segment中，经过再散列运算，可以避免这种情况。

```java
final Segment<K, V> segmentFor(int hash){
	// 段偏移和段掩码定位segment
	return segments[(hash >>> segmentShift) & segmentMask];
}

// 定位HashEntry的hash算法
// int index = hash & (tab.length - 1);
```


## 基础操作

这里介绍ConcurrentHashMap的三种基本操作

- get
- put
- size

#### get操作

HashTable的每个get操作都是上锁的，而ConcurrentHashMap整个get不需要加锁，除非读到的值是空才会加锁重读（volatile使缓存失效，或者是值确实是空）。


```java
transient volatile int count;
volatie V value;

public V get(Object key){
	int hash = hash(key.hashCode());
	return segmentFor(hash).get(key, hash);
}
```

不加锁原因在于，get方法里将要使用的共享变量都定义为volatile类型，如用于统计当前Segment大小的count字段和用于存储值的HashEntry的value。

#### put操作

进行put操作，需要进行加锁。Segment是继承于ReentrantLock的类。插入操作分为两步：

- 插入前判断是否需要扩容：比较当前定位到的Segment中的HashEntry数组大小是否超过threshold，如果超过才进行扩容。HsahMap的插入策略是插入之后再进行判断，有可能扩容之后却不再使用，浪费空间。

- 如何扩容：首先会创建一个容量为原来两倍大的HashEntry数组，然后将原数组里的元素进行再散列之后插入到新数组。注意，为了高效，这里只是某个Segement进行扩容。


#### size操作

预选方案：

1. 统计每个segment元素中的count大小，但是count可能发生变化，造成脏读。

2. 在使用size方法时，把所有的Segment的put、remove和clean方法锁住，效率最低。

3. 使用modCount变量，在put、remove和clean方法里操作元素前都会将其加1，尝试2次通过不加锁的方式累加count，如果modCount值不变，返回统计结果，反之，使用方案1。


--

**以下为JDK 1.8版本的改动**

在JDK 1.8之后，ConcurrentHashMap摒弃了Segment数组的概念，直接使用Node数组+Synchronized+CAS来实现线程安全。



![](http://owj98yrme.bkt.clouddn.com/665f644e43731ff9db3d341da5c827e1.png)


## 构造函数

```java
public ConcurrentHashMap(int initialCapacity,
                         float loadFactor, 
                         int concurrencyLevel) {
    if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
        throw new IllegalArgumentException();
    // Map的容量大小肯定要大于等于Node数组个数
    if (initialCapacity < concurrencyLevel)   // Use at least as many bins
        initialCapacity = concurrencyLevel;   // as estimated threads
    // sizeCtl为table的大小
    long size = (long)(1.0 + (long)initialCapacity / loadFactor);
    int cap = (size >= (long)MAXIMUM_CAPACITY) ?
        MAXIMUM_CAPACITY : tableSizeFor((int)size);
    this.sizeCtl = cap;
}
```

## 初始化Node数组

只有在执行第一次put方法才会调用initTable方法初始化Node数组。

```java
private final Node<K, V>[] initTable(){
	Node<K, V>[] table;
	int sc;
	while((tab = table) == null || tab.length == 0){
		if((sc = sizeCtl) < 0){
			Thread.yield();
		}
		else if(U.compareAndSwapInt(this, SIZECTL, sc, -1)){
			try{
				if((tab = table) == null || tab.length == 0){
					int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
					@SuppressWarnings("unchecked")
					Node<K, V>[] nt = (Node<K, V>[])new Node<?, ?>[n];
					table = tab = nt;
					sc = n - (n >>> 2);
				}
			}finally{
				sizeCtl = sc;
			}
			break;
		}
	}
	return tab;
}
```