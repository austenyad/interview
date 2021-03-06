### Java 中共享变量的内存可见性问题

谈到可见性，我们首先来看看在多线程环境下处理共享变量时 Java 的内存模型，如图所示：

图片

Java 内存模型规定，将所有的变量都放在主内存中，当线程使用变量时，会把主内存里的变量复制到自己的工作空间或工作内存，线程读写变量时操作的是自己工作内存中的变量。Java 内存模型是一个抽象概念，实际上在物理上并不存在什么线程的工作内存，那么实际现实中线程的工作内存是什么呢？

如下图：

图中所示 是一个双核 CPU 系统架构，每个核有自己的控制器和运算器，其中控制器包含一组寄存器和操作控制器，运算器执行算数逻辑运算。每个核都有自己的一级缓存，在有些架构里面还有一个所有 CPU 都共享的二级缓存。那么 Java 内存模型里面的工作内存，就对应这里的 L1 或者 L2 缓存或者 CPU 寄存器。



当一个线程访问共享变量时，它首先存主内存复制共享变量到自己的工作内存，然后对工作内存里面的变量进行处理，处理完成后将变量更新到主内存。

那么假设线程 A 和线程 B 同时处理一个共享变量，它会出现什么情况？我们使用上图所示的 CPU 架构，假设线程 A 和线程 B 使用不同 CPU 执行，并且当前两级 Cache 都为空，那么这时候由于 Cache 的存在，将会导致内存不可见问题，具体看下面分析：

* 线程 A 首先获取共享变量 X 的值，由于两级缓存都没有命中，所以加载主内存中 X 的值，假设为 0。然后把 X = 0 的值缓存到两级缓存，线程 A 修改 X 的值为 1，然后将其写入到两级缓存 Cache，**并且刷新到主内存**。线程 A 操作完成后，线程 A 所在的 CPU 的两级缓存 Cache 内和主内存里面的 X 的值都是 1。
* 线程 B 获取 X 的值，首先一级缓存没有命中，然后二级缓存，二级缓存没有命中了，所以返回 X = 1;这里一切都是正常的，因为这里主内存中也是 X = 1。然后线程 B 修改 X 的值为 2，并将其存放到线程 B 所在的一级 Cache 和二级 Cache 中，**最后更新主内存中 X 的值为2**；到这里一切都是好的。
* 线程 A 又需要修改 X 的值，获取时一级缓存命中，并且 X = 1，到这里问题就出现了，明明 B 线程已经修改 X 的值为 2，为何线程 A 获取的还是 1呢？这就共享变量的内存不可见问题，也就是线程 B 写入的值对线程 A 不可见。



### Volatile

该关键字可以确保对一个变量的更新其他线程马上可见。当一个变量被声明为 volatile 时，线程在写入变量时不会把值缓存在缓存中，而是把值刷回主内存。当其他线程读取该变量时，会从主内存重新获取新值，而不是使用工作内存中的值。