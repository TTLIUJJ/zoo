# Manacher 算法

求字符串n中的最长回文串

```java
暴力法:
以每个字符串为中心，判断其左右两边的相等的最长字符串
往字符串中间添加特殊字符，可以满足字符串长度为奇数和偶数的情况
eg.
m:  1 2 2 1 3 3 1 2 2 1
n: #1#2#2#1#3#3#1#2#2#1#
maxLength: 在最中间的位置，找到最长的回文半径 21/2 = 10
时间复杂度: O(n^2)

Manacher算法:
1. 回文半径c: 包含字符x在内的,回文半径
eg.
a#1#2#x#2#1#b
x的回文半径c为6, #1#2#x 或者 x#2#1#

2. 回文半径数组: 保存字符串m中每个字符的回文半径

3. 整体最右回文右边界: 
eg.
n   :    #  1  #  2  #  3  #  2  #  1   #
i   :    0  1  2  3  4  5  6  7  8  9  10
R   :-1  1  2  2  4  4 10 10 10 10 10  10

算法流程
- i处于R外面，使用暴力扩展
- i处于R里面，c一定位于i左边，那么肯定有i的对称点i'
	- i'的回文半径位于c的回文半径内
	- i'的回文半径超出c的回文半径，那么i的回文半径只有R-i
	- i'的回文半径，刚好与L重合·，那么需要验证以i为半径，验证R+1的位置

时间复杂度: O(n)

eg. 考虑i在R里面
n :     [          o          ]
                   c          R-1    回文半径:
p1:      [ i']           [ i ]     radius[2*c-i], i'的回文半径
p2:    [   i'  ]       [   i   ]       R-i, 去除不符合大于R边界的
p3:     [  i' ]         [  i  ]      需要验证

```


```java
public static Class Manacher {
	/**
	 * 返回值s字符串的最大回文长度 等于最大回文半径 - 1
	 * eg.
	 *    1221  ==> #1#2#2#1#   返回值为 5-1 = 4
	 *    12321 ==> #1#2#3#2#1# 返回值为 6-1 = 5
	 */
	public static int maxLcpsLength(String s) {
		if (s == null || s.length() == 0) {
			return 0;
		}
		char []chars = manacherString(s);
		int []radius = new int[chars.length];
		int c = -1;
		int R = -1;	//  L[...c...]R
		int max = Integer.MIN_VALUE;
		
		for (int i = 0; i < radius.length; ++i) {
			/**
			 * 三目运算法, 表示算法流程4种情况下, i最小的回文半径
			 * 进入while循环的条件: R可以往右扩展, 并且对应的L可以往左扩
			 * while 处于情况: 
			 *   - i大于R 或者 i'的回文半径等于 c的回文半径		
			 *   - i'的回文半径 大于或者小于 c的回文半径
			 *     第一次判断就会break退出循环
			 * 
			 */
			radius[i] = R > i ? Math.min(radius[2*c-i], R-i) : 1;
			while (i + radius[i] < chars.length && i - radius[i] > -1) {
				if (chars[i+raidus[i]] == chars[i-radius[i]]) {
					++radius[i];
				}
				else {
					break;
				}
			}
			if (i + radius[i] > R) {	// 更新整体最右回文边界
				R = i + radius[i];
				C = i;
			}
			max = Math.max(max, radius[i]);
		}
	
		return max - 1;	
	}
	
	/**
	 * 将普通字符串转换成manacher可以处理的字符串
	 * eg.
	 * s       :  "12321"
	 * manacher:  "#1#2#3#2#1#"
	 */
	private static char[] manacherString(String s) {
		char []chars = s.toCharArray();
		char []res = new Char[s.length*2+1];
		int index = 0;
		for (int i = 0; i < res.length; ++i) {
			res[i] = (i & 1) == 0 ? '#' : chars[index++];
		}
		
		return res;
	}
}
```

	
## 应用

在字符串m="abc12321"的尾部添加一个字符串x，使得新的字符串为回文串，求字符串x在长度最小的情况的值。

```java
eg.
m : a b c 1 2 3 2 1
n : a b c 1 2 3 2 1 c b a

使用manacher算法, 从左往右处理
当找到某个c, 使得整体最右边界R等于字符串的最后一个字符时, 
此时就找到了m包含最后一个字符的最大回文串,
然后逆序非回文串的内容, 即需要添加的字符串x
```

