# Android Handler

1. 可以在子线程创建 Handler 吗？
2. 主线程的 Looper 和 子线的 Looper 有什么区别吗？
3. Looper 和 MessageQueue 有什么区别吗？
4. MessageQueue 是怎么创建的？

子线程的 Looper 是可以退出的，并且子线程的 Looper 是要自己 prepare 的， 主线程的 Looper 不能退出的，主线程的 Looper 在进程启动的时候就创建好了。

Java 层 Looper 里面是包含一个 MessageQueue 的，它们是一对一的关系，但是在 native 层 NativeMessageQueue 是包含一个 Looper 的，也是一对一的关系。

MessageQueue 有一个 Java 对象和一个 native 对象，java 对象在创建的时候就会调用一个 native 函数创建 NativeMessageQueue ，然后 NativeMessageQueue 中会创建一个 Looper ，Looper 里面会创建一个 eventfd，并且添加一个 可读事件到 epoll 里面。



![image-20201010061431968](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-221434.png)





线程间传递消息机制

1. 消息循环的过程是怎么样的？就是 Looper 的 loop() 的原理
2. 消息是怎么发送的？另一个线程是怎么往当前线程的消息队列里面发消息的呢
3. 消息是怎么处理的？另外一个线程往当前线程的消息队列里面发了消息之后，当前线程是怎么处理消息的，它是怎么去唤醒，怎么去分发消息的呢？





![image-20201010062946450](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-222948.png)

消息循环就是这个 Looper 的 loop() 函数：

1. 首先拿到 当前线程的 loop 对象，再从 looper 对象取出对应的 MessageQueue 。
2. 然后在 for 循环里面不断的 从 MessageQueue 获取下一个消息。注意：如果消息队列里面如果是空的话，这个函数会一直阻塞在这。这个 next() 函数一旦返回，要么说明这个 Looper 结束了，要么就说明有下一条消息了
3. 如果 next() 返回 msg，并且 msg != null ，那么就开始分发这个消息，分发的时候是通过 msg.target 即 Handler 进行分发的，调用 handler 的 dispatchMssage
4. 等消息分发完会回收这个消息，其实里面有一个 message 的单链表，就是把 message 重置了一些状态之后，把它塞回单链表。

怎么从消息队列里面取下一条消息

![image-20201010064709941](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-224711.png)

next() 函数里面是一个 for 循环:

在 for 循环里面调用 nativePollOnce() 函数，它是一个 native 函数 ，这个函数会一直阻塞在这，它阻塞在这主要是监听一个事件，如果有别的线程往当前的消息队列里面发一条消息的话，它就会去唤醒这个阻塞，函数就会返回继续向下执行，或者 nativePollOnce() 它有一个超时参数，到了超时时间它也一样被唤醒返回，或者说里面出了错误也会返回，

返回之后，它就会从消息队列里面取出消息，把消息标记为 使用，再返回。

注意的点：就是 next() 函数被调用的时候，也就是 nativePollOnce 第一次被调用的时候，超时nextPollTimeoutMillis 是 0，就是不管有没有新的消息过来，这个函数要立即返回的，检测这个消息队列有没有消息的，如果第一次发现这个消息队里里面没有消息，nextPollTimeoutMillis 就要被设置成 -1 ，设置成 -1 下次调用就会一直等待在这，一直等到有新消息来它才会返回 ，

![image-20201010070026492](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-230027.png)

![image-20201010070101893](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-230103.png)

![image-20201010070131083](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-230133.png)

这个函数是整个消息循环的核心

epoll_wait 一直在这等，等着有没有事件发生，这个 epoll_wait 会一直阻塞在这，这个 epoll_wait 返回的话有几种情况：1.出错了，出错的话这个 eventCount 是小于0的。2.超时了，超时的时候这个 eventCount 是 = 0 的 .3 真的有时间发生了 eventCount 就是事件的个数

然后就会在 for 循环里面依次去处理 epoll event  每个时间对应一个 fd ，如果 fd 就是我们关注的 wakeEventFd，并且这个事件是 读事件的话，那么就调用 awoken() 去消化这个事件，就是和管道一样，往里面写了东西，那对方总是不消化这个东西，不把它读出来的话 这个管道不停的写就会写满，哪下次可能再也写不进去了，所以有事件过来先得把这个事件消化一下。哪这样的话 pollInner 就可以返回了



消息循环的整个过程就是在一个 for 循环里面不断的调用 next 方法，从消息队列里面获取下一条消息，然后再把这个消息分发到对应的 handler ，这个 next 是怎么获取下一个消息的呢？就是在 for 循环里面不停的 nativePollOnce() ，不停的看有没有事件 ，这个nativePollOnce() 在没有消息的时候会阻塞在这，如果有别的线程往当前线程的消息队列里面发送消息的话，那个 nativePollOnce 就会返回，当前线程就会被唤醒，唤醒之后就会检查当前线程的消息队列里面有没有消息要处理。

怎么往消息队列里面发消息，原理是怎么的。

![image-20201010072353895](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-232355.png)

![image-20201010072422696](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-232427.png)

![image-20201010072447322](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-232448.png)

它其实就是往 wakeEventFd 里面写了一个东西，eventfd 就是一个计数器，哪这样 epoll_wait 就能收到这个 wakeEventFd 的可读事件了。





![image-20201010072727072](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-232728.png)

线程 A 里面有一个消息队列，还有一个 eventfd ，线程 A 它有两种状态，一种状态就是出于运行态，就处理这些消息的过程中，还有一种状态就是处在阻塞中，阻塞在 eventfd 上，它要监听这个 eventfd 的事件，什么时候这个 eventfd 会有事件呢？就是当有另外一个线程往当前线程的 消息队列里面 发了消息，并且还要通过往 evetfd 里面写一个东西，这样线程 A 就能收到这个事件了，线程就会被唤醒，唤醒了之后就会来处理这些消息。