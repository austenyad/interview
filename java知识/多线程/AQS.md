## AQS 

Abstract Queued Synchronizer，一般翻译为 **同步器** 。它是实现多线程同步的框架。**AQS  非常重要。**

> 理解 AQS ：需要理解 双端队列，和 Unsafe 。

AQS 是抽象类，它是实现 JUC （Java Util Concrrent）中，比如 `ReentrantLock`、`Semaphore`、`CountDownLatch` 、`ThreadPoolExcutor`。理解 AQS 是我们理解 JUC 中的其他组件的重点，并在实际开发中可以通过过自定义 AQS 来实现各种需求场景。

我们通过 `ReentrantLock` 来理解 AQS 内部的机制。

首先从 `ReentrantLock` 的 `Lock()` 方法开始：

```java
/**
 * Acquires the lock.
 * 获取锁
 * <p>Acquires the lock if it is not held by another thread and returns
 * immediately, setting the lock hold count to one.
 *
 * <p>If the current thread already holds the lock then the hold
 * count is incremented by one and the method returns immediately.
 *
 * <p>If the lock is held by another thread then the
 * current thread becomes disabled for thread scheduling
 * purposes and lies dormant until the lock has been acquired,
 * at which time the lock hold count is set to one.
 */
public void lock() {
    sync.lock();
}
```

代码很简单只是调用 `Sync` 的 `lock()` 方法，这个 `Sync` 是什么呢？

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811164355.png)

可以看出，`Sync` 是 `ReentrantLock` 中的内部类。`ReentrantLock` 并没有直接继承 AQS，而是通过内部类 `Sync` 来扩展 AQS 的功能，然后 `ReentrantLock` 中存有 `Sync` 的全局变量引用。并且是在 `ReentrantLock` 构造方法中实例化 `Sync` 对象的。

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811170341.png)

`Sync` 在 `ReentrantLock` 有两种实现：`NofairSync` 和 `FairSync` ，分别对应 **非公平锁**  和 **公平锁** 。以非公平锁为例，实现源码如下：

```java
/**
 * Sync object for non-fair locks
 */
static final class NonfairSync extends Sync {
    private static final long serialVersionUID = 7316153563782823691L;

    /**
     * Performs lock.  Try immediate barge, backing up to normal
     * acquire on failure.
     */
    final void lock() {
        if (compareAndSetState(0, 1)) // 通过 CAS 操作来修改 state 状态，表示争抢锁的操作
            setExclusiveOwnerThread(Thread.currentThread());// 设置当前或得锁的线程 AbstractOwnableSynchronizer
        else
            acquire(1); // 上面 CAS 修改 state 状态失败，尝试去获取锁
    }

    protected final boolean tryAcquire(int acquires) {
        return nonfairTryAcquire(acquires);
    }
}
```

可以看出在非公平锁中的 `lock` 方法中，主要做了如下操作：

1. 如果通过 CAS 设置 `State` （同步状态）成功，表示当前线程获取锁成功，则当前线程设置为 独占线程。
2. 如果通过 CAS 设置 `State` （同步状态）失败，表示当前锁正在被其他线程持有，则进入 `aquire()` 方法进行后续处理。

**aquire()** 方法是一个比较重要的方法，可以分解为 3 个主要步骤：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811171956.png)

1. `tryAcquire()` 方法主要目的是尝试获取锁；
2. `addWaiter()` 如果 `tryAcquire()` 尝试获取锁失败则调用 `addWaiter` 将当前线程添加到一个等待队列中；
3. `acquireQueued()` 处理加入到队列中的节点，通过自旋去尝试获取锁，根据情况将线程 挂起 或者 取消。

以上 3 个方法都定义在 AQS 中，其中 `tryAcquire()` 有点特殊，其实现如下：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811172743.png)

默认情况下直接抛出异常，因此它需要在子类中复写，也就是说 **真正的获取锁的逻辑由子类同步器自己实现。**

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811173118.png)

```java
/**
 * Performs non-fair tryLock.  tryAcquire is implemented in
 * subclasses, but both need nonfair try for trylock method.
 */
final boolean nonfairTryAcquire(int acquires) {
    final Thread current = Thread.currentThread(); // 获取当前线程
    int c = getState(); // 获取 state 值
    if (c == 0) { // c = 0 说明当前是无锁状态 
        if (compareAndSetState(0, acquires)) { // 通过 CAS 操作来替换 state 值为 1
            setExclusiveOwnerThread(current); // 设置当前线程持有 独占锁
            return true;
        }
    }
    else if (current == getExclusiveOwnerThread()) { // 如果是同一个线程来获取锁，则直接添加 重入次数
        int nextc = c + acquires;
        if (nextc < 0) // overflow
            throw new Error("Maximum lock count exceeded");
        setState(nextc);
        return true;
    }
    return false;
}
```

解释说明：

* 获取当前线程，判断当前线程的锁状态

* 如果 `state = 0` 表示当前是无锁状态，通过 cas 更新 state 更新状态的值，返回 true;
* 如果当前线程输入重入，则增加重入次数，返回 true;
* 上述都不满足，则获取锁失败返回 false;

最后用一张图表示 `ReentrantLock.lock()` 过程：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811174722.png)

从图中可以看出，在 `ReentrantLock` 执行 `lock` 的过程中，大部分同步机制的核心逻辑都已经在 AQS  中实现，`ReentrantLock` 自身只实现要实现的特定步骤方法即可，这个设计模式叫做 **模板模式**。



### AQS 核心功能原理分析

首先看一下 AQS 中的几个关键的属性，如下所示：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811175511.png)

代码中展示了 AQS 两个比较重要的属性 **Node** 和 **state**。

**state 锁状态**

state 表示当前锁状态。当  state = 0 时表示无锁状态；当 state > 0 时，表示已经有线程获取到了锁，也就是 state = 1，如果一个线程多次获取同步锁的时候， state 会递增，比如 重入 5 次，那么 state = 5。而在释放锁的时候，同样需要释放 5 次直到 state = 0，其他线程才有资格获取锁。

state 还有一个功能是实现锁的 **独占模式** 或者 **共享模式**

* 独占模式： 只有一个线程能够只有同步锁。

比如在独占模式下，我们可以把 state 的初始化值设置成 0，当某个线程申请锁对象时，需要判断 state 的值是不是 0，如果不是 0 的话意味着其他线程已经持有该锁，则本线程需要阻塞等待。

* 共享模式：可以有多个线程持有同步锁。

某种场景下，我们允许 10 个线程同时进行，超过这个数量的线程就需要阻塞等待。那么只需要在线程申请对象时判断 state 的值是否小于 10。如果小于 10 ,就将 state 加 1 后继续同步语句的执行；如果等于 10 ,说明已经有 10 个线程在同步执行该操作，本线程需要阻塞等待。

**Node 双端队列节点**

Node 是一个先进先出的 双端队列，并且是等待队列，当多个线程争用资源被阻塞时会进入此队列。这个队列是 AQS 实现多线程同步的核心。

从之前的 `ReentrantLock` 图中可以看到，在 AQS 中有两个 Node 指针，分别执行队列的 head 和 tail。

Node 的主要结构如下：

```java
static final class Node {
    // 该等待同步的节点处于共享模式
    static final Node SHARED = new Node();
   //  该等待同步的节点处于独占模式
    static final Node EXCLUSIVE = null;

   // Node 中的线程状态，这个和 state 是不一样的：有1，0，-1，-2，-3 五个值
    volatile int waitStatus;
    
    static final int CANCELLED =  1;
 
    static final int SIGNAL    = -1;
  
    static final int CONDITION = -2;
   
    static final int PROPAGATE = -3;

 
    volatile Node prev; // 前继节点

    volatile Node next;// 后继节点

    volatile Thread thread;//等待锁的线程

    Node nextWaiter;

    /**
     * Returns true if node is waiting in shared mode.
     */
    final boolean isShared() {
        return nextWaiter == SHARED;
    }

    /**
     * Returns previous node, or throws NullPointerException if null.
     * Use when predecessor cannot be null.  The null check could
     * be elided, but is present to help the VM.
     *
     * @return the predecessor of this node
     */
    final Node predecessor() throws NullPointerException {
        Node p = prev;
        if (p == null)
            throw new NullPointerException();
        else
            return p;
    }

    Node() {    // Used to establish initial head or SHARED marker
    }

    Node(Thread thread, Node mode) {     // Used by addWaiter
        this.nextWaiter = mode;
        this.thread = thread;
    }

    Node(Thread thread, int waitStatus) { // Used by Condition
        this.waitStatus = waitStatus;
        this.thread = thread;
    }
}
```

默认情况下，AQS 中的链表结构如下图所示：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200811183645.png)

**获取锁失败后续流程分析**

锁的意义就是使用竞争到的锁对象线程执行同步代码，多个线程竞争锁时，竞争失败的线程需要被阻塞等待 后续唤醒。那么 `ReentrantLock` 是如何实现线程等待并唤醒的呢？

前面，我们提到 `ReentrantLock.lock()` 阶段，在 `acquire()` 方法中会先后调用 `tryAcquire()`、`addWaiter()`、`acquireQueued()` 这 3 个方法来处理。`tryAcquire()` 在 `ReentrantLock` 中被复写并实现，如果返回 true 说明成功获取锁，就继续执行同步代码语句。可是如果 `tryAquire` 返回 false，也就是说当前锁对象被其他线程所持有，那么当前线程会被 AQS 如何处理？

**addWaiter** 

首先当前获取锁失败的线程会被添加到一个等待队列的末端，具体源码如下：

```java
/**
 * Creates and enqueues node for current thread and given mode.
 *
 * @param mode Node.EXCLUSIVE for exclusive, Node.SHARED for shared
 * @return the new node
 * 将线程以 Node 方式添加到队列中
 */
private Node addWaiter(Node mode) {
    Node node = new Node(Thread.currentThread(), mode); // 把线程封装到一个新的 Node
    // Try the fast path of enq; backup to full enq on failure
    Node pred = tail;
    if (pred != null) { // 将 node 插入队列
        node.prev = pred;
        if (compareAndSetTail(pred, node)) { // CAS 替换当前的尾部。成功则返回
            pred.next = node;
            return node;
        }
    }
    enq(node); // 插入队列失败，进入 enq 自旋重试插入队列
    return node;
}
```

```java
/**
 * Inserts node into queue, initializing if necessary. See picture above.
 * @param node the node to insert
 * @return node's predecessor
 */
private Node enq(final Node node) {
    for (;;) {  // 自旋    重试 插入队列
        Node t = tail;
        if (t == null) { // Must initialize 如果队列从未初始化，需要初始化一个空的 Node
            if (compareAndSetHead(new Node()))
                tail = head;
        } else {  
            node.prev = t;
            if (compareAndSetTail(t, node)) {
                t.next = node;
                return t;
            }
        }
    }
}
```

有两种情况会导致插入失败：

1. tail 为空：说明队列从未初始化，因此需要调用 enq 方法在队列中插入一个 **空的 Node**。
2. compareAndSetTail 失败：说明插入过程中有线程修改了队列，因此需要调用 enq 将当前 node 重新插入到 队列末端。（**自旋** 重试 插入队列）

经过 `addWaiter`  方法之后，此时线程以 Node 的方式加入到队列的末端，但是线程还没有被执行阻塞操作，真正的阻塞操作是在下面的 `acquireQueued()` 方法中判断执行。

**acquireQueued**

在 `acquireQueued()` 方法中并不会立即挂起该节点中的线程，因此在插入节点的过程中，之前持有锁的线程可能已经执行完毕并释放锁，所以这里使用自旋再次去获取锁（不放过任何优化细节）。如果自旋操作还没有获取到锁！那么就将该线程挂起（阻塞），该方法的源码如下：

```java
/**
 * Acquires in exclusive uninterruptible mode for thread already in
 * queue. Used by condition wait methods as well as acquire.
 *
 * @param node the node
 * @param arg the acquire argument
 * @return {@code true} if interrupted while waiting
 */
final boolean acquireQueued(final Node node, int arg) {
    boolean failed = true;
    try {
        boolean interrupted = false;
        for (;;) {
            
            final Node p = node.predecessor();
             /**
             *  检测 当前节点的前驱节点是否是 head,这是获取锁的资格。
             *  如果是的话，则调用 tryAcquire 尝试获取锁
             *  成功，则将 head 置为当前节点
             */
            if (p == head && tryAcquire(arg)) {
                setHead(node);
                p.next = null; // help GC
                failed = false;
                return interrupted;
            }
            /**
             * 如果未成功获取锁 则根据前驱节点判断是否要阻塞。
             * 如果阻塞过程中被中断，则置 interruputed 标志为 true。
             * shouldParkAfterFailedAcquire 方法在前驱状态不为 SIGNAL 的情况下都会循环重试获取锁。
             */
            if (shouldParkAfterFailedAcquire(p, node) &&
                parkAndCheckInterrupt())
                interrupted = true;
        }
    } finally {
        if (failed)
            cancelAcquire(node);
    }
}
```

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200813150253.png)

上面是 ReentrantLock 在多线程环境下获取锁的步骤，从 lock 方法开始：

1. A 线程调用 AQS 的 `compareAndSetState` 修改 **state** 。
   * 成功：说明当前线程成功获取到锁，并调用 `setExclusiveOwnerThread` ，标记当前线程要独占锁。
   * 失败：因为 `comareAndSetState` 是 CAS 操作说明，在修改 state 值的过程中已经有其他线程成功修改了 state，即锁已经被其他线程占用。相反，调用 `acquire` 对没有获取到锁的线程进行其他处理。