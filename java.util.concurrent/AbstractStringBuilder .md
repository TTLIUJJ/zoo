# AbstractStringBuilder 

StringBuilder和StringBuffer都是通过继承抽象类AbstractStringBuilder来实现。StringBudiler的实现都是封装AbstractBuilder的函数，同样的StringBufferu也是对AbstractBuilder中的函数进行封装，但是它的函数都是synchornized的，所以是线程安全的。

AbstractStringBuilder的实现：

- value：底层实现的char数组

- count：当前的capacity大小

- MAX\_ARRAY\_SIZE：值为Integer.MAX_VALUE - 8，避免某些情况下的虚拟机一处

```java
package java.lang

abstract class AbstractStringBuilder 
	implements Appendable, CharSequence {
	
	/** 用来储存字符的底层数组 */
	char []value;	
	int count;
	
	/**
	 * value数组最大的容量
	 * 某些虚拟机在数组放入一个8字的头部
	 * 由于虚拟机对数组的大小有限制
	 * 为了避免在申请数组时造成内存溢出
	 * 不能构造一个数组大小值为（Integer.MAX_VALUE + 8）
	 */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	
	AbstractStringBuilder() {}
	AbstractStringBuilder(int capacity) {
		value = new char[capacity];
	}
	/**
	 * 确保value的容量大小 >= 传入的参数minimumCapacity
	 * 如果value的容量不满足，那么value数组会进行扩容
	 * 扩容后大小为原来数组的两倍（直到满足minimumCapacity的大小）
	 * 
	 * @Param minimumCapacity 要求的最低容量数
	 */
	public void ensureCapacity(int minimumCapacity) {
		if(minimumCapacity > 0)
			ensureCapacityInternal(minimumCapacity);
	}
	
	private void ensureCapacityInternal(int minimumCapacity) {
		if(minimumCapacity - value.length > 0) {
			value = Arrays.copyOf(value, newCapacity(minimumCapacity));
		}
	}
	
	/**
	 * 进行扩容操作
	 * 扩容后的大小为 newCapacity =  oldCapacity * 2 + 2;
	 * if newCapactiy > Integer.MAX_VALUE 抛出OutOfMemoryError
	 * else newCapactiy > MAX_ARRAY_SIZE 设置value的数组大小
	 */
	private int newCapacity(int minCapacity) {
		int newCapacity = (value.length << 1) + 2;
		if(newCapacity - minCapacity < 0) {
			newCpacity = minCapacity;
		}
		return (newCapacity <= 0 || MAX_ARRAY_SIZE - newCapacity < 0) ?
			hugeCapacity(minCapacity) :
			newCapacity;
	}
	
	privagte int hugeCapacity(int minCapacity) {
		if(Integer.MAX_VALUE - minCapacity < 0) {
			throw new OutOfMemoryError();
		} 
		return (minCapacity > MAX_ARRAY_SIZE) ?
			minCapacity : MAX_ARRAY_SIZE;
	}
	
	@Override
	public char charAt(int index) {
		if((index < 0) || (index >= count)) 
			throw new StringIndexOfBoundsException(index);
		return value[index];
	}
	
	/**
	 * 在value数组尾部添加str对应的字符
	 * 如果传入的是null，那么添加"null"
	 */
	public AbstractStringBuilder append(String str) {
		if(str == null)
			throw appendNull();
		int len = str.length();
		ensureCapacityInternal(count + len);
		/**
		 * getChars(int srcBegin, int srcEnd, char []dst, int dstBegin)
		 * 函数调用System.arraycopy方法
		 */
		str.getChars(0, len, value, count);
		count += len;
		return this;
	}
	
	private AbstractStringBuilder appendNull() {
		int c = count;
		ensureCapacityInternal(c + 4);
		final char []value = this.value;
		value[count++] = 'n';
		value[count++] = 'u';
		value[count++] = 'l';
		value[count++] = 'l';
		count = c;
		return this;
	}
	
	@Override
	public AbstractStringBuilder append(char c) {
		ensureCapacityInternal(count + 1);
		value[count++] = c;
		return this;
	}
	
	/** 
	 * 生成新的String对象
	 * 对新对象操作并不会影响原对象的值
	 * @Param start The begining index, inclusive
	 * @Param end   The ending   index, exclusive
	 */
	public String subString(int start, int end) {
		if(start < 0)
			throw new StringIndexOutOfBoundsException(start);
		if(end > 0)
			throw new StringIndexOutOfBoundsException(end);
		if(strat > end)
			throw new StringIndexOutOfBoundsException(end - start);
		return new String(value, start, end - start);
	}
}
```


# StringBuilder

```java
public final Class StirngBuilder
	extends AbstractStringBuilder
	implements java.io.Serializable, CharSequence {
	
	public StringBuilder() {
		super(16);
	}
	
	public StringBuilder(int capacity) {
		super(capacity);
	}
	
	public StringBuilder(String str) {
		super(str.length() + 16);
		append(str);
	}
	
	/**
	 * StringBuilder中的函数都类似于append函数
	 * 只是对AbstractStringBuilder中的方法进行封装
	 */
	@Override
	public StringBuilder append(char c) {
		super.append(c);
		return this;
	}
	
	@Override
	public String toString() {
		return new String(value, 0, count);
	}
}
```

# StringBuffer


```java
package java.lang

public final class StringBuffer 
	extends AbstractStringBuilder 
	implements java.io.Serializable, CharSequence {
	
	/**
	 * 保存着value的缓存
	 * 每一次对StringBuffer进行更新，都会将cache置为null
	 */
	private transient char []toStringCache;	
	public StringBuffer() {
		super(16);
	}
	
	public StringBuffer(int capacity) {
		super(capacity);
	}
	
	public StringBuffer(String str) {
		super(str.length() + 16);
		append(str);
	}
	
	@Override 
	public synchronized char charAt(int index) {
		if((index < 0) || (index >= count))
			throw new StringIndexOutOfBoundsException(index);
		return value[index];
	}
	
	@Override
	public synchronized void setCharAt(int index, char ch) {
		if((index < 0) || (index >= count))
			throw new StringOutOfBoundsException(index);
		toStringCache = null;
		value[index] = ch;
	}
	
	@Override 
	public synchronized StringBuffer append(String str) {
		toStringCache = null;
		super.append(str);
		return this;
	}
	
	/**
	 * 设置toStringCache缓存
	 * 这样重复获取StringBuffer的值就不用重复生成
	 * 但是StringBuffer进行更新之后，cache更新为null
	 */
	@Override
	public synchronized String toString() {
		if(toStringCache == null) {
			toStringCache = Arrays.copyOfRange(value, 0, count);
		}
		return new String(toStringCache, true);
	}
}

```