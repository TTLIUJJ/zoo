# LRU 算法

```java
/**
     输入：第一行capacity,
     接下来每一行 p x y 表示新增表示缓存命中, 更新LRU队列; 更新表示非缓存命中, 不更新LRU队列
                g x   表示缓存查询, 命中返回val值, 并更新LRU队列, 否则返回-1
         2
         p 1 1
         p 2 2
         g 1
         p 2 102
         p 3 3
         g 1
         g 2
         g 3
     输出：
         1
         1
         -1
         3
 */
public class TestLRU {

    public static class Entry {
        int key;
        int val;
        Entry next;
        Entry prev;
        Entry(int key, int val) {
            this.key = key;
            this.val = val;
        }
    }

    /**
     * LRU
     * map 存储对应的 k, Entry
     * Entry 保存v的节点，并且组成 最近最少使用的 双端队列
     * head, tail 不储存数据,  分别next, prev来表示下一个节点
     */
    public static class LRU {
        int capacity;
        Map<Integer, Entry> map;
        Entry head;
        Entry tail;

        public LRU(int capacity) {
            this.capacity = capacity;
            this.head = new Entry(-1, -1);
            this.tail = new Entry(-1, -1);
            head.next = tail;
            tail.prev = head;
            this.map = new HashMap<>();
        }

        /** 更新, 不增加算最近使用 */
        public void put(int key, int val) {
            if (map.containsKey(key)) {
                map.get(key).val = val;
                return;
            }
            else {
                if (map.size() == capacity) {
                    removeEntryFromHead();
                }
                addEntryToTail(key, val);
            }
        }

        public int get(int key) {
            if (!map.containsKey(key)) {
                return -1;
            }
            else {
                return getAndUpdateEntry(key);
            }
        }


        private void removeEntryFromHead() {
            Entry cur = head.next;
            cur.next.prev = head;
            head.next = cur.next;

            map.remove(cur.key);
            cur.prev = null;
            cur.next = null;
            cur = null;
        }

        private void addEntryToTail(int key, int val) {
            Entry cur = new Entry(key, val);
            map.put(key, cur);

            tail.prev.next = cur;
            cur.prev = tail.prev;
            tail.prev = cur;
            cur.next = tail;
        }

        private int getAndUpdateEntry(int key) {
            Entry cur = map.get(key);

            cur.prev.next = cur.next;
            cur.next.prev = cur.prev;

            tail.prev.next = cur;
            cur.prev = tail.prev;
            cur.next = tail;

            return cur.val;
        }
    }


    public static void main(String []args) {
        Scanner in = new Scanner(System.in);
        int capacity = Integer.parseInt(in.nextLine());

        LRU lru = new LRU(capacity);
        while (in.hasNextLine()) {
            String []arg = in.nextLine().split(" ");
            int key = Integer.parseInt(arg[1]);
            if(arg.length == 2) {
                int val = lru.get(key);
                System.out.println(val);
            }
            else {
                int val = Integer.parseInt(arg[2]);
                lru.put(key, val);
            }
        }
    }
}
```