# HashMap


- modCount变量：与fast-fail机制有关。


 
```java
public class HashMap<K, V> extends AbstractMap<K, V>
	implements Map<K, V>, Cloneable, Seriablizable {
	
	/** 默认初始化容量大小为16， 并且要求HashMap容量为2的n次方 */
	static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;	
	/** 最大容量为2的30次方 */
	static final int MAXIMUM_CAPACITY = 1 << 30;
	
	/** 默认的负载因子 0.75 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;
	
	/** Hash冲突转化为链表的节点为8个 */
	static final int TREEIFY_THRESHOLD = 8;
	
	/** 红黑树退为链表结构要求的节点为6个 */
	static final int UNTREEIFY_THRESHOLD = 6;
	
	/**  ？？ */
	static final int MIN_TREEIFY_CAPACITY = 64;
	
	static class Node<K, V> 
		implements Map.Entry<K, V> {
		
		final int hash;
		final K key;
		V value;
		Node<K, V> next;
		Node(int hash, K key, V value, Node<K, V> next) {
			this.hash = hash;
			this.key = key;
			this.value = value;
			this.next = next;
		}
		public final K getKey() { return key; }
		public final V getValue() { return value; }
		public final String toString() { return key + "=" + value; }
		
		/** 该hashCode返回的是节点的Hash值 */
		public final int hashCode() { 
			return Objects.hashCode(key) ^ Objects.hashCode(value);
		}
		public final V setValue(V newValue) {
			V oldValue = value;
			value = newValue;
			return oldValue;
		}
		
		/**
		 * HashMap比较的底层实现
		 * 1. 同一个引用，毫无疑问返回true
		 * 2. Param(o)是一个Map.Entry对象
		 *    并且Node的k和v与入参o一致，才能返回true
		 */
		public final boolean equals(Object o) {
			if(o == this)
				return true;
			if(o instanceof Map.Entry) {
				Map.Entry<?, ?> e = (Map.Entry<?, ?>)o;
				if(Objects.equals(key, e.getKey()) &&
					Obejcts.equals(value, e.getValue()))
					return true;
			}
			return false;
		}
	}
	
	/**
	 * 计算key的hash值
	 * 若传入的key是null的话，它的hash值恒为0
	 */ 
	static final int hash(Object key) {
		int h;
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	}
	
	/**
	 * 扩容之后Hash表的大小
	 * 值为2的n次方，最大为2的30次方
	 */
	static final int tableSizeFor(int cap) {
		int n = cap - 1;
		n |= n >>> 1;
		n |= n >>> 2;
		n |= n >>> 4;
		n |= n >>> 8;
		n |= n >>> 16;
		return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
	}
	
	transient Node<K, V>[] table;
	transient Set<Map.Entry<K, V>> entrySet;
	transient int size;
	transient int modCount;
	
	publc V get(Object key) {
		Node<K, V> e;
		return (e = getNode(hash(key), key)) == null ? null : e.value;
	}
	
	final Node<K, V> getNode(int hash, Object key) {
		Node<K, V>[] tab;
		Node<K, V> first, e;
		int n;
		K k;
		if((tab = table) != null && (n = tab.length) > 0 &&
			(first = tab[(n-1) & hash]) != null) {
			if(first.hash == hash && 
				((k = first.key) == key || (key != null && key.equals(k)))) {
				return first;	
			}
			if((e = first.next) != null) {
				if(first instance TreeNode)
					return ((TreeNode<K, V>)first).getTreeNode(hash, key);
				do {
					if (e.hash == hash && 
						((k = e.key) == key || (key != null && key.equals(k))))
						return e;
				} while((e = e.next) != null);	
			}	
		}
		return null;
	}
	
	/**
	 * 往Map中添加一个键值对，如果key已经存在，替换旧value
	 * @return 返回key对应的旧value
	 *         如果之前key没有对应的value，返回null
	 */ 
	public V put(K key, V value) {
		return putVal(hash(key), key, value, false, true);
	}
	
	/**
	 * @param hash key对应的hash值
	 * @param key  key
	 * @param value value
	 * @param onlyIfAbsent 传入true，不更新旧值（旧值为null还是会更新）
	 * @param evict 传入false，表示hash表处于构造模式
	 * @return 旧的value，返回null如果不存在的话 
	 */
	public V putVal(int hash, K key, V value, boolean onlyIfAbsent, boolean evict) {
		Node<K, V>[] tab;
		Node<K, V> p;
		int n, i;
		if ((tab = table) == null || (n = tab.length) == 0)
			/** hash表还未初始化 */
			n = (tab = resize()).length;
		if((p = tab[i = (n-1) & hash]) == null)
			/** hash表中i下标的位置尚未有节点 */
			tab[i] = new Node(hash, key, value, null);
		else {
			/** 进入该分支，说明存在hash碰撞 */
			Node<K, V> e;
			K k;
			/** 判断第一个节点 */
			if(p.hash == hash &&
				((k = p.key) == key || (key != null && key.equals(k))))
				e = p;
			else if (p instance TreeNode) {
				e = ((TreeNode<K, V>)p).putTreeVal(this, tab, hash, key, value);
			}
			else {
				/**
				 * for循环判断是否达到链表转红黑树的条件
				 * e节点可以是新增或者更新的节点
				 */
				for(int binCount = 0; ; ++binCount) {
					if ((e = p.next) == null) {
						p.next = newNode(hash, key, value, null);
						if (binCount >= TREEIFY_THRESHOLD - 1) 	// -1 for 1st
							treeifyBin(tab, hash);
						break;
					}
					if(e.hash == hash && 
						((k = e.key) == key || (key != null && key.equals(k))))
						break;
					p = e;
				}
			}
			if (e != null) {
				V odlValue = e.value;
				if(!onlyIfAbsent || oldValue == null)
					e.value = value;
				afterNodeAccess(e);
				return oldValue;
			}	
		}
		++modCount;
		/** 插入元素之后再判断扩容，可能不是一个很好的选择 */
		if(++size > threshold)
			resize();
		afterNodeInsertion(evict);
		return null;	
	}
	
	/**
	 * 在初始化和扩容时会调用的resize
	 */
	final Node<K, V>[] resize() {
		Node<K, V> []oldTab = table;
		int oldCap = (oldTab == null) ? 0 : oldTab.length;
		int oldThr = threshold;
		int newCap = 0;
		int newThr = 0;
		if (oldCap > 0) {
			if (odlCap >= MAXIMUM_CAPACITY) {
				threshold = Integer.MAX_VALUE;
				return oldTab;
			}
			else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY 
						&& oldCap >= DEFAULT_INITIAL_CAPACITY)
				newThr = oldCap << 1;
		}
		else if (old > 0) {
			newCap = oldThr;
		}
		else {
			newCap = DEFAULT_INITIAL_CAPACITY;
			newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
		}
		if (newThr == 0) {
			float ft = (float)newCap * loadFactor;
			newThr = (newCap < MAXIMUM_CAPACITY &&
						ft < (float)MAXIMUM_CAPACITY ? (int)ft : Integer.MAX_VAULE);
		}
		threshold = newThr;
		Node<K, V>[] newTab = (Node<K, V>[])new Node[newCap];
		/** 并发场景下，在这里可能取到空value */
		table = newTab;
		if (oldTab != null) {
			for(int j = 0; j < oldCap; ++j) {
				Node<K, V> e;
				if((e = oldTab[j]) != null) {
					oldTab[j] = null
					if(e.next == null)
						newTab[e.hash & (newCap - 1)] = e;
					else if(e instanceof TreeNode)
						((TreeNode<K, V>)e).split(this, newTab, j, oldCap)
					else {
						Node<K, V> loHead = null, loTail = null;						Node<K, V> hiHead = null, hiTail = null;
						Node<K, V> next;
						do {
							next = e.next;
							if ((e.hash & oldCap) == 0) {
								if(loTail == null)
									loHead = e;
								else 
									loTail.next = e;
								loTail = e;
							}
							else {
								if(hiTail == null) 
									hiHead = e;
								else 
									hiTail.next = e;
								hiTail = e;
							}
						} while((e = next) != null);
						if(loTail != null) {
							loTail.next = null;
							newTab[j] = loHead;
						}
						if(hiTail = null) {
							hiTail.next = null;
							newTab[j + oldCap] = hiHead;
						}
					}	
				}
			}
		}
		return newTab;
	}
}
```


```java
public abstract class AbstractMap<K, V>
		implements Map<K, V> {
	
	/** 只有一个构造函数，并且只有继承类可以构造AbstractMap */
	protected AbstractMap() {
	}
	
	public int size() {
		return entrySet().size();
	}
	
	public boolean isEmpty() {
		return size() == 0;
	}
	
	/**
	 * 通过entrySet()实现的迭代器搜索entry
	 * 时间复杂度与map的大小呈线程关系
	 * 可以看出，AbstractMap是支持储存null的V
	 */
	public boolean containsValue(Object value) {
		Iterator<Entry<K, V>> i = entrySet().iterator();
		if(value == null) {
			while(i.hasNext()) {
				Entry<V, V> e = i.next();
				if(e.getValue() == null)
					return true;
			}
		}
		else {
			while(i.hasNext()) {
				Entry<K, V> e = i.next();
				if(value.equals(e.getValue()))
					return ture;
			}
		}
		return false;
	}
	
	/**
	 * 与containValue一样的搜索策略
	 * AbstractMap支持Entry的K, V为null
	 */
	public boolean containsKey(Object key) {
		Iterator<Map.Entry<K, V>> i = entrySet().iterator();
		if(key == null) {
			while(i.hasNext()) {
				Entry<K, V> e = i.next();
				if(e.getKey() == null)
					return true;
			}
		}
		else {
			while(i.hasNext()) {
				Entry<K, V> e = i.next();
				if(key.equals(e.getKey()))
					return true;
			}
		}
		return false;
	}
}
```