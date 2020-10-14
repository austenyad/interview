# Synchronized 实现的原理

**Java 的线程是映射到操作系统原生线程上的，如果要阻塞或唤醒一个线程就需要系统帮忙，这就是要从用户态转换到内核态，就相当于 工作从 总公司分部 转换 到   总公司总部 的操作一样，状态转换需要花费很多的处理器时间。**



要了解 synchronized 的原理需要先理清楚两件事前： 

* 对象头
* Monitor

### 对象头

Java 对象在内存中的布局分为 3 部分：对象头、实例数据、对齐填充。

当我们在 Java 中使用 new 关键字的时候，JVM 会在堆中创建一个 `instanceOopDesc` 对象，这个对象包含了对象头以及实例数据。

`instanceOopDesc` 的基类为 `oopDesc`。它的结构如下

```c++
class oopDesc{
	friend class VMStructs;
    private volatile markOop _mark;
    union _metadata{
        videKlassOop _klass;
        narrowOop _compressed_klass;
    } _metadata;
}
```

其中 _mark 和 _metadata 一起组成对象头。 _metadata 主要保存了类的元数据，不用过多介绍。

重点看一下 _mark 属性， _mark 属性是 markOop 类型数据，一般称为 **标记字段（Mark Word）**，其中主要存储对象的 hashCode、分代年龄、是否是偏向锁、锁标志位等。

下图是 32 位 Java 虚拟机的 Mark Word 的默认存储结构：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20201014183300.png" style="zoom: 67%;" />

默认情况下，没有线程进行加锁操作，所以锁对象中的 Mark Word 处于 无锁状态。但是考虑到 JVM 的空间效率，Mark Word 被设计成为一个非固定的数据结构，以便存储更多有效的数据，它会根据对象本身的状态复用自己存储空间，如 32 为 JVM 下，除了上述列出的 Mark Word 默认结构外，还有如下可能的结构：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200727115151.png" style="zoom:67%;" />

从图中可以看出，更具 "锁标志位" 以及 "是否是偏向锁" ，java 中的锁可以分为以下几种状态：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200727115745.png" style="zoom:67%;" />

Java 6 之前，并没有轻量级锁和偏向锁，也就是通常所说的 synchronized 的对象锁，锁标志位 为 10 。从图中的描述可以看出：当锁是重量级锁时，对象头中 Mark Word 会用 30 bit 来指向一个 "互斥量"，而这个互斥量就是 Monitor 。

### Monitor

Monitor 可以把它理解为 一个同步工具，也可以描述为一种同步机制。实际上，它是一个保存在对象头中的一个对象。在 markOop 中如下代码：

```c++
bool has_monitor() const{
    return ((value() & monitor_value) != 0);
}

ObjectMonitor* monitor() const {
        assert(has_monitor(),"check");
    return (ObjectMonitor*)(value() ^ monitor_value);
}
```

通过 `monitor()` 创建一个 ObjectMonitor 对象，而 ObjectMonitor 就是 Java 虚拟机的 Monitor 的具体实现。因此 Java 中每个对象都会有一个对应的 ObjectMonitor 对象，这也是 Java 中所有的 Object 都可以作为锁的原因。

那 ObjectMonitor 如何实现同步机制的呢？

首先看下 ObjectMonitor 的结构：

```c++
ObjectMonitor{
	_header = NuLL;
	_count = 0;    // 记录个数
	_waiters = 0;
	_recursions = 0; // 锁重入次数
	_object = NULL; 
	_owner = NULL; // 指向持有 ObjectMonitor 对象的线程
	_WaitSetLock = 0; // 处于 wait 状态的线程，会被加入 _WaitSet
	_Responsible = NULL;
	_succ = NULL;
	_cxq = NULL;
	FreeNext = NULL;
	_EntryList = NULL; // 处于等待锁block状态的线程，会被加入到该列表
	_SpinFreq = 0;
	_SpinClock = 0;
	OwnerIsThread = 0;
}
```

其中有几个比较关键的属性：

```
_owner:   		指向持有 ObjectMonitor 对象的线程
_WaitSet:		存放处于 wait 状态的线程队列
_EntryList:		存放处于等待锁block状态的线程队列
_recursions:	锁的重入次数
_count:			用来记录该线程获取锁的次数
```

当多个线程同时访问同一段代码时，首先会进入 **_EntryList** 队列中，当某个线程通过竞争获取到对象的 monitor 后，monitor 会把 _owner 变量设置为当前线程，同时 monitor 中的计数器 _count 加 1，即获得对象锁。

若有 monitor 的线程调用 wait() 方法，将释放当前持有的 monitor，_owner 变量恢复为 null，_count 自减 1，同时该线程进入 _WaitSet 集合中等待唤醒。若当前线程执行完毕也将释放 monitor 锁 并复位变量的值，以便其他线程进入获取 monitor 锁。

