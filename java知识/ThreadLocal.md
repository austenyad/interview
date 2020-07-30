在每个线程内部都有一个名为 `threadLocals` 的成员变量，该变量的类型为 HashMap ，其中 key 为我们定义的 ThreadLocal 变量的 this 引用，value 则为我们使用 set 方法设置的值。每个线程的本地变量存放在线程自己的内存变量 threadLocals 中，如果当前线程一直不消亡，那么本地变量会一直存在，所以可能会造成内存泄漏，因此使用完毕后要记得调用 ThreadLocal 的 remove 方法删除对应线程的 threadLocals 中的本地变量。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-07-30-001142.png" alt="image-20200730081140612" style="zoom:50%;" />



