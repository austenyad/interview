## HashMap / ConcurrentHashMap

### 构造方法



### put

### get

### resize 1.7 1.8

### 巧妙的而进行

### HashMap 为什么线程不安全

https://www.jianshu.com/p/c0642afe03e0

## ConcurrentHashMap

### 构造函数

默认构造

sizeCtl = 0

初始化容量的构造函数

sizeCtl = cap





### sizeCtl 

当 sizeCtl 为负数，表示 table 正在被初始化或者 resize。	

具体的是 ： 

-1 表示初始化

\- (1+ 正在执行 resize 的线程数)

其他情况：

1. 如果 table 未初始化，表示 table 需要初始化的大小。
2. 如果 table 初始化完成，表示 table 的容量，默认是 table 大小的 0.75 倍，

```java
private transient volatile int sizeCtl;
```

### initTable

table 的初始化操作延缓到第一次 put 时，但是对于 ConcurrentHashMap 它要满足在并发执行的情况下，只初始化一次。

那么它怎么做到的呢？

```java
/**
 * Initializes table, using the size recorded in sizeCtl.
 * 
 */

private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    while ((tab = table) == null || tab.length == 0) {
        if ((sc = sizeCtl) < 0)
            Thread.yield(); // lost initialization race; just spin
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) { //cas
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc; // 
            }
            break;
        }
    }
    return tab;
}

```

**sizeCtl** 默认值是 0，如果 ConcurrentHashMap 实例化时有传参数，**sizeCtl** 会是一个 2 的幂次方的值。所以执行第一次 put 操作的线程会执行 Unsafe.compareAndSwapInt 方法将 **sizeCtl** 值修改为 -1 ，有且只有一个线程能修改成功，其他线程通过 **Thread.yield()** 让出 CPU 执行时间片，等待 table 初始化完成。



### put

```java
/** Implementation for put and putIfAbsent */
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
          // 第一次 put 初始化 table 哈希表
            tab = initTable();
      	 	// 由于是 for 循环 第二次进入， 根据 key 的哈希值确定要放入 table 的索引 i
          // 确定数组索引 i 后，从 table 中取出对应索引的元素 f。
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {  
        // 如果f为null，说明table中这个位置第一次插入元素，利用Unsafe.compareAndSwapObject方法插入Node节点。
          // cas 成功 说明 node 节点已经插入 随后addCount(1L, binCount)方法会检查当前容量是否需要进行扩容。
          // 如果CAS失败，说明有其它线程提前插入了节点，自旋重新尝试在这个位置插入节点。
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
              
                break;                   // no lock when adding to empty bin
        }
      // 如果f的hash值为-1，说明当前f是ForwardingNode节点，意味有其它线程正在扩容，则一起进行扩容操作。
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        else {
          //其余情况把新的Node节点按链表或红黑树的方式插入到合适的位置，
          //这个过程采用同步内置锁实现并发，代码如下:
            V oldVal = null;
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                       value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);
                if (oldVal != null)
                    return oldVal;
                break;
            }
        }
    }
    addCount(1L, binCount);
    return null;
}
```

1. 首先拿键值对 key - value 的 key ，通过哈希算法得到 key 对象的哈希值。

2. 在哈希表 table 中根据 key 的哈希值，定位键值对 key - value 要放入的哈希桶，也就是数组的索引 index。

3. 根据索引 index 获取哈希表中的元素 f。

   在获取时，采用 Unsafe.getObjectVolatile(Object obj,long offset) 来获取，

   主要是因为 在 Java 内存模型中，我们知道每个线程有工作内存，里面会存储 table 的副本，虽然 table 是被 volatile 修饰的，它这并不能保证线程每次拿到 table 中的元素是新元素。

   `Unsafe.getObjectVolatile(Object obj,long offset)` 方法：获取对象中偏移量为 offset 的变量对应的 volatile 语义的值。obj 是数组对象，用 offset 来指定在数组中的偏移量来获取数组中的元素，并使其元素值，具有 volatile 语义。

4. 

在节点 f 上进行同步操作，再次利用 tabAt(tab, i) == f 判断，防止被其他线程修改。

1. 
2. 如果 f 属于 TreeBin 类型节点，说明 f 是红黑树根结点，则在树结构上遍历元素，更新或增加结点。
3. 如果链表中节点数 binCount >= TREEIFY_THRESHOLD(默认是8)，则把链表转化为红黑树结构。



```java
    private static final int ASHIFT;
		// Unsafe.arrayIndexScale(Class arrayClass)：获取数组中一个元素占用的字节。
 		int scale = U.arrayIndexScale(ak);
//这个方法是用来计算int的二进制值从左到右有连续多少个0。
//我们知道Java里的int型是有负数的，负数的二进制第1位肯定是1，所以如果参数i小于0，应该直接返回0就可以了
  	ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);

static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
  // Unsafe.getObjectVolatile(Object obj,long offset) 获取对象 obj 中偏移量为 offset 的变量对应
  // 的对应的 volatile 语义的值。
  // 此处表示 获取数据对象，从其实位置 ABASE 偏移量为 ((long)i << ASHIFT) 的元素的 volatile 语义值
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}
```

```java
long objectFieldOffset(Fidld field) 方法： 返回指定变量在所属对象的内存偏移地址。
```

