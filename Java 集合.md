##### Java 集合的类结构图：



* List：放入顺序，有序、可重复；基于数组查询速度块；插入，删除伴随数据的移动，速度慢。

* Set： 放入顺序，无序，不可重复，重复元素被覆盖。检索元素效率低下，删除和插入效率高，插入和删除不会引起其他元素位置改变。

List 支持for循环，也就是通过下标来遍历，也可以使用迭代器，但是 Set 只能用迭代，因为他无序，无法用下标取得想要的元素。

线程安全集合类与非线程安全集合类

LinkedList、ArrayList、HashSet 是非线程安全的，Vector 是线程安全的；

HashMap是非线程安全的，HashTable 是线程安全的。

StringBuilder 是非线程安全的，StringBuffer是线程安全的。

```java
SyStem.arraycopy(Object src,  int  srcPos, Object dest, int destPos,int length);
// src 源数组
// dest 目标数组
//将源数组从 srcPos 开始，到 length 个元素，复制到 目标数组 从 destPos 位置开始。
// 复制 源数组的一部分 到 目标数组
// 从源数组 srcPos 索引开始，复制 length 个元素,将 length 个元素 粘贴到 目标数组，但是是从 destPost 位置开始粘贴。
```

ArrayList 默认构造初始容量为 0，当第一调用 add 方法时，容量扩展为 10 ，并且在后续容量会变为当前容量的 1.5 倍。

Vector 默认构造初始容量为 10，默认参数 capacityIncrement 参数为 0 时，扩展时默认为源数组的 2 倍长。

在 Vector 可以设置 capacityIncrement 在构造函数时，capacityIncrement 表示 在扩容时的数量，比如 capacityIncrement = 5，在进行扩容时，新数组会变为 源数组长度 + 5。

##### HashSet 和 TreeSet 的区别和适用场景

1. TreeSet 是二叉树（红黑树结构）实现的，TreeSet 中的数据是自动排序好的，不允许放入 null 值。
2. HashSet 是哈希表实现的，HashSet 中的数据是无序的可放入 null，但只能放入一个 null，两者中的值都不重复，就如数据库中唯一约束。HashSet 底层是 HashMap 实现的，在调用 add() 方法添加元素时，实质是有一个 HashMap 的实例，调用这个 hashMap 的 put 方法，put 方法 key 就是 add 方法要添加的元素，value 是一个静态且final的运行时常量，每次 add() ,value 的值都是一样的。

使用场景分析：

HashSet 是基于 Hash 算法实现的，其性能通常都优于 TreeSet。为了快速查找而设计的 Set，我们通常都应该使用 HashSet ，在我们需要排序功能时，采用 TreeSet。



### HashMap

**Put 方法**

1. 默认构造创建的 HashMap 对象，容量是 0（具体的就是 数组的大小为 0，这个数组存储的的类型的 Node,Node 中存放这 key - value、key的hashcode值，**由于 Node 代表的是在开始代表的是 链表的节点**，那么它还有 Node 的下一个节点，默认肯定是 null 。）。

2. 当首次调用 put 方法将 键值对 key 和 value 放入 HashMap 中时，发现此时 HashMap 的容量是 0 ，会调用 resize() 方法对 HashMap 容量进行初始化，且初始化长度为 16 。

   紧接着，将 key 的 hashcode 与 容量 做求余操作（这块并不是简单的求余操作，涉及到位运算，在后面具体分析），此时求得数组索引 index 肯定在数组索引范围内。

3. 根据就得的索引 index，取出数组中的元素，如果是空，空表明没有 hash 冲突，直接在当前索引位置根据 key - value 信息创建新的 Node 对象放进数组的此位置中。

4. 如果根据 key 的 hashcode 算出的索引并从数组中取出来的值不是空，那么就存在 hash 冲突。

5. Hash 冲突分为两种情况，1）key 的 hashcode 值相等，且 key 的引用也与从数组取出的 key 相等，那么肯定是同一个对象，直接替换调用原来的原来此处 Node 的 value，然后返回替换前的老值。

   2）key 的 hacode 值相同，但 key 的引用不相同。（极有可能 hashcode 也不一样，两种情况处理方式一样）。

6. 此时判断当前取出的 Node 是不是 TreeNode，是 TreeNode 表明当前桶中的元素早已经由链表转换为 红黑树，直接将值添加到 红黑树中。

7. 如果不是 TreeNode 那么表明链表还没有转换成红黑树，还是链表，那么遍历链表到链表尾部，将新值链到链表的尾部，插入完成。插入链表后会判断当前链表的长度是不是大于 8 ，如果大于 8 就会调用进行链表的树化，还有一个条件：此时 HashMap 的容量必须大于等于64才会进行正真的链表树化。

8. 不还是冲突还是不冲突，这个插入完成后，会对当前 HashMap 的键值对个数和扩容阈值做比较，如果大于阈值就会进行扩容

**resize** 方法



