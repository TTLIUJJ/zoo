# CyclicBarrier

CountDownLatch只能使用一次，而CyclicBarrier的计数器可以使用reset()方法重置。比如有一个场景，如果多线程计算结果不符合预期，可以令其再重新执行一次。

```java
public class CyclicBarrier {
	
}
```