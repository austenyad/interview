![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200914142148.png)

ConcurrentHashMap 是一个二级哈希表。在总的 Hash 表下面，有若干个子 Hash 表。

Concurrent 优势就是采用 **锁分段技术**，每个 Segment 就好比一个自治区，读写操作高度自治，Segment 之间互不影响。

ConcurentHashMap 在读写是都需要二次定位。首先定位到 Segment，之后定位到 Segment 内具体的数组下标。



* HashMap  key 可以为 null,但只能有一个 key 为 null ，value 可以有多个为 null。
* HashMap 线程安全 : HashTable 。Collections.synchronizedMap 
* 但它们都是给整个集合加锁，导致同一时间锻内其他操作为阻塞状态，性能存着问题。
* ConcurrentHashMap key 和 vaule 都不能为 null。 hash 函数 spread 是什么意思？
* 



### get

* 

```java
/**
 * Returns the value to which the specified key is mapped,
 * or {@code null} if this map contains no mapping for the key.
 *
 * <p>More formally, if this map contains a mapping from a key
 * {@code k} to a value {@code v} such that {@code key.equals(k)},
 * then this method returns {@code v}; otherwise it returns
 * {@code null}.  (There can be at most one such mapping.)
 *
 * @throws NullPointerException if the specified key is null
 */
public V get(Object key) {
    Segment<K,V> s; // manually integrate access methods to reduce overhead
    HashEntry<K,V>[] tab;
    // 1. 获取 key 的哈希值
    int h = hash(key);
    // 2. 通过 key 的哈希值 在 segments 数组中定位到对应的 Segment 对象
    long u = (((h >>> segmentShift) & segmentMask) << SSHIFT) + SBASE;
    if ((s = (Segment<K,V>)UNSAFE.getObjectVolatile(segments, u)) != null &&
        (tab = s.table) != null) {
        // s != null 找到对应的 semgment && segment 中的 table 哈希表不为 null 
        // 3. 再通过 hash 值 定位到 segment 当中数组中的具体位置。
        for (HashEntry<K,V> e = (HashEntry<K,V>) UNSAFE.getObjectVolatile
                 (tab, ((long)(((tab.length - 1) & h)) << TSHIFT) + TBASE);
             e != null; e = e.next) {
            
            K k;
            if ((k = e.key) == key || (e.hash == h && key.equals(k)))
                return e.value;
        }
    }
    return null;
}
```

### put

```java
@SuppressWarnings("unchecked")
public V put(K key, V value) {
    Segment<K,V> s;
    if (value == null)
        throw new NullPointerException();
    // 1. 为输入的 key 做 hash 运算，得到哈希值
    int hash = hash(key);
    int j = (hash >>> segmentShift) & segmentMask;
    if ((s = (Segment<K,V>)UNSAFE.getObject          // nonvolatile; recheck
         (segments, (j << SSHIFT) + SBASE)) == null) //  in ensureSegment
        s = ensureSegment(j);
    return s.put(key, hash, value, false);
}
```

### size

size 方法的目的是统计 ConcurrentHashMap 的总元素数量，自然要把各个 Semgent 内部的元素数量汇总起来。

但是，如果在统计 Segment 元素数量的过程中，已经统计过的 Segment 瞬间插入新的元素，这时候该怎么办？



```java
public int size() {
    // Try a few times to get accurate count. On failure due to
    // continuous async changes in table, resort to locking.
    final Segment<K,V>[] segments = this.segments;
    int size;
    boolean overflow; // true if size overflows 32 bits
    long sum;         // sum of modCounts
    long last = 0L;   // previous sum
    int retries = -1; // first iteration isn't retry
    try {
        for (;;) {
            if (retries++ == RETRIES_BEFORE_LOCK) {
                // 每次统计都会给 retries +1  如果 这个统计次数超过 阈值，则对每个 Segment 加锁，再重新统计。
                // 下面进行再次统计时，由于已经加锁，次数肯定和上一次相等。
                for (int j = 0; j < segments.length; ++j)
                    ensureSegment(j).lock(); // force creation
            }
            sum = 0L;
            size = 0;
            overflow = false;
            // 遍历所有的 Segment
            // 把 Segment 元素数量累加起来
            // 把 Segment 的修改次数累加起来
           
            for (int j = 0; j < segments.length; ++j) {
                Segment<K,V> seg = segmentAt(segments, j);
                if (seg != null) {
                    sum += seg.modCount;
                    int c = seg.count;
                    if (c < 0 || (size += c) < 0)
                        overflow = true;
                }
            }
            if (sum == last) // 判断所有 Segment 的总修改次数是否等于上一次总修改次数。如果相等说明没有修改过，统计结束
                break;
            last = sum; // 走到这里，不相等，一般都是 sum 大于。 说明统计过程中有修改，重新统计 for 循环
        }
    } finally {
        if (retries > RETRIES_BEFORE_LOCK) {
            for (int j = 0; j < segments.length; ++j)
                segmentAt(segments, j).unlock(); // 释放锁，统计结束
        }
    }
    return overflow ? Integer.MAX_VALUE : size;
}
```

ConcurrentHashMap 的 size 方法是一个嵌套循环，大体逻辑如下：

* 遍历所有的 Segment
* 把 Segment 的元素数量累加起来
* 把 Segment 的修改次数累加起来。
* 判断所有 Segment 的总次数是否与上一次统计的总次数相等，相等表示统计过程中没有修改过，统计结束。如果不相等，一般都是 本次统计的修改次数大于上一次的修改次数，说明统计过程中有修改，重新统计。并且统计次数会 +1。
* 如果统计次数超过阈值，则对每一个 Semgent 加锁，再重新统计。
* 再次判断所有 Segment 的总修改次数是否等于上一次的修改次数。由于已经加了锁，次数一定和上次相等。
* 释放锁，统计结束。

为什么是这样的设计呢？**这种思想和乐观锁悲观锁思想如出一辙**

**为了尽量不锁住所有的 Segment，首先乐观地假设 Size 过程中不会有修改。当尝试一定次数，才会无奈转为悲观锁，锁住所有的 Segment 保证一致性。**







## 1.8 

### get

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    // 得到 key 的哈希值
    int h = spread(key.hashCode());
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n - 1) & h)) != null) {
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;
        }
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;
        }
    }
    return null;
}
```