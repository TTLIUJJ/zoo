# CopyOnWirteArrayList

```java
public class CopyOnWirteArrayList<E> 
	implements List<E>, RandomAccess, Cloneable, java.io.Serializable{
		/** 在写数组时进行加锁保护 */
		final tranisient ReentrantLock lock = new ReentrantLock();
		/** 底层数组，只通过getArray/setArray方法获得 */
		private transient volatile Object []array;
		
		final Object []getArray() { return array; }
		final void setArray(Object []a) { array = a; }
		
		/**
		 * 在array数组末端添加一个元素
		 * 可以看出，如果频繁添加元素，每次都要进行加锁和内存拷贝
		 * 故，CopyOnWriteList只适合于读多写少的场景
		 */
		public boolean add(E e){
			final ReentrantLock lock = this.lock;
			lock.lock();
			try{
				Object [] elements = getArray();
				int len = elements.length;
				Object []new Elements = Arrays.copyOf(elements, len+1);
				newElements[len] = e;
				setArray(newElements);
				return true;
			}finally{
				lock.unlock();
			}
		}
		
		/**
		 * 涉及到修改array数组的操作，都需要进行上锁和拷贝新数组
		 */
		public E set(int index, E element){}
		public E remove(int index){}
	}
```