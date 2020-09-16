## HashMap / ConcurrentHashMap

### 构造方法



### put

### get

### resize 1.7 1.8

### 巧妙的而进行

### HashMap 为什么线程不安全

https://www.jianshu.com/p/c0642afe03e0

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
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                if ((tab = table) == null || tab.length == 0) {
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    sc = n - (n >>> 2);
                }
            } finally {
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```

**sizeCtl** 默认值是 0，如果 ConcurrentHashMap 实例化时有传参数，**sizeCtl** 会是一个 2 的幂次方的值。所以执行第一次 put 操作的线程会执行 Unsafe.compare