##### HashMap Java 1.7 100 cpu 死循环

在内部 HashMap 使用，保存 key - value 数据，当一对key、value 被加入时，会通过一个 Hash 算法得到数据的下标 index，算法很简单，根据 key 的 hash 值，对数组的大小取模 hash & (length - 1)，把 value 插入数据该位置，如果数组该位置，即桶中已经有元素了，那么说明存在 hash 冲突这样会在 index 位置的桶这里形成链表。

如果存在 hash 冲突，可能在极端情况下出现所有的元素都在同一个位置，形成一个长长的链表，这样 get 一个值时，最坏的情况下要遍历所有节点，性能变成 O(n)。

当插入一个节点，如果不存在相同的 key ，则会判断当前内部元素是否已经达到阈值，如果已经达到阈值，会对数组进行扩容，也会对链表进行 rehash 。

HashMap 方法的实现：

1. 判断 key 是否存在

```java
public V put(K key, V value) {
    if (key == null)
        return putForNullKey(value);
    int hash = hash(key);
    int i = indexFor(hash, table.length);
    // 如果key已经存在，则替换value，并返回旧值
    for (Entry<K,V> e = table[i]; e != null; e = e.next) {
        Object k;
        if (e.hash == hash && ((k = e.key) == key || key.equals(k))) {
            V oldValue = e.value;
            e.value = value;
            e.recordAccess(this);
            return oldValue;
        }
    }

    modCount++;
    // key不存在，则插入新的元素
    addEntry(hash, key, value, i);
    return null;
}
```

2. 检查容量是否达到 threahold

```java
void addEntry(int hash, K key, V value, int bucketIndex) {
    if ((size >= threshold) && (null != table[bucketIndex])) {
        resize(2 * table.length);
        hash = (null != key) ? hash(key) : 0;
        bucketIndex = indexFor(hash, table.length);
    }

    createEntry(hash, key, value, bucketIndex);
}

```

如果元素已经达到了 阈值 ，则扩容，并把元素移动到新数组上去。

3. 扩容实现

```
void resize(int newCapacity) {
    Entry[] oldTable = table;
    int oldCapacity = oldTable.length;
    ...

    Entry[] newTable = new Entry[newCapacity];
    ...
    transfer(newTable, rehash);
    table = newTable;
    threshold = (int)Math.min(newCapacity * loadFactor, MAXIMUM_CAPACITY + 1);
}
```

这里会创建一个更到的数组，并通过 transfer 方法，移动原来数组上的元素到新数组上。

```java
void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```

移动的逻辑也很清晰，就是遍历元素上的每个位置上的链表，并对元素重新进行 rehash ，在新的 newtable 找到新的位置，并插入。

1.7 中 put 方在遇到 hash 冲突的时候新插入的元素会被插入到链表的头部，在 rehash 时候也是一样，遍历链表，每次插入的时候都插入到链表的头部，其他元素向后移动。



假设 HashMap 初始化大小为 4，插入 3 个节点，不巧的是 3 个节点的 hash 到同一个位置，如果按照默认的负载因子的话，插入 3 个节点就会扩容，为了验证效果负载因子是 1。

```java
void transfer(Entry[] newTable, boolean rehash) {
    int newCapacity = newTable.length;
    for (Entry<K,V> e : table) {
        while(null != e) {
            Entry<K,V> next = e.next;
            if (rehash) {
                e.hash = null == e.key ? 0 : hash(e.key);
            }
            int i = indexFor(e.hash, newCapacity);
            e.next = newTable[i];
            newTable[i] = e;
            e = next;
        }
    }
}
```

以上是节点移动的相关逻辑，

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-075207.png" alt="image-20200912155205124" style="zoom:30%;" />

插第四个节点的时候发生 rehash ，假设现在有两个线程进行，线程1 和 线程 2，两个线程都会创建两个数组。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-075338.png" alt="image-20200912155337116" style="zoom:30%;" />

假设**线程 2** 在 `Entry<K,V> next = e.next;` 之后，cup 时间片用完了，这是变量 e 执行 a，变量 next 指向 b。

**线程 1** 继续执行，不巧的是 a 、b、c 节点 rehash 之后又在同一个位置 7 ，开始移动节点：

第一步移动 a

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-075721.png" alt="image-20200912155719454" style="zoom:30%;" />

第二步移动 b

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-075800.png" alt="image-20200912155759115" style="zoom:30%;" />

继续移动 c

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-075834.png" alt="image-20200912155828591" style="zoom:30%;" />

这时候 **线程1** 时间片用完了，内部的 table 还没有设置成新的 newTable，线程 2 开始执行，这个时候内部的引用关系如下：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-080048.png" alt="image-20200912160046844" style="zoom:30%;" />

这时 **线程2** 中，变量 e 执行 a，变量 next 执行 b，开始执行循环体的剩余逻辑。

```java
Entry<K,V> next = e.next;
int i = indexFor(e.hash, newCapacity);
e.next = newTable[i];
newTable[i] = e;
e = next;
```

执行之后的关系如下图：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-080341.png" alt="image-20200912160339549" style="zoom:30%;"/>

执行后，变量 e 指向 b ，因为 e 不是 null，则继续执行循环体，执行后的引用关系：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-081218.png" alt="image-20200912161217006" style="zoom:30%;" />

变量 e 又重新会到变量 a，只能继续执行循环体，这里仔细分析一下：1、执行完 `Entry<K,V> next = e.next;`，目前 a 没有 next 了，所以变量 next 执行 null；2、`e.next = newTable[i];` 其中 newTable[i] 指向节点 b，那么就是把 a 的 next 指向节点 b，这样 a 和 b 就互相引用了，形成一个环；3、`newTable[i] = e`，把 a 方到了数组 i 的位置；4、e = next ，把 e 复制为 null ，因为第一步变量中，next 指向的就是 null。

所以，最终的关系是这样：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-12-082022.png" alt="image-20200912162021060" style="zoom:30%;" />

节点 a 和 b 就行相互引用了形成了一个还，当在 hash 表中 get 获取 这个位置的 value 时，就会发生死循环。

另外，完成线程 2 把 newTable 设置成 table 之后，节点数据 c 就丢失了，看来还有数组遗失的问题。

###### 总结 

在并发环境下，发生扩容时，会产生循环链表，在执行 get 的时候，会发生死循环，引起 CPU 的 100% 问题，所以要在并发环境下避免使用 HashMap。**使用 ConCurrentHashMap。**