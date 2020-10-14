谈谈你的 Zygote 的理解？

* 了解 Zygote 的作用
* 熟悉 Zygote 的启动流程
* 深刻理解 Zygote 的工作原理

### 了解 Zygote 的作用

1. 启动 SystemServer 进程 

SystemServer 也是通过 Zygote 启动的，因为 SystemServer 需要用 Zygote 进程准备好的系统资源，比如说：我们常用的一些类、注册的 JNI 函数、主题资源、还包括一些共享库等等。直接从 Zygote 继承过来的话，就不需要自己再重新加载了。

2. 孵化应用进程



### Zygote 的启动流程

###### 启动三段式

启动三段式（总结的 Android 里面进程启动的常用套路）：

1. **进程启动**
2. 进程启动之后可以做一些 **准备工作**
3. 准备工作完成后进程 **Loop 循环** ，这个 Loop 循环它的作用就是不停的接受消息、处理消息。这个消息是从哪来的？--> 消息可能是通过 Socket 发过来的，也可能是 MessageQueue 里面的消息，也可能是 Binder 驱动发过来的消息。不管这个消息是从哪来的，它总的流程不变：就是 **接受消息--> 处理消息**。

这个启动三段式它不光 Zygote 进程里面是这样的，只要是有独立进程的，比如说：**系统服务进程**，我们自己的 **应用进程** 都是这样的。

##### Zygote 启动流程：

可以分成两块：

1. Zygote 的进程是怎么启动的？

init 进程 是 Linux 系统启动之后 **用户空间的第一进程**

init 进程启动之后，首先会去加载一个启动配置文件 init.rc ，然后在配置文件里面查看定义了那些系统服务需要被启动，咋们的 Zygote 就是要启动的服务之一，除了 Zygote 之外当然还有别的系统服务也是要启动的，比如说：我们很熟悉的 ServiceManager 进程。 

进程启动是通过 fork + execve 系统调用 。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-145339.png" alt="image-20201005225335871" style="zoom:20%;" align="left" />

如下是摘取的 启动配置文件 init.rc 中关于 Zygote 服务进程的启动配置信息

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-145553.png" alt="image-20201005225550054" style="zoom:30%;" align="left" />

如上图中：

 **Zygote** (表示 Service 的名词)

**/system/bin/app_process**   进程启动是通过 fork + execve 系统调用，这个系统调用它需要传入一个**可执行程序的路径**和**参数**。

所以 **-Xzytote /system/bin --zygote --start-system-server** 就是启动**参数**。

##### 启动进程方式

两种：1. fork + handle  ; 2. fork + execve 

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-150417.png" alt="image-20201005230415570" style="zoom:30%;"  />

fork 函数调用 它会返回两次 ，父进程中 返回子进程的 pid ，子进程中返回的 pid 为 0。

子进程默认继承父进程的资源（系统库 ？），而在子进程中调用 `execve(path,argv,env)` 那么继承父进程的资源就会被清掉，完成被 execve 加载的新的二进制程序替换掉， 1. path 可执行程序的路径；2. argv 它带的参数；3. env 环境变量。

#### 信号处理 - SiGCHILD

在 fork 子进程的时候，我们的父进程一般都要关注这个信号。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-151534.png" alt="image-20201005231532486" style="zoom:30%;" />

如上图：父进程 fork 出了 子进程，子进程挂了，那么它的父进程就会收到一个 SIGCHILD 的信号，这个时候父进程就可以去做相应的处理。 

比如说 Zygote 进程，如果它挂了，那么 init 进程就是会收到这个 SIGCHILD 信号，然后就会去重启这个 Zygote 进程。

2. 进程启动之后做 了些什么事？

*  Zygote 的 Native 世界

* Zygote 的 Java 世界

我们知道 Zygote 进程创建之后，它执行了个 exevce 的系统调用，这个系统调用它执行的是一个二进制的可执行程序，用 C++ 写的，里面有个 main 函数作为程序入口， 所有这个 Zygote 它天生是 native 的，做了一些准备工作之后，就切换到 Java 世界去运行了

Zygote 的 Native 世界做了些什么事？

其实就三件事，目的只有一个就是为了之后进入 Java 世界做准备，呢三件事呢？

1. 启动 Android 虚拟机
2. 注册 Android 的 JNI 函数 （注册一些系统的关键类的 JNI 函数 ）
3. 再通过 JNI 调用进入 Java 世界。

**思考题：在 C++ 的main函数去切到 Java 代码执行？**

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-153407.png" alt="image-20201005233405764" style="zoom:50%;" />

Zygote 的 Java 世界做了些什么事？

1. 预加载资源，这个是为了将来孵化出来的子进程可以进行继承的资源。常用的类、主题相关的资源、共享库。
2. 启动 SystemServer 进程（单独跑在一个进程里面，启动进程之后，后边的事就不用 Zygote 操心了）
3. 最后进入 Loop 循环，这个 Loop 循环它等待是 Socket 消息。（主要是 Socket 用一个名称表示就可以了，别人知道这个名称之后，就可以和这个 Socket 进行通信了）

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-154230.png" alt="image-20201005234229613" style="zoom:30%;" />



这个 Loop 循环里面是怎么处理 Socket 请求的？

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-05-154447.png" alt="image-20201005234444391" style="zoom:50%;" />

上面是 Loop 处理 Socket 请求的关键代码，我们前面说了 Zygote 在启动之后，最后会进入一个 Socket Loop 循环，在这个 Socket 循环里面他会不断的去轮询 Socket，当有新的请求过来的时候就会去执行这个 runOne() 函数，这个函数一共就做三件事：1. 先读取参数列表；2. 在根据这参数去启动子进程；3.最后在子进程里面开始干活，调用的就是这个 `handleChildProc(args,...)`函数。-->	在子进程里面，执行了一个 Java 类的 main 函数 入口函数，这个 Java 类的类名就是来自于  `readArgumentList()` 函数中读的参数列表，这个参数列表它是 AMS 跨进程发过来的，这个类名是 什么呢？类名其实就是 **ActivityThread** 。



###  要注意的点：

* Zygote fork 要保证单线程

在 fork 创建子进程的时候，把这个主线程之外的线程全停掉 ，等创建完成子进程之后再去把这些线程重启，Zygote 就是这么做的，Zygote 本身不是一个单线程，它里面跑了很多和虚拟机相关的一些守护线程

* Zygote 的 IPC 是没有采用 binder 

而是采用本地的 Socket，所以咋们这个应用程序的 binder 机制并不是从 Zygote 继承过来的，而是在应用程序它的进程创建之后自己启动的 binder 机制。

#### 问题：

1. 孵化应用进程这种事为什么不交给 SystemServer 来做，而专门设计一个 Zygote 呢？
2. Zygote 的 IPC 机制为什么不采用 biner ?如果采用 binder 会后什么问题吗？



