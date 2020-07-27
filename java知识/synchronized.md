### Synchronized 原理

了解 Synchronized 原理需要先清除两件事情：对象头和 Monitor



Java 对象在内存中的分布为 3 部分：

* 对象头
* 实例数据
* 对齐填充

当我们在 Java 代码中，使用 new 创建一个对象的时候，JVM 会在堆中创建 

![](E:\Study\interview\java知识\synchronized\20200727115151.png)



在 Java 6 之前，并没有轻量级锁和偏向锁，只有重量级锁，也就是 synchronized 的对象锁，锁标志位位 10。从图中的描述可以看出：当锁是重量级锁时，对象头中 Mark Word 会用 30 bit 来指向一个 “互斥量”，这个互斥量就是 Monitor 。

##### Monitor

Monitor 可以把它理解成为一个同步工具，也可以描述为一个同步机制。实际上，它是一个保存在对象头中的一个对象。在 markOop 中有如下代码：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200727131703.png)

通过 `Monitor()` 方法创建一个 `ObjectMonitor` 对象，而 `ObjectMonitor` 就是 Java 虚拟机中的 `Monitor` 中的具体实现。因此 Java 中每个对象头会有一个对应的 `ObjectMonitor` 对象，这也是 Java 中所以的 `Object` 都可以作为锁的原因。

那 `ObjectMonitor` 是如何实现同步机制的呢？

首先看下 `ObjectMonitor` 的结构：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200727132743.png)

其中有几个比较关键的属性：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200727133922.png)

当多线程同时访问一段同步代码时，首先会进入 _EntryList 队列中（处于等待获取锁，但锁被其他线程已经获取，此时这个线程处于阻塞等待的状态，这种状态的线程，就会在 _EntryList 队列当中），当某个线程通过竞争获取到对象的 `monitor` 后，`monitor` 会把 ` _onwer` 变量设置为当前线程，同时 `monitor` 中的计数器 ` _count` 会加 1，即获得对象锁。

若持有 `Monitor` 的线程调用 `wait()` 方法，将释放当前持有的 `monitor` ,` _onwer` 变量恢复为 `null` ，` _count` 自减 1，同时该线程进入 ` _WaitSet` 集合中等待被唤醒。若当前线程执行完毕也将释放 `monitor` 并复位变量的值，以便其他线程进入获取 `monitor` 。



图例子：补





> 实际上，`ObjectMonitor` 的同步机制是 JVM 对操作系统的 Mutex Lock (互斥锁) 的管理过程，期间都会转入操作系统内核态。也就是说 synchronized 实现的锁，在 “重量级锁” 状态下，当多个线程之间切换上下文时，还是一个比较重量级的操作。

 