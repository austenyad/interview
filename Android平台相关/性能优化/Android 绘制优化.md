# Anroid 绘制原理及工具选择

#### 1.绘制原理

*  Android 系统的绘制依赖于两个硬件，一个是 CPU ，一个是 GPU 。其中 CPU 负责计算显示内容：比如说 视图创建、 布局计算、图片解码、文本绘制等等功能。而 GPU 负责栅格化操作（UI 元素绘制到屏幕上）

* Android 系统每个 16 ms 会发出 Vsync 信息触发 UI 渲染，这个就意味着 没一帧都要在 16 ms 之内完成

* 绝大多数的 Android 设配屏幕刷新率都是 ： 60Hz 



#### 2. 工具选择

* Systrace （前面我们使用 Systrace 关注的是 CPU 的使用率），在绘制优化这里我们关注的是 Frames  
* 正常情况下每一帧都是用 绿色来表示的，如果是 黄色或者是绿色就意味着界面出现丢帧的异常，这个时候我们可以去找 **Alerts 栏：是 自动分析并且标准异常性能问题的条目**
* LayoutInspector
* Choreographer
  * 获取 FPS，线上使用，具有实时性
  * Api 16 之后
  * Choreographer.getInstance().postFrameCallback 

#### 3. Android 布局加载原理

setContentView -> LayoutInflater -> inflate -> getLayout -> createViewFromeTag -> Factory -> createView -> 反射。

 性能瓶颈

* 布局文件解析： IO 过程，将 Xml 文件解析到内存当中。有可能我们的布局文件非常复杂，文件内容非常大，那么这个 IO 过程就可能是非常消耗性能的，这就有可能导致卡顿。
* 创建 View 的过程它是这个反射的过程，还是如果当布局文件非常的复杂，就会多次调用反射，那么就更可能导致创建的过程会很慢。

##### LayoutInflate.Factory

​	在源码中可以看到，创建 View 的过程它有两种方式来创建，一个是 Factory2，另外一个是 Factory。对于这个 LayoutInflate.Facotry 它是 LayoutInflate 创建 View 的一个 Hook 。

那么我们就可以定制创建 View 的过程：全局替换自定义 TextView 等。

Factory2 继承于 Factory，Factory2 比 Factory 方法里面多个一个参数：**parent**，在开发过程中，我们更常用的是 Factory2，系统在判断的时候也是优先使用 Factory2。

#### 4. 优雅获取界面布局耗时

第一种：常规的方法 **手动获取 setContentView 的耗时**

第二种：AOP/ArtHook  ：使用 AspectJ

如何优雅的绘制任何一个控件的加载耗时。

##### 5. AsyncLayoutInflate 使用以及兼容问题（只是缓解的手段）

#### 6. x2c 框架

x2c 框架的缺点：

* 部分 xml 属性 Java 不支持
* 失去了系统的兼容性（AppCompat）



#### 7.视图绘制优化实战

如何优化布局的层级一个复杂度

避免过度绘制的技巧

其他

视图绘制的过程：

1. 测量：确定大小，Android 系统会自顶向下，对视图树进行遍历，确认每一个 ViewGrop 和 View 元素应该有多大
2. 布局：Android 系统会进行一个自顶向下的操作，每个 ViewGrop 会根据在测量阶段所测定的大小确认自己要拜访的位置
3. 绘制：对于视图树中的每一个对象，系统都会为其创建一个 Canvas 对象，然后向 GUP 发送绘制命令，以便进行绘制

在视图绘制阶段的性能瓶颈：

* 每个阶段耗时，这三个阶段如果都耗时的话，那肯定是需要优化的，
* 自顶向下的遍历，这三个阶段都有自顶向下的遍历过程，如果说 Layout 层级比较深，你这个遍历其实也是比较耗时的，
* 触发多次

##### 如何减少布局的层级和复杂度

在开发过程中：

* 尽量介绍 View 树的层级，布局减少嵌套。
* ConstraintLayout ，它实现了几乎完全扁平化的布局。它在构造复杂布局的时候明显性能会更高。使用也非常方便。





布局优化模拟面试

* 你在布局优化的时候都用到过哪些工具？

Choreographer

AOP、Hook

Systrace 、Layout Inspector

* 布局为什么会导致卡顿，你又是怎么优化的？

IO、反射、遍历、重绘

首先我们写的 Xml 文件，是通过 LayoutInflate 来讲 xml 文件映射到内存当中，这个是一个 IO 的过程，这个 IO 的过程可能会导致卡顿，

布局的加载是一个 反射 的过程，反射的过程也有可能卡顿

同时这个布局的层级比较深，那么遍历的次数也会变多也就会导致性能的消耗

层级的重绘

怎么优化的：

异步 Inflate x2c、减少层级、重绘

Aop 、监控

* 做完布局优化你有哪些成果产出









