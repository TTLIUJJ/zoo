# Exception



## Java 标准异常

Throwable这个Java类被用来表示任何可以作为异常抛出的类。Throwable对象可分为两种类型（从Throwable继承而得到的类型）：

- Error用来表示编译时和系统错误（除特殊情况，一般不用关心）
- Exception是可以被抛出的基本类型


### 特例：RuntimeException

属于RuntimeException的异常类型会被Java虚拟机自动抛出，所以不必在异常说明中把它列出来，也被称为“不受检查异常”，这种异常属于错误，将被自动捕获，不用亲自动手。

```
if(t == null)
	throw new NullPointerException();
```

对于RuntimeException，编译器不需要异常说明，其输出被报告给System.err，并且程序会在退出main()之前，调用异常的printStackTrace()方法。

务必记住：只能在代码中忽略RuntimeException（及其子类）类型的异常，其他类型异常的处理都是由编译器强制实施的。究其原因，RuntimeException代表的是编程错误。


### 常见的异常类型

![](http://p5s0bbd0l.bkt.clouddn.com/exception1.jpg)


### 处理异常

- 尽量避免RuntimeException: 比如可能出现的空指针异常，数组越界异常
-  对于catch块的代码要打印日志，并且不能让数据库信息返回给客户端
-  在I/O处理和SQL连接之后，及时在finally块中关闭