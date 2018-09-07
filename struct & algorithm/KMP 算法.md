# KMP 算法

在一个很长的字符串n中，找到包含字符串m的起始位置，时间复杂度O(n)

next数组，保存字符串m前缀和后缀相等的最大长度

```java
前缀和后缀的定义
eg. 
m: abc1234abcxyz
定位x元素，它的前缀和后缀相等的最大长度为3, 并且值为"abc"
注: 人为规定, m第一个元素的next值为-1

next数组保存的信息
eg.
m   : "abc1234abcxyz"
m   : [ a, b, c, 1, 2, 3, 4, a, b, c, x, y, z]
next: [-1, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 0, 0]

next数组保存的信息分别两点:
1. 前缀和后缀相等的最大长度
2. 前缀的下一个坐标

分析kmp算法流程
eg.
next数组中，y位置保存的信息应该是5, 表示y的前缀和后缀相等的最大长度为5
n : 1234abcdef123abcdefx
m :     abcdef123abcdefy
===> 此时配不上n配不上m字符串, 那么m往右推多个位置，如下，
     而不是仅仅从往右推1格，
n : 1234abcdef123abcdefx
m :              abcdef123abcdefy 

kmp算法的优化点: 前缀和后缀相等，那么m往右移动大于等于1个位置，并且还是从x的位置开始新一轮的匹配

构造next数组
eg. 求此时z的next值
m   : a b a c d a b a t k s a b a c d a b a y z
z的next值的查找过程如下,
y的相等缀长8, 第一次比较y和t, 结果不相等
t的相等缀长3, 第二次比较y和c, 结果不相等
c的相等缀长1, 第三次比较y和b, 结果不相等
b的相等缀长0, 第四次比较y和a, 结果不相等
next: a ||| b a || c d a b a | t k s a b a c d a b a | y z
```

```java
public class KMP {
	/** 
	 * 当且仅当 Y == str.length 表示Y越界
	 * 即, str1中有子字符串匹配了str2
	 * 那么, X - Y 的值即str1中匹配的起始位置
	 * 
	 * 反之, 退出循环条件表示X越界
	 * 即, 表示str1中不包含str2子串
	 */
	public static int getIndexOf(String n, String m) {
		
	
		return Y == str2.length ? X - Y : -1;
	}
	
	public int[] getNextArray(String m){
		char []chars = m.charArray();
		if (chars.length == 1) {
			return new int[] { -1 };
		}
		
		int []next = new int[chars.length];
		next[0] = -1;
		next[1] = 0;
		int i = 2;
		int cn = 0;
		
		while (i < next.length) {
			if (chars[i-1] == chars[cn]) {
				next[i++] = ++cn;
			}
			else if (cn > 0) {
				cn = next[cn];
			}
			else {
				next[i++] = 0;
			}
		}
		return next;
	}
}
```