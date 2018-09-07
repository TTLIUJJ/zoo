# String

String作为一个不可被修改的类，主要使用以下两个final来保证

- final类 意味着String类不能被继承，并且它的成员方法都默认为final方法

- private final char value [] 
	
	- fianl 意味着String在创建之后，保证内部保存字符串的char数组不可以指向其
	- private 意者不能被外部访问，即不会外部代码修改

```java
public final class String
	implements java.io.Serializable, Comparable<String>, CharSequence {
	
	private final char value[];
	
	/** 保存string的hash值， 用于快速比较 */
	private int hash;
	
}
```

## 真的是不可以修改？

当然不是，利用Java的反射机制不仅可以修改value保存的值，也可以改变value指向其他数组。

```java
/**
 * 验证了可以利用反射修改String的值
 */
public static void testString() {
    String str = new String("will be change ?");
    try {
        Field field = String.class.getDeclaredField("value");
        field.setAccessible(true);
        char []ref = (char [])field.get(str);
        for(int i = 0; i < 4; ++i) {
            ref[i] = '?';
        }
        System.out.println(str);    // 输出  ???? be change ?
        
        ref = new char [] {'a', 'n', 'o', 't', 'h', 'e', 'r'};
        field.set(str, ref);
        System.out.println(str);	// 输出 another
    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

## String的比较 == 