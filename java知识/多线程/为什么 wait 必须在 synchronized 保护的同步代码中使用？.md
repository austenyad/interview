#### 为什么 wait 必须在 synchronized 保护的同步代码中使用？

首先，看 `wait` 方法的注释：

> this method should always be used in loop : 这个方法应该始终在循环中使用。
>
> ```java
> synchronized(obj){
>     while(condition does not hold)
>         obj.wait();
>     
>     
>         // Perform action appropriate to condition
>     
> }
> ```
>
> this method should only be called by a thread that is this owner of this object's monitor. ：只能在持有锁的线程中调用。
>
> if this current thread is not this owner of this object's monitor：如果线程持有锁，调用这个方法会抛出 `IllegalMonitorStateException` 异常。

从 `wait()` 方法的注释中看到：

方法必须放在 synchronized 保护的 while 代码块中，并始终判断执行条件是否满足，如果满足就往下继续执行，如果不满足就执行 `wait()` 方法。而在执行 `wait()` 方法之前，执行的线程必须持有对象的 monitor 锁，也就是 synchronized 锁。

这种写法是固定的，也就是设计这个语法的时候，必须按照这种方式来写。

那么这么写，这么设计有什么好处呢？

这个问题最后用逆向思维：如果不要求 `wait()` 方法放在 synchronized 保护的同步代码块中使用，而是可以随意调用，那么就会出现这样的代码：

```java
class BlockingQueue{
    Queue<String> buffer = new LinkedList<String>();
    // 生成者：往 buffer 队里中添加数据
    public void give(String data){
        buffer.add(data);
        notify();
    }
    
    //消费者：从 buffer 队里中取出数据，当 buffer 为空时，调用 wait 消费者线程处于 阻塞状态
    public String take(){
        while(buffer.isEmpty()){
            wait();
        }
        return buffer.remove();
    }
}
```

这段代码没有受到 synchronized 保护，于是便有可能出现以下场景：

1. 首先，消费者线程调用 take 方法并判断 buffer.isEmpty 方法是返回 true，若为 true 代表 buffer 为空的，则线程希望进入等待，但在线程调用 wait 方法之前，就被线程调度器暂停了，所以此时还没有来得及执行 wait 方法。
2. 此时生成者开始运行，执行了整个 give 方法，它往 buffer 中添加了数据，并执行了 notify 方法，但 notify 方法并没有任何效果，因此消费者线程的 wait 方法还没有来得及执行，所以没有线程被唤醒。
3. 此时，刚才被线程调度器暂停的消费者线程回来继续执行 wait 方法并进入类等待。

虽然刚才消费者判断了 buffer.isEmpty 条件，但真正执行 wait 方法时，之前的 buffer.isEmtpy 的结果已经过期类，不在符合新的场景了，因为这里的 “判断-执行” 不是原子操作，它在中间被打断了，是线程不安全的。

甚至，上面的极端情况下，如果没有类更多的消费者进行生产了，消费者便有可能陷入无穷无尽的等待，因为它错了刚才 give 方法内的 notify 的唤醒。

我们看到正是因为 wait 方法所在的 take 方法没有被 synchronized 保护，所以它的 while 判读和 wait 方法无法构成 **原子操作** ，那么此时整个程序很容易出错。

我们把代码改成源码注释所要求的的 被 synchronized 保护的同步代码块块形式，代码如下：

```java
public void give(String data){
    synchronized(this){
        buffer.add(data);
        notify();
    }
}

public String take() throws InterruptedException {
    synchronized(this){
        while(buffer.isEmtpy()){
            wait();
        }
        return buffer.remove();
    }
}

```

这样就可以确保 notify 方法永远不会在 buffer.isEmpty 和 wait 方法之间被调用，提升类程序的安全性。（因为不能获取到 锁）

另外，wait 方法会释放 monitor 锁，这也要就我们必须首先进入到 synchronized 内持有这把锁。（注释说了 调用 wait 的线程，调用时要释放当前线程持有的锁，没有锁 就抛出异常 InterruptedException）。



这里还有一个 “虚假唤醒” （spurious wakeup）的问题，线程可能在即没有被 notify/notifyAll，也没有被中断或者超时的情况下被唤醒，这种唤醒是我们不希望看到的。虽然在实际生产中，虚假唤醒 发生的概率很小，但是程序依然需要保证在发生 虚假唤醒的时候的正确性，所以就需要采用 while 循环的结构。

```java
while(condition does not hold)
	object.wait()
```

这样即便被虚假唤醒类，也会再次检查 while 里面的条件，如果不满足条件，就会继续 wait, 也就消除了虚假唤醒的风险。



#### 为什么 wait / notify /notifyAll 方法被定义在 Object 方法中，而 Sleep 定义在 Thread 中？

1. Java 中每个对象都可以成为一个 monitor 监视器锁，由于
2. 因为如果把 wait/notify/notifyAll 方法定义在 Thread 类中，会带来很大的局限性，比如



#### wait / notify 和 sleep 方法的异同：

###### 相同点：

1. 它们都可以让线程阻塞。
2. 它们都可以响应 interrupt 中断：在等待的过程中收到中断信号，都可以进行响应，并抛出 InterruputedException 异常。

###### 不同点：

1. wait 方法必须在被 synchronized 保护的代码块中执行，而 sleep 不需要；
2. 在同步代码块执行 sleep 方法时，并不会释放 monitor 锁，但执行 wait方法会主动释放 monitor 锁。
3. sleep 方法中会要求必须传入一个时间参数，时间到后会系统主动唤醒线程，而对于没有参数的 wait 方法而言，意味着永远等待，直到被中断或被唤醒才恢复，它并不会主动恢复。
4. wait / notify 是 Object 类的方法，而 sleep 方法是 Thread 类的方法。



