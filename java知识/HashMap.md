# HashMap

`loadFactor` ：哈希表负载因子，？？？？？？ 默认构造函数中是 0.75f。

## 构造函数

`initialCapacity`>0

最大容量 `1 << 30`

```java
// 判断不是一个数字
public static boolean isNaN(float v) {
    return (v != v);
}
```

```java
 /**
     * Returns a power of two size for the given target capacity.
     */
    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }
```

```java
if (p.hash == hash &&
    ((k = p.key) == key || (key != null && key.equals(k))))
    e = p;
```

情况—当put时 ，散列表位置有值：

`p` 是散列表中，已经有的值，

* `p.hash == hash` 哈希碰撞
* 1. `p.key == key` 在槽中的值 与 put 进来的 key 是执行堆中的同一个对象。肯定判断 是替换 value
  2. 如果 `p.key != key`，那么说明 hashcode 值相同，但不是同一个对象，接着`key != null && key.equals(k)` ，如果这个为 `true` 那么也 替换 value。这种情况 比如说是 String `"name"` 与 `new String("name")` 肯定是要替换 value

```java
  System.out.println("name".equals(new String("name"))); true
  System.out.println("name" == new String("name")); false
```

