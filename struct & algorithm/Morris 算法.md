# Morris 算法


- 程序开始时，cur指向head节点

- 如果cur节点 有左子树，那么找到其左子树的最右节点，记为mostRight，让 cur = cur.left，进行下一步
	
	- 如果mostRight指向null，那么第一次到达cur节点，更新mostRight = cur ，让 cur = cur.left

	- 如果mostRight指向cur ，那么第二次到达cur节点，更新mostRight = null，让 cur = cur.right

- 如果cur节点没有左子树，让 cur = cur.right


### 辅助数据结构

```java
package algorithm.binayTraverse;

public class TreeNode {
    int val;
    TreeNode left;
    TreeNode right;
    public TreeNode(int val){
        this.val = val;
    }

    /**
     *          1
     *      2       3
     *   4    5   6   7
     * 8  9    11
     */
    public static TreeNode buildTree(){
        TreeNode node1 = new TreeNode(1);
        TreeNode node2 = new TreeNode(2);
        TreeNode node3 = new TreeNode(3);
        TreeNode node4 = new TreeNode(4);
        TreeNode node5 = new TreeNode(5);
        TreeNode node6 = new TreeNode(6);
        TreeNode node7 = new TreeNode(7);
        TreeNode node8 = new TreeNode(8);
        TreeNode node9 = new TreeNode(9);
        TreeNode node11 = new TreeNode(11);

        node1.left = node2;
        node1.right = node3;
        node2.left = node4;
        node2.right = node5;
        node3.left = node6;
        node3.right = node7;
        node4.left = node8;
        node4.right = node9;
        node5.right = node11;

        return  node1;
    }
}

```


### Morris 遍历

```java
package algorithm.binayTraverse;

/**
 * Morris的行为是在模拟系统的递归压栈
 *
 * 当前节点cur, 初来到head节点
 * 1. 如果cur没有左子树, cur向右移动, cur = cur.right
 * 2. 如果cur  有左子树, 找到左子树的最右节点, 记为 mostRight
 *    1. 如果mostRight的右指针指向null, 让其指向cur,  并且cur向左移动 cur = cur.left
 *    2. 如果mostRight的右指针指向cur,  让其指向null, 并且cur向右移动 cur = cur.right
 *
 * Morris并不使用额外的系统栈空间，也不使用额外的stack数据结构
 */
public class MorrisTraverse {

    public static void morrisPreTraverse(TreeNode head) {
        if(head == null) {
            return;
        }
        TreeNode cur = head;
        TreeNode mostRight = null;
        while (cur != null) {
            mostRight = cur.left;
            if(mostRight != null) {
                while(mostRight.right != null && mostRight.right != cur) {
                    mostRight = mostRight.right;
                }
                if(mostRight.right == null) {   // 第一次到达cur节点
                    mostRight.right = cur;
                    System.out.print(cur.val + " ");
                    cur = cur.left;
                    continue;
                }
                else {                          // 第二次到达cur节点
                    mostRight.right = null;     // 此时mostRight.right == cur
                }
            }
            else {
                System.out.print(cur.val + " ");
            }
            cur = cur.right;
        }
    }

    public static void morrisInTraverse(TreeNode head) {
        if (head == null) {
           return;
        }

        TreeNode cur = head;
        TreeNode mostRight = null;
        while (cur != null) {
            mostRight = cur.left;
            if (mostRight != null) {
                while (mostRight.right != null && mostRight.right != cur) {
                    mostRight = mostRight.right;
                }
                if (mostRight.right == null) {
                    mostRight.right = cur;
                    cur = cur.left;
                    continue;
                }
                else {
                    mostRight.right = null;
                }
            }
            System.out.print(cur.val + " ");
            cur = cur.right;
        }
    }

    public static void morrisPostTraverse(TreeNode head) {
        if (head == null) {
            return;
        }
        TreeNode cur = head;
        TreeNode mostRight = null;
        while (cur != null) {
            mostRight = cur.left;
            if(mostRight != null) {
                while (mostRight.right != null && mostRight.right != cur) {
                    mostRight = mostRight.right;
                }
                if(mostRight.right == null) {
                    mostRight.right = cur;
                    cur = cur.left;
                    continue;
                }
                else {
                    mostRight.right = null;
                    printEdge(cur.left);
                }
            }
            cur = cur.right;
        }
        printEdge(head);
    }

    private static void printEdge(TreeNode cur) {
        TreeNode tail = reverseEdge(cur);
        cur = tail;
        while (cur != null) {
            System.out.print(cur.val + " ");
            cur = cur.right;
        }
        reverseEdge(tail);
    }

    private static TreeNode reverseEdge(TreeNode cur) {
        TreeNode pre = null;
        TreeNode next = null;
        while ( cur != null) {
            next = cur.right;
            cur.right = pre;
            pre = cur;
            cur = next;
        }
        return pre;
    }

    public static void main(String []args) {
        TreeNode head = TreeNode.buildTree();

        morrisPreTraverse(head);
        System.out.println();

        morrisInTraverse(head);
        System.out.println();

        morrisPostTraverse(head);
    }
}

```

### 非递归遍历

```java
package algorithm.binayTraverse;

import java.util.Stack;

public class NonRecursionTraverse {
    public static void nonRecursionPreTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        TreeNode cur = head;
        Stack<TreeNode> stack = new Stack<>();
        while (cur != null || !stack.isEmpty()) {
            while(cur != null) {
                System.out.print(cur.val + " ");
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop().right;
        }
    }

    public static void nonRecursionInTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        TreeNode cur = head;
        Stack<TreeNode> stack = new Stack<>();
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            cur = stack.pop();
            System.out.print(cur.val + " ");
            cur = cur.right;
        }
    }

    public static void nonRecursionPostTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        TreeNode cur = head;
        TreeNode mark = head;
        Stack<TreeNode> stack = new Stack<>();
        while (cur != null || !stack.isEmpty()) {
            while (cur != null) {
                stack.push(cur);
                cur = cur.left;
            }
            TreeNode root = stack.peek();
            if (root.right == null || root.right == mark) {
                System.out.print(root.val + " ");
                mark = stack.pop();
            }
            else {
                cur = root.right;
            }
        }
    }

    public static void main(String []args) {
        TreeNode head = TreeNode.buildTree();

        nonRecursionPreTraverse(head);
        System.out.println();

        nonRecursionInTraverse(head);
        System.out.println();

        nonRecursionPostTraverse(head);
    }
}
```


### 递归遍历

```java
package algorithm.binayTraverse;

public class RecursionTraverse {
    public static void recursionPreTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        System.out.print(head.val + " ");
        recursionPreTraverse(head.left);
        recursionPreTraverse(head.right);
    }

    public static void recursionInTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        recursionInTraverse(head.left);
        System.out.print(head.val + " ");
        recursionInTraverse(head.right);
    }

    public static void recursionPostTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        recursionPostTraverse(head.left);
        recursionPostTraverse(head.right);
        System.out.print(head.val + " ");
    }

    public static void main(String []args) {
        TreeNode head = TreeNode.buildTree();

        recursionPreTraverse(head);
        System.out.println();

        recursionInTraverse(head);
        System.out.println();

        recursionPostTraverse(head);
    }
}
```


### 其他遍历

```java
package algorithm.binayTraverse;

import java.util.Deque;
import java.util.LinkedList;

public class LetterTraverse {
    public static void levelTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        TreeNode cur = head;
        Deque<TreeNode> deque = new LinkedList<>();
        deque.add(cur);

        while (!deque.isEmpty()) {
            int len = deque.size();
            while (len-- > 0) {
                cur = deque.poll();
                System.out.print(cur.val + " ");
                if (cur.left != null) {
                    deque.add(cur.left);
                }
                if (cur.right != null) {
                    deque.add(cur.right);
                }
            }
        }
    }

    public static void zLetterTraverse(TreeNode head) {
        if (head == null) {
            return;
        }

        TreeNode cur = head;
        Deque<TreeNode> deque = new LinkedList<>();
        deque.add(cur);
        boolean odd = true;

        while (!deque.isEmpty()) {
            int len = deque.size();
            while (len-- > 0) {
                if (odd) {
                    cur = deque.poll();
                    if (cur.left != null) {
                       deque.add(cur.left);
                    }
                    if(cur.right != null) {
                        deque.add(cur.right);
                    }
                }
                else {
                    cur = deque.pollLast();
                    if (cur.right != null) {
                        deque.addFirst(cur.right);
                    }
                    if (cur.left != null) {
                        deque.addFirst(cur.left);
                    }
                }
                System.out.print(cur.val + " ");
            }
            odd = !odd;
        }
    }

    public static void main(String []args) {
        TreeNode head = TreeNode.buildTree();

        levelTraverse(head);
        System.out.println();

        zLetterTraverse(head);
        System.out.println();
    }
}
```
