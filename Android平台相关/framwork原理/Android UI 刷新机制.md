# Android UI 刷新机制

问题：

1. 丢帧一般是什么原因引起的？
2. Android 刷新频率 60 桢每秒，每隔 16 ms 调用 onDraw() 绘制一次？
3. onDraw 完之后屏幕会马上刷新吗？
4. 如果界面没有重绘，还会每隔 16 ms 刷新屏幕吗？
5. 如果在屏幕快要刷新的时候才去掉 onDraw 绘制会丢帧吗？

### 屏幕的刷新机制

![image-20201009001857319](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-161903.png)

1. 首先应用从系统服务里面申请 buffer ，然后系统服务返回 buffer 。
2. 应用再拿到这个 buffer 之后，它就可以进行绘制了。绘制完之后它就把这个 buffer 提交给系统服务。
3. 然后系统服务把这个 buffer 写到屏幕的一块缓存区里面，屏幕自己会有一定的帧率去刷新，每次去刷新的时候，它就会从这个缓存区里面把数据读出来然后显示出来。
4. 如果说缓存区里面没有新的图形数据，屏幕就会一直用老的数据，这样的话屏幕看起来就一直没有变。

#### 关于屏幕缓存区

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-162532.png" alt="image-20201009002531033" style="zoom:25%;" />

屏幕的缓存区不止一个，因为：如只有一个缓存区，屏幕这边正在读缓存，系统服务又正在写缓存，屏幕上的显示就会很奇怪，一半显示第一帧图像，另一半显示第二桢图像。怎么解决这个问题呢？我们搞两块缓存区就可以了。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-163039.png" alt="image-20201009003037416" style="zoom:25%;" />

一个缓存拿来显示，另一个缓存系统服务往里面写图像数据，这样的话这两个缓存区就不会相互干扰。如果屏幕要显示下一帧图像，把两个缓存交换一下就可以了。

#### 应用端是什么时候开始绘制的？

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-163354.png" alt="image-20201009003353324" style="zoom:33%;" />

屏幕会根据 vsync 信号进行周期性的刷新，这个 vsync 信息它是一个固定频率的脉冲信号，屏幕每次收到这个 vsync 信号就会去从缓存区里面取一帧图像来显示，而绘制是由应用端发起的（也就是缓存区的数据是有应用端来准备的），什么时候发起？这个随时都有可能。

我们看一下上面的图，第一个脉冲周期，屏幕上显示的是第 0 桢图像，第二个脉冲周期屏幕上显示的是第 1 桢，因为在 vsync 信号来的时候，第 1 桢已经绘制完了，所以在第二个脉冲周期就可以显示第 1 桢图像了。再来看第三个脉冲周期，它还是显示第 1 桢图像，因为此时第 2 桢还没有绘制完成，就是在 第二个 vsyn 信号来的时候还没有绘制完成。**它没有画完不是因为绘制太耗时，其实可能第 2 桢优化的还不错，它是绘制是小于这个 vsync 生命周期的，但是它比较倒霉，它是在 vsync 信号快来的时候才开始绘制，第 2 桢才绘制了一半，vsync 就来了，哪第三个周期它是赶不上了，所以它只能挪到下一个周期了，如果这个现象频繁的发生的话，这个是能感知到的，界面会看起来有点卡顿，就算应用层优化的再好也没用，因为这个是底层刷新机制的缺陷。**

这个问题怎么优化呢？其实很简单，那就是如果这个绘制也能和 vync 信号一个节奏的话哪这个问题就解决了。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-165049.png" alt="image-20201009005048619" style="zoom:33%;" />

每次 vsync 信号过来的时候：一方面屏幕获取图形数据，刷新界面，另一方面应用开始绘制来准备下一帧图形数据，如果说应用绘制优化的好，每一帧的绘制都能在 16 ms 以内，哪界面肯定非常的流畅。

但是有一个问题，应用里面界面的重绘，我们一般都调用 requestLayout() 函数拉发起重绘，这个函数随时都能够调用，但你怎么能够受你的控制，当 vsync 信号来的时候才重绘呢？**这是个问题？**

**Android 系统是怎么做的呢？**这个里面的关键就是 **Choreographer** 这个类。它最大的作用就是 你往里面发一个消息，哪这个消息最快也要等到下一个 vync 信号来的时候才能处理这个消息。  那么咋们的绘制可能是随时就能够发起的，你封装了一个 Runnable 丢给 Choreographer，然后下个 vsync 信号来的时候，就开始处理任务，然后就会正真的开始界面的重绘了。相当于你的 UI 绘制的节奏，完全由这个 Choreographer 来控制。

#### Choreographer 的底层实现原理

从 requestLayout 说起：

`requestLayout` 主要是发起 UI 重绘的，

![image-20201009021110010](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-181112.png)

这个函数里面又调到了 `scheduleTraversals()` 函数，

![image-20201009021258964](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-181300.png)

在 `scheduleTraversals()` 里面也没有直接就开始绘制，它干了两件事：

1. 线程的消息队里里面插入一个 SyncBarrier

SyncBarrier 它其实就是一个屏障，把这个屏障插入到消息队里里面，这个屏障后面的普通消息就不能处理，都得在这等着， 等到这个屏障撤除了以后才能处理，但是一个屏障对异步消息是没有影响的，为什么要有这么一个机制呢？ 主要是因为有些类型的消息非常紧急需要马上处理，如果说消息队里里面普通消息太多的话，是非常耽误事的，所以这个插了一个屏障，把普通消息先档在一边，优先处理异步消息，比如说下面往 Choreographer 丢的这个 Runnable ，它其实就是异步消息。等下一个 Vsync 信号来的时候，这个异步消息是要紧急处理的。

2. 往 Choreographer 的消息队里里面插了一个 postCallback：

**Choreographer** 是和 ViewRootImpl 一起创建的：

```java
  public ViewRootImpl(Context context, Display display) {
  ...
    mChoreographer = Choreographer.getInstance();// 通过 getInstance 函数创建
  }
```

```
public static Choreographer getInstance() {
    return sThreadInstance.get();
}
```

`sThreadInstance` 它是一个 `ThreadLocal<Choreographer>` ，也就是说你在不同的线程中去调用 `getInstance ` 函数它返回的是不同的 Choreographer 对象。

还有一点就是，假如说有人一口气调用了 10 次 `requestLayout()` 那么等下一次 Vsync 信号来的时候是不是会 触发 10 次 UI 重绘呢？这当然不是呢？为什么呢？

因为 requestLayout 会掉到 `scheduleTraversals()`

![image-20201009021258964](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-181300.png)

这个函数里面有一个 boolea 值：mTraversalScheduled，每次都会判读这个 boolean 变量，如果是 false 的时候才往 Choreographer 里面 post 一个 callback。

mTraversalRunnable 这个 Runable 里面调用了 doTraversal() 函数，mTraversalRunnable 在下一次 Vsync 信号来的时候才会在执行。



![image-20201009023632894](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-183634.png)

在 doTraversal() 方法里面有把 mTraversalScheduled 置为 false 。

![image-20201009025440772](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-185441.png)

所以在 Vsync 周期里面只会有一次 requestLayout() 有效。

2. 

![image-20201009025741254](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-08-185743.png)

```java 
mChoreographer.postCallback(
        Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);
```

最终会调用 `postCallbackDelayedInternal()` 函数：

1. Choreographer 里面有个数组，叫 `mCallbackQueues`，这个数组里面每个元素都是一个 callback 的单链表，方法中 根据不同的 callback 类型将 callback 插到对于的 单链表里面，插入的同时根据 callback 要执行的时间顺序来对 callback 在单链表里面排序，越是马上要发生的 callback，越是要放在链表的前边

![image-20201010053329206](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-09-213331.png)