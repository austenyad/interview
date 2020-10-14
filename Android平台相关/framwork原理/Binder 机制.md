# Binder 机制

### 一、Android Framework 里面用到了那些 IPC 方式？

考察点：

1. 看你是否了解 Linux 常用的跨进程方式

2. Android Framework 有没有研究过，并了解一些实现原理

3. 是否了解 Framework 各个组件之间的通信方式



### 1. 

#### 管道

* **半双工的，单向的；**管道的描述符数据只能往一个方向流，要么读要么写，那么要实现即能读又能写你需要两个描述符才行。Linux 提供 API `pipe(fds)` 可以生成一对描述符，一个用来写、一个用来读。
* **一般是在父子进程之间使用；** 匿名管道、命名管道 

下图是管道的用法，代码是 父进程向子进程里面通过管道写数据。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-114100.png" alt="image-20201006194057605" style="zoom:25%;" />

管道和epoll相结合监听读写事件。

#### Socket

本地 Socket

* **全双工的，即可以读也可以写；**
* **两个进程之间无需亲缘关系；** Socket 在创建的时候需要指定一个路径，只要将这个路径公开给别人，别人就可以通信了

Android framework 中用到 Socket 的地方：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-121004.png" alt="image-20201006201003075" style="zoom:25%;" />

Zygote 里面使用到了 Socket 。

通过 Socket 来接受 AMS 的请求，然后去启动应用进程，这个 main 函数就是 Zygote 的入口函数，里面通过 `registerZygoteSocket(socketName)`： 创建了一个本地的 Socket ，并且传入 Socket 的名字。然后做好了一些准备工作之后执行 `runSelectLoop(abliList)` ：**进入了 Select Loop 循环，去检测这个 Socket 有没有 新过来的连接或者数据** 

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-122020.png" alt="image-20201006202017444" style="zoom:33%;" />

`runSelectLoop()` 函数里面有一个循环，循环里面 Os.poll 是用来检测有没有我们关注的事件发生 ，如果有可能分两种情况：1. 有新的连接建立好了 2.有新的数据发过来；当有新的数据发过来就会执行 runOnce 来处理数据了：怎么处理呢？先从 Socket 里面把参数读出来，然后根据参数去执行相应的指令，主要是去创建应用进程，进程启动之后，还要通过 Socket 把进程的 PID 写给对象。

#### 共享内存

* **很快，不需要多次拷贝**（前面说到的不管是管道还是Sockt，它们传输的数据不能太大，太大的话性能会很糟，因为里面至少涉及到两次拷贝）共享内存不需要多次拷贝，当拿到文件描述符，把它同时映射到两个进程的内存空间，这样一个进程往里面写，另一个进程就能读到，所以非常快。
* **进程之间无需亲缘关系** 只有能拿到共享内存的文件描述符就行了，这个文件描述符是一个跨进程传递的。

1. Android 使用到的共享内存，在 Android 里面涉及到进程之间大数据传输的问题主要就是图像相关的

2. MemoryFile 工具内 : Andorid 匿名共享内存

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-123712.png" alt="image-20201006203655267" style="zoom:33%;" />

`native_open(name,length)` 这个函数在 native 层会调用 `ashmen_create_region(namestr,length)` 创建了一块匿名共享内存，这个函数它会返回一个文件描述符，然后调用 `native_mmpa` 函数，这个函数在 native 层会调用 `mmap(NULL,length,prot,MAP_SHARED,fd,0)`,将这个描述符 映射到当前进程的内存空间，这个内存空间的地址就是 mAddress。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-125120.png" alt="image-20201006205117556" style="zoom:33%;" />

#### 信号

* 单向的，发出去之后怎么处理是别人的事
* 只能带一个信号，不能带别的参数
* 知道进程的 PID 就能发送信号了，也可以一次给一群进程发信号 
* 发送信号也是要权限的，除非你是 root 权限，或者是你跟别的进程的 UID 形同，才能给发信号

Android Framework 中有用到信号：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-125614.png" alt="image-20201006205612028" style="zoom:25%;" />

1. 比如有时候要杀掉应用进程 killProcess 函数，需要权限，两个进程的 uid 相同才能发信号，虽然我们的应用进程都是从 Zygote 进程 fork 出来的，uid 默认都是和 Zygote 相同的，但是进程启动之后都是会马上重新设置自己的 uid 的，所以是不能随便给被人发信号的。

2. 2图代码：Zygote 关注的信号，SIGCHILD 信号，Zygote 启动了子进程之后，它需要关注这个子进程退出了没有，如果说子进程退出了 Zygote 要及时把它的资源回收掉。





### 二 

考察点：

1. 了解 binder 是用来干什么的？
2. 应用里面那些地方用来 binder 机制？
3. 应用启动的大致流程是怎么样的？
4. 一个进程是怎么启动 binder 进制的？

应用进程的启动流程

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-144044.png" alt="image-20201006224043185" style="zoom:25%;" />

AMS 要启动一个应用组件，比如说要启动一个 Activity，结果 AMS 去仔细一查发现这个组件它的进程还没有启动，然后 AMS 它就会向 Zygote 发一个请求，让 Zygote 去启动这个进程，Zygote 马上就把这个进启动起来了，这个应用进程启动起来之后就会向 AMS 报告，说我已经启动好了。

代码：Zygote 端

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-144603.png" alt="image-20201006224600896" style="zoom:25%;" />

1. 当 Zygote 收到 AMS 请求启动进程的请求之后，它就会调用这个 `runOnce` 函数；
2. 函数里面首先通过`readArgumentList()` 读取参数 ，参数就是 AMS 发过来的；
3. 读取了参数之后就会调用 `Zygote.forkAndSpecialize(... )`去创建应用的进程；
4. 创建完之后在 子进程 里面调用 `handleChildProc(...)`

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-145951.png" alt="image-20201006225950267" style="zoom:25%;" />

5. 在 handleChildProc 函数里面会调用 `zygoteInit()` 函数做一些初始化，这个初始化大概包括三块：

   1. `commonInit();` 里面主要是一些常规的初始化；
   2. `nativeZygoteInit()` 这个函数是一个 native 函数：

   ```C++
   //frameworks/base/core/jni/AndroidRuntime.cpp
   static void com_android_internal_os_ZygoteInit_nativeZygoteInit(JNIEnv* env, jobject clazz)
   {
       gCurRuntime->onZygoteInit();
   }
   ```

   ```c++
     virtual void onZygoteInit()
       {
           sp<ProcessState> proc = ProcessState::self();
           ALOGV("App process: starting thread pool.\n");
           proc->startThreadPool();
       }
   ```

   ```C++
   //frameworks/base/cmds/app_process/app_main.cpp 
      sp<ProcessState> ProcessState::self()
   {
       Mutex::Autolock _l(gProcessMutex);
       if (gProcess != nullptr) {
           return gProcess;
       }
       gProcess = new ProcessState(kDefaultDriver);
       return gProcess;
   }
   //单例
   ```

   ```c++
   ProcessState::ProcessState(const char *driver)
       : mDriverName(String8(driver))
       , mDriverFD(open_driver(driver)) // 通过 open_driver() 函数返回值 赋给 mDriverFD
       , mVMStart(MAP_FAILED)
       , mThreadCountLock(PTHREAD_MUTEX_INITIALIZER)
       , mThreadCountDecrement(PTHREAD_COND_INITIALIZER)
       , mExecutingThreadsCount(0)
       , mMaxThreads(DEFAULT_MAX_BINDER_THREADS)
       , mStarvationStartTimeMs(0)
       , mManagesContexts(false)
       , mBinderContextCheckFunc(nullptr)
       , mBinderContextUserData(nullptr)
       , mThreadPoolStarted(false)
       , mThreadPoolSeq(1)
       , mCallRestriction(CallRestriction::NONE)
   {
       if (mDriverFD >= 0) {
           // mmap the binder, providing a chunk of virtual address space to receive transactions.
           mVMStart = mmap(nullptr, BINDER_VM_SIZE, PROT_READ, MAP_PRIVATE | MAP_NORESERVE, mDriverFD, 0);
           if (mVMStart == MAP_FAILED) {
               // *sigh*
               ALOGE("Using %s failed: unable to mmap transaction memory.\n", mDriverName.c_str());
               close(mDriverFD);
               mDriverFD = -1;
               mDriverName.clear();
           }
       }
   
       LOG_ALWAYS_FATAL_IF(mDriverFD < 0, "Binder driver could not be opened.  Terminating.");
   }
   
   ```

   ```C++
   static int open_driver(const char *driver)
   {
       int fd = open(driver, O_RDWR | O_CLOEXEC);
       if (fd >= 0) {
           int vers = 0;
           status_t result = ioctl(fd, BINDER_VERSION, &vers);
           if (result == -1) {
               ALOGE("Binder ioctl to obtain version failed: %s", strerror(errno));
               close(fd);
               fd = -1;
           }
           if (result != 0 || vers != BINDER_CURRENT_PROTOCOL_VERSION) {
             ALOGE("Binder driver protocol(%d) does not match user space protocol(%d)! ioctl() return value: %d",
                   vers, BINDER_CURRENT_PROTOCOL_VERSION, result);
               close(fd);
               fd = -1;
           }
           size_t maxThreads = DEFAULT_MAX_BINDER_THREADS;
           result = ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads);
           if (result == -1) {
               ALOGE("Binder ioctl to set max threads failed: %s", strerror(errno));
           }
       } else {
           ALOGW("Opening '%s' failed: %s\n", driver, strerror(errno));
       }
       return fd;
   }
   ```

   ![image-20201006232335094](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-152336.png)

   `int fd = open(driver, O_RDWR | O_CLOEXEC);` 会打开一个设备，所以这个 ProcessState 构造方法里面会打开 binder 驱动 ;

   `if(mDriverFD >= 0)` 即 mDriverFD 是有效的，那么就会把它映射到当前进程的内存空间。

   ![image-20201006232844630](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-152846.png)

   这个 mThreadPoolStarted 是用来防御的，防止 spawnPooledThread() 多次调用。 

   ```C++
   void ProcessState::spawnPooledThread(bool isMain)
   {
       if (mThreadPoolStarted) {
           String8 name = makeBinderThreadName();
           ALOGV("Spawning new pooled thread, name=%s\n", name.string());
           sp<Thread> t = new PoolThread(isMain);
           t->run(name.string());
       }
   }
   ```

     `spawnPooledThread` 里面会 `new PoolThread()` ，然后开始 run 起来 ，那么这个线程池里面就一个线程。

   ![image-20201006233409081](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-153410.png)

   这个 PoolThread 它是继承了 Thread 类，线程启动就会调用这个 `threadLoop()` 函数， 函数里面 `IPCThreadState::self()`也是一个单例，但是它是一个线程里面的单例不是进程里面的单例，然后这个 `joinThreadPool(mlsMain)` 调用

   ```c++
   void IPCThreadState::joinThreadPool(bool isMain)
   {
       LOG_THREADPOOL("**** THREAD %p (PID %d) IS JOINING THE THREAD POOL\n", (void*)pthread_self(), getpid());
   
       mOut.writeInt32(isMain ? BC_ENTER_LOOPER : BC_REGISTER_LOOPER);
   
       status_t result;
       do {
           processPendingDerefs();
           // now get the next command to be processed, waiting if necessary
           result = getAndExecuteCommand();
   
           if (result < NO_ERROR && result != TIMED_OUT && result != -ECONNREFUSED && result != -EBADF) {
               ALOGE("getAndExecuteCommand(fd=%d) returned unexpected error %d, aborting",
                     mProcess->mDriverFD, result);
               abort();
           }
   
           // Let this thread exit the thread pool if it is no longer
           // needed and it is not the main process thread.
           if(result == TIMED_OUT && !isMain) {
               break;
           }
       } while (result != -ECONNREFUSED && result != -EBADF);
   
       LOG_THREADPOOL("**** THREAD %p (PID %d) IS LEAVING THE THREAD POOL err=%d\n",
           (void*)pthread_self(), getpid(), result);
   
       mOut.writeInt32(BC_EXIT_LOOPER);
       talkWithDriver(false);
   }
   ```

   ![image-20201006234048163](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-154049.png)

   首先往 mOut 里面一个 BC_ENTER_LOOPER （上面传下来的 isMain 是 ture），哪这个 mOut 是什么呢？ 这里介绍一个 `IPCThreadState` 里面有个两个 Parcel，一个是 mIn、一个是 mOut ，如果有什么数据要写到 binder 驱动，你就先把数据放到这个 mOut 里面，如果说 驱动有什么数据要返回回来，它这个数据就是会方法 mIn 里面。接下来会在一个循环调用 `getAndExecuteCommand();`这个循环一般是不会退出的，除非出现了 while 条件里面的哪两个错误。

![image-20201006234939975](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-154941.png)

首先会 talkWithDriver()，这个函数可能是向驱动写，也可能是向驱动读，不过对于 binder 线程来说主要还是等待从 binder 驱动发过来的 transcant 请求，就是从驱动读，然后读了之后再去回复这个请求；再往下看， 这里 mIn.readInt32() 会从 mIn 里面读一个指令出来，再去通过 executeCommmand(cmd) 处理这个指令， 它会根据 binder 驱动发过来的不同指令执行不同的操作就行了。



### 三、谈谈你对 binder 的理解

考察点：

1. binder 是干嘛的？
2. binder 存在的意义是什么？为什么不用别的替代方案呢？
3. binder 的架构原理又是怎么样的呢？

#### 1. binder 是干嘛的？

总的来说它就是用来通信的，binder 可以分成两端，一个是 Client 端、一个是 Server 端；Client 端和 Server 端可以在同一个进程，也可以不在同一个进程。Client 端可以向 Server 端发起远程调用，当然也可以传递数据，数据当做函数的参数来传递。特点是 远程调用时进程的边界是比较模糊的，你不用关心对方是在哪个进程。

可以自己思考一下 自己来实现一套远程调用机制的话，你会怎么做？

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-184615.png" alt="image-20201007024614110" style="zoom:25%;" />

答：Client 端要调用 Server 端的一个 call 函数

1. 首先 Client 端要将函数的参数序列化到一个 buffer 
2. 然后再通过 Linux 的各自跨进程通信方式把 buffer 传到 Server 端
3. 在 Server 端再把 buffer 反序列化，还原成各个参数，然后再去调用这个对应的函数
4. 最后在把这个函数的返回结果 原路返回到 Clinet 端

这里面要考虑几个问题： 1. 性能问题 （跨进程传递 buffer 的时候要能够快，尽量减少数据拷贝）2. 方便（因为 Linux 只提供最底层数据传输通道，相等于只有一个物理层，但是正真的跨进程调用涉及到的事太多了，就好比我只给了你一根电话线，你就能打电话吗？当然不行，你确实有了电话线之后呢，就有了消息的传输通道，我用知道你是在隔壁还是在千里之外，但是我说的话是声音啊，你要把它转成电信号，才能通过电信号发出去，而且你要知道发给谁，那么多人你知道它是谁了你还要知道怎么转发给他，这里又需要一个转接中心，而且对于接受方来说，突然电话线来个一股电流，你就光凭一股电流你就能知道是谁发了消息，又发了是什么消息？你得先把电信号转成声音信号，所以我们可以看到，就光凭一根电话线是远远不够的，上层还有好多好多工作要做，我们要在Linux 底层提供的跨进程通信传输机制上面再搭建一整套框架才行，不然的话应用层开发就太痛苦了）3. 安全 （就像打电话，只有电话进来我就要接，哪我是不是还有看看来号码对不对，或者说要有一个骚扰拦截机制等等）总之，一个完善的跨进程通信机制其实还是挺复杂的，性能要好，用起来要方便而且还非常的安全，binder 机制就是一套怎么好用的工具。

#### 2. binder 存在的意义是什么？为什么不用别的替代方案呢？

binder 是跑在驱动层的，它是在内核态，并没有使用 Linux 的跨进程通信机制，它是自己发明一套机制。

* **性能**  Linux 里面常用的一些跨进程通信机制，比如说管道、Socket，它们在跨进程通信的时候都是需要内存来做中转的，这个就意味这两次数据拷贝，一次从应用层拷贝到内核，还有一个成内核拷贝到应用层，但是 binder 是有区别的，binder 是把一块物理内存同时映射到内核和用户进程的内核空间，这样当你把数据拷贝到内核空间的时候，其实就相当于也拷贝到另一个进程的用户空间了，所以只用拷贝一次。
* **方便易用** 逻辑简单直接，共享内存虽然性能很好，但是用起来很复杂，远远没有我们这个 binder 好用
* **安全** 普通的 Linux 跨进程通信是非常不安全的，比如说像 Socket 它的 IP 地址都是开方的，别人知道它的 IP 地址就能连接它了，或者说 命名管道也是，你知道这个管道的名称了也能往里面写东西，这个是很容易被人恶意利用的。 主要是 **我们拿不动调用方 可靠的身份信息** ，这个身份信息你总不能说让调用方自己去填，这个明显是不可靠的。可靠的方式是怎么做呢？身份标记只能由 IPC 机制本身在内核态中添加。



#### 3. binder 的架构原理又是怎么样的呢？

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-104122.png" alt="image-20201006184121476" style="zoom:33%;" />

1. 一共有四方参与: Client 端、Server 端、ServiceManger端、binder 驱动。
2. 上面是展示的是 系统服务的 binder 通信，只有系统服务才能注册到 ServiceManger 中，应用层的服务是不能注册到 ServiceManger 中的，通过不了权限验证的。
3. Client 是应用进程；Server 是系统服务，它可能是跑在 SystemServer 进程也可能是单独的进程；ServiceManger 是一个单独的系统进程。这里无论哪个进程它们在启动的时候第一件事都是要启动 binder 进制，这个是 binder 通信的前提。



#### 怎么启动 binder 机制？

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-160154.png" alt="image-20201007000152294" style="zoom:25%;" />

* 打开 Binder 驱动 （Binder 驱动就会为这个进程创立一套档案）
* 内存映射、分配缓存区（将上一步返回的 描述符 进行内存映射、分配缓存区，接下来的 Binder 通信要用到这个缓存区）
* 启动 Binder 线程（启动binder 线程一方面是要把这个线程注册到 binder 驱动， 另一方面这个线程要进入 Loop 循环，不断跟 binder 驱动进行交互）

##### binder 通信

frameworks/native/cmds/servicemanager/service_manager.c ![image-20201007035647902](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-195649.png)

上面是 ServiceManger 的入口函数 main 函数

1. 打开 binder 驱动，映射内存
2. `binder_become_context_manager（bs）` binder 成为了上线文的管理者，其实就是告诉 binder 驱动我就是 ServiceManger，我就是那个中转站，无论是注册函数查询都一个来找我
3. 进入 binder loop 循环 

![image-20201007040832312](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-200834.png)

1. 这个函数首先将当前线程注册成 binder 线程，BC_ENTER_LOOPER 把这个指令写到 binder 驱动 ，就表示把当前线程注册成 binder 线程，此时线程就是 ServiceManager 的主线程。
2. 然后在一个 for 循环里面，bwr.read_size > 0 是读，把 binder 驱动发过来的数据读到 bwr 中，就是请求读进来
3. 读进来之后，解析这个请求，然后再去调这个 func 回调函数，去处理这个请求。

ServiceManger 启动 binder 机制之后进入一个 Loop 循环，等待 Client 端、Server 端的请求，Server 是系统服务，Client 端一般是应用程序，系统服务启动完之后才是应用启动，那么就是 Server 端先和 ServiceManger 进行交互的，Server 启动的时候，它要把自己的 binder 对象注册到 ServiceManger，我们看一下代码：

以一个系统服务 SurfaceFlinger 为例看一下它是怎么注册到 ServiceManger 中的：

![image-20201007042809815](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-202811.png)

上面是 SurfaceFlinger 的入口函数即这个 main 函数，

1. 启动 binder 机制，映射内存，启动 binder 线程；
2. 下面就是 binder 对象的初始化了，对于 SurfaceFlinger 来说它的业务类对象就是 SurfaceFlinger ,它同时也是一个 binder 实体对象。
3. 向 ServiceManger 注册。首先通过 `defaultServiceManager()` 拿到 ServiceManger 的 bpbinder，然后发起 `addService` 调用，把 flinger 这个对象传到 ServiceManager。最后进入一个 Loop 循环。

怎么获取 ServiceManger 获取 binder 对象？

![image-20201007043600692](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-203602.png)

1. gDefaultServiceManager 只要初始一次
2.   getContextObject 是正真获取 ServiceManger 的 binder 对象的

```c++
sp<IBinder> ProcessState::getContextObject(const sp<IBinder>& /*caller*/)
{
    return getStrongProxyForHandle(0);
}
```

3. `getStrongProxyForHandle(0)` 它查的是 0 号 handler 值对应的 binder 引用。 如果没有查到就说明可能 ServiceManger 自己还没有注册到 binder 驱动。那么从上面的代码来看 sleep(1) ，等一会在去获取在试一试，直到获取的 ServiceManger 的 binder 对象

##### addService 的实现

![image-20201007045033207](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-205034.png)

1. data 是发到驱动的参数，reply 是驱动返回的结果
2. 然后把参数都放进 data 里面，包括这个 finlinger 系统服务的 binder 对象，也写到 Parcel 里面通过 `writeStrongBinder(service)` 方法。
3. `remote() ` 拿到 ServiceManger 的 binderProxy 对象，然后调用它的 transact(ADD_SERVICE_TRANSACTION, data, &reply) 把这个请求发出去，这个请求的请求码是 ADD_SERVICE_TRANSACTION。

```c++
status_t IPCThreadState::transact(int32_t handle,
                                  uint32_t code, const Parcel& data,
                                  Parcel* reply, uint32_t flags)
```

![image-20201007050018786](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-210020.png)

底层在跟  驱动交换的时候它是不分 bpBinder 还是 binderProxy 的，它其实只认这个 handler 值，所以我们看上层封装的这么一层层对象它的核心就是这么一个 handle 值。

1. ```C++
    err = writeTransactionData(BC_TRANSACTION_SG, flags, handle, code, data, nullptr);
   ```

   就是把要写到 binder 驱动的数据准备好，我们要知道 Pacel 这个数据结构 binder 驱动是不认识的，所以得先把它转成 binder 驱动认识的数据，再发给 binder 驱动，就是 BinderTransactionData 这个数据结构。

2. 如果是 One_way 的话就不用等回复了：`err = waitForResponse(nullptr, nullptr);`

3. 如果不是 One_way ，哪就要带一个 reply 来接受回复

4. waitForResponse 就是和 binder 驱动进行交互，走通信协议的，具体实现先不说。

##### 我们看请求到了 Service 端是怎么处理的

![image-20201007051115905](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-211117.png)

上面代码就是 ServiceManger 的 Server 端处理了，binder 的 Server 端处理请求都是在 OnTransact 函数里面

1. 根据 Code 找到 ADD_SERVICE_TRANSACTION: 这个 switch case。
2. 然后从 Pacel 里面把这个系统服务的 binder 读出来，这里读出来其实就是根据 binder 引用的 handle 值封装的一个 binderProxy 对象而已。
3. 再调用本地的 addService 函数把它存好，存好了之后往这个 reply 里面写一个 返回值，Ser





### 四、一次完整的 IPC 通信流程是怎么样的

考察点：

1. 了解 binder 的整体架构原理
2. 了解 应用 和 biner 驱动的交互方式（两块：1. Client 端和 binder 驱动的交互 2. Server 端和 binder 驱动的交互）
3. 了解 IPC 过程中的通信协议

#### binder 的分层架构

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-06-211630.png" alt="image-20201007051628007" style="zoom:33%;" />

这个图可以分成几个维度来看，首先从角色维度来看：一个是 Client 、一个是 Server 、还有一个是 binder 	驱动；分层的角度来看，它有可以分成三层：1. 应用层 2.framework 层（framework 层又分成 Java 层和 native 层） 3. 驱动层；从 binder 对象角度来看 它也可以分成两端：1. 代理端 2. 实体端，从 Client 端开始，首先从 Client 端拿到一个 Proxy 发起 IPC 调用，这个其实是将请求丢到了 BinderProxy ，然后这个请求继续 往下丢，丢到 native 层的 BpBinder，然后这个又会继续往下丢，丢到了 IPCThreadState 的 transact，这个 transact 就把请求丢给驱动，驱动再转发到 Server 进程然后就会在这个 Server 进程的 binder 线程处理，执行它的 onTransact  ，然后这个请求又会一层一层的往上传，传到 BBiner -> Binder -> 再传到业务层，传给这个业务层的接口对象 Stub 。总的来说这个分层跟网络传输分层的结构有点像，可以进行对比一下。

接下来我们看一下这个请求是怎么到 Server 端的：

![image-20201007122604036](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-042605.png)

1. Client 端就是这个 Proxy，这个 Proxy 里面的 binder 调用都是先把参数写到 Parcle 里面
2. 然后再通过 mRemote，transact 出去，这个 mRemote 就是 BinderProxy 对象

![image-20201007122912143](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-042914.png)

这个 BinderProxy 的 transact 是怎么实现的：

这个 transact 调的了一个 native 层的函数

![image-20201007123028663](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-043029.png)

上图就是 BinderProxy 的 transact 在 native 层的实现：

1.  首先拿到 native 层的 Parcel 
2. 然后根据 BinderProxy 对象 拿到它 native 对应的 binder 对象 ，也就是 BpBinder，这个请求再通过 BpBinder transact 出去

![image-20201007124513030](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-044514.png)

BpBinder 的 transact 函数它又继续把这个请求交给了 IPCThreadState ，transact 出去了：

1. IPCThreadState::self() 你可把它理解成线程类的单例 ，就是没有线程只有一个 IPCThreadState
2. 然后这个 transact 第一个参数 mHandle ，我们和驱动打交道的时候，我们传这个 mHandle 就行了，驱动根据这个 mHandle 就知道对应的是那个 binder 引用，再根据这个 binder 引用再找到这个 binder 实体对象

![image-20201007125135708](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-045137.png)

1. writeTransctionData 把数据写出去
2. waitForResponse 等待回复，我们这里只考虑不是 one_way 的情况，所以这个 waitForResPonse 是带 reply 的。

![image-20201007141015622](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-061016.png)

看一下 writeTransactionData 是怎么把数据写出去的

1. 首先声明一个 binder_transaction_data 这么一个数据结构，然后这个数据机构里面有很多字段，我们要把传进来的参数都一个赋给这些字段，data 是 Parcel ；把 Parcel 的 dataSize 赋给 tr.data_size，把 Parcel 的缓存区赋给 buffer, 把 Parcel 的偏移 ipc.Objects() 赋给 offsets ，这是什么偏移，这就是 Parcel 里面 binder ：flattenBinderObject 它的偏移。
2. 赋完值以后就要把这个值写到 mOut 里面，这个 IPCThreadState 里面有两个 Parcel，一个是 mIn ，一个是 mOut，就是如果要往 binder 驱动写东西，那么就要先写到 mOut 里面，如果是从 binder 驱动读东西，它就先读到 mIn；所以这里 先往 mOut 里面写了一个 cmd，一个命令，然后再把这个 binder_transaction_data 写到mOut里面去。

我们再接着往下看：

![image-20201007142001659](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-062003.png)

发完了数据之后，这个函数要等待回复，这里有两个问题要注意一下：1. 上面那个 binder_transaction_data，它其实没有真正的把数据发给驱动，它只是把数据写到 mOut 这个 Parcel 里面而已。所以我们这个 waitForResponse 里面不光是等待回复，它其实里面有一个 talkWithDriver()，和驱动通信的过程，对话的过程。这个对话分为两种：一个是往驱动里面写，一个是从驱动里面读，因为上一步我们已经把数据写到 mOut 里面了，所以它先是写数据到 binder 驱动，2. 我们注意到 这个方法里面是一个 while 循环，看起来是像死循环，其实不是，为什么这里要有一个循环，因为一个完整的 transction 它是分好几个步骤的，它不是你 transaction 出去就完事了，它还要多次回复，多次来往，这么一个过程，所以要在这个循环里面来处理整个通信的协议，等所以的协议走完之后，它就会跳出这个循环了，我们看 goto finish 了，最后一个就是 BR_REPLY 了，这个 BR_REPLY 就表示这个 transaction 结束了 ，Client 端也收到这个 Server 端发过来的 Reply 了，这时候就可以 goto finish 了，整个协议流程就结束了。

方法流程：

1. 首先它会 talkWithDriver(); 接着上边的那个 writeTransactionData 之后，talkWithDriver 其实就是往驱动里面写东西
2. 写完东西之后驱动就回复了，回复了什么-> 就是从 mIn 里面把指令读出来 cmd ，然后根据指令来执行不同的操作 
3. BR_TRANSACTION_COMPLETE 是驱动返回的一个回值，表示说 驱动已经收到 Client 端的 transact 请求了
4. BR_REPLY 表示 Client 端已经收到 Server 端的 Reply 了，这个时候 transaction 就要结束了，首先这个驱动返回的数据是存在 mIn 这个 Parcel 里面，现在要把它读到这个 binder_transaction_data 里面，读出来之后，然后 ipcSetDataReference 就是说把 binder_transaction_data 里面的数据 在把它赋给 reply，这样我们就可以从 reply 拿到数据了

我们接着往下看

![image-20201007144008158](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-064010.png)

1. 这里面先有个一个数据结构 binder_write_read ，这个数据结构是干嘛的呢？正真跟驱动交互的时候是通过这个 ioctl 这个系统调用，这个系统调用它就需要传递这么一个数据结构，它没有传什么 Parcel 啊、binder_transaction_data 这种东西。
2. binder_write_read 里面有这么几个变量 write_size、write_buffer    read_szie 、 read_buffer ，如果你要往驱动里面写 你就要指明你要写的这个缓存区，和数据的大小，你如果要从 binder 驱动里面读 你也要指定 缓存区 以及缓存区的大小

我们继续往下看

![image-20201007144706073](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-064707.png)

这个 ioctl 这个系统调用到了 binder 驱动层执行的就是上面的函数

1. 函数会根据 cmd 参数，来执行不同的操作，这个是一个 switch-case
2. BINDER_WRITE_READ  这个命令里面：首先它看这个 write_size 是不是 > 0，如果 > 0 就会调用这个 binder_thread_write 去正真的写，如果 read_size > 0 ，再调用 binder_thread_read 来读，所以我们看到这个 写的优先级是大于读的，就是有什么东西要写的话，那么就是先把数据写出去再说，之后才是来读，所以这个我们注意。

我们继续往下看，我们刚讲完了 Client 端怎么跟驱动交互的，现在我们再来看一下 Server 端，看一下它是怎么和驱动交互的

![image-20201007150350136](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-070351.png)

1. Server 端注意是通过 binder 线程和驱动进程交互的，这个 binder 线程其实就是一个普通的线程，然后调用了 IPCThreadStata::self() -> joinTheadPool 把这个线程注册到 binder 驱动，这样它就能成为一个 binder 线程
2. joinTheadPool 里面首先要往 mOut 里面写一个 BC_ENTER_LOOP 或者是是 BC_REGISGER_LOOP，BC_ENTER_LOOP 表示这个线程自己主动 要把自己注册到 binder 驱动，BC_REGISGER_LOOP 是被动的，就是又 binder 驱动先申请，然后由应用来注册，
3. 在注册之后，它在一个 while 循环里面不停的 getAndExecuteCommand  从这个函数的名字上我们知道它是不停的去取指令处理指令，这个 while 循环一般是不会退出的 ，除非遇到什么错误。

继续往里看：  getAndExecuteCommand

![image-20201007150705842](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-070707.png)

1. talkWithDriver 从驱动读取请求
2. 处理这个请求，调用 executeCommand(cmd)，在处理请求之前还要把这个 cmd 从 mIn 把指令读出来，然后根据指令来执行不同的操作

![image-20201007150651847](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-070653.png)

1. 根据不同的 cmd，执行不同的 case
2.   我们主要关注 BR_TRANSCTION 这个 case，这个 case 是 Client 把请求发到驱动，驱动在把请求转发个 Server 端，转发到 Server 端是时候 Server 端就会收到 BR_TRANSCTION 这个指令，收到这个指令之后
3. 首先把数据从这个 mIn 里面读出来，放到 binder_transaction_data  里面，放完之后再准备两个 Parcel ,一个是 buffer 一个是 reply，这 buffer 其实就是要把它传递到Server 端上层去，相当于参数，reply 就是回复给 Client 端的，我们先把 参数的 buffer 准备好
4. 准备好了之后 ，从 tr.cookie ，它保存的是 binder 实体对象，因为这里是 native 层，也就是 binder 对象在native 层的表现 BBinder
5. 然后在 调用 Bbinder 的 transact 把请求往上传，传到上层执行完之后，就是 sendReply，就把这个回复 Client 端的 reply 发出去

![image-20201007151721249](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-071722.png)

sendReply 其实就是把这个reply 数据写到驱动里面，然后驱动再转发给 Clinet 端，这个协议是 BC_REPLY 指令，waitForReponse(NULL,NULL) 两个参数都是 NULL ，跟 one_way 有点像， 就是不用再等待对方回复了。

继续看 BBinder 它的 transact 函数，看一下它是怎么把这个请求往上传的。

![image-20201007152019354](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-072020.png)

它调用的是 onTransact ，为什么调用 onTransact 还没有人知道：

我们再看这个 onTransact 函数

![image-20201007152314664](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-072317.png)

1. 首先它根据 JavaVm 拿到当前线程的 JNIENv
2. 拿到 JNIENv 就可以 发起 JNI 调用了，调的是 binder 对象的 execTransact 函数

![image-20201007152608733](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-072610.png)

 execTransact 函数 调到了binder 对象的 onTransact 函数

![image-20201007152646540](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-072652.png)

onTransact 函数其实就是我们的 AIDL 生成的 Stub 类它的 onTransact 函数，因为我们的 Stub 类就是继承了 binder 对象，

根据 code ，code 就是你调用的函数的函数码，首先从 Parcel 里面把 binder 对象读出来，binder 对象是 Client 端发出来的要发布给Server端，所以在 Server 端先把它读出来，读出来再把它转换成一个，业务接口对象，在这里我们注意，这个 binder 对象它到了 Server 端就变成了一个 binderProxy 了，任何执行 Server 端的逻辑。

##### 协议的通信过程，下图

![image-20201007153404527](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-073406.png)

一个简单的 transction 

1.  首先从 Client 端发起 BC_TRANSACTION ，就 Client 向 Server 端发起一个 IPC 调用的时候，它是首先先 biner 驱动写了一个  BC_TRANSACTION 指令

2. binder 驱动收到后会给它一个回执，就是 BR_TRANSACTION_COMPLETE，回执发完了之后 ,binder 驱动把这个请求转发个 Server 端通过 BR_TRANSATION

3. Server 端收到之后，去处理这个请求，处理完了之后会回 binder 驱动 BC_REPLY，binder 驱动收到了 Server 端的回复之后也会给 Server 端一个回执，也是这个 BR_TRANSACTION_COMPLETE,

4. 最后 binder 驱动再把这个 返回结果转发给 Client 端，通过 BR_REPLY。

   

   整体流程就是上面这样：有几个状态，就 Clinet 端发出请求之后，在等待 Server 端回复的过程中，处于休眠状态，另外 Server 端因为是 binder 线程 ，这个 binder 线程在处理请求之外，在没有处理请求的时候它也处于休眠状态，等待 binder 驱动发过来请求。



![image-20201007154541663](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-074542.png)

面试时的思路



### 五 binder 对象的传递

考察点：

1. binder 传递的方式有哪些？
2. binder 传递过程中是怎么存储的？
3. Binder 的序列号和反序列化过程？(首先它是怎么打包的，到目标进程又是怎么还原的)
4. binder 对象传递过程中 驱动做了什么？（为什么我们传递的时候是一个 binder 实体对象，但是到了目标进程收到的确是一个 binder 代理对象，这里面有什么门到没有？）

为了分析这个传递原理我们从一个 AIDL 例子来入手：

```java
// IRemoteCaller.aidl
package com.example.binderdemo;

// Declare any non-default types here with import statements
import com.example.binderdemo.ICallback;
interface IRemoteCaller {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
  void publishBinder(ICallback callback);
}
```

我们定义了一个 AIDL 接口，这个接口的主要作用就是发布 binder，把这个 ICallback 这个 binder 对象从一个进程发布到另外一个进程。

```jajva
// ICallback.aidl
package com.example.binderdemo;

// Declare any non-default types here with import statements

interface ICallback {
    void onCallback(int code);
}
```

这个 ICallback 就是一个普通的 binder 对象。

接下来生成 AIDL 接口对应的 对应的 Java 类 

```java
@Override
public void publishBinder(com.example.binderdemo.ICallback callback) throws android.os.RemoteException {
    android.os.Parcel _data = android.os.Parcel.obtain();
    android.os.Parcel _reply = android.os.Parcel.obtain();
    try {
        _data.writeInterfaceToken(DESCRIPTOR);
        _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
        boolean _status = mRemote.transact(Stub.TRANSACTION_publishBinder, _data, _reply, 0);
        if (!_status && getDefaultImpl() != null) {
            getDefaultImpl().publishBinder(callback);
            return;
        }
        _reply.readException();
    } finally {
        _reply.recycle();
        _data.recycle();
    }
}
```

这个函数就是生成的 Java 类，Proxy 类它的 pulishBinder 方法实现。

1. 创建一个 Parcel 的 data 
2. 把 callback.asBinde() 即这个 binder 对象 通过 writeStrongBinder 这个函数把 它写到 Parcel 里面，然后再 在把它发出去，发出去带上 data 

```java
@Override
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
    java.lang.String descriptor = DESCRIPTOR;
    switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(descriptor);
            return true;
        }
        case TRANSACTION_publishBinder: {
            data.enforceInterface(descriptor);
            com.example.binderdemo.ICallback _arg0;
            _arg0 = com.example.binderdemo.ICallback.Stub.asInterface(data.readStrongBinder());
            this.publishBinder(_arg0);
            reply.writeNoException();
            return true;
        }
        default: {
            return super.onTransact(code, data, reply, flags);
        }
    }
}
```

刚才是 Proxy 端把请求发出去，发出去然后进过驱动转发，就会到实体端，也就是 Server 端，就会收到这个 onTransact 回调，这个 onTransact 里面：

1. 把binder 驱动转发过来的 Parcel，通过 data.readStrongBinder() 把 binder 对象读出来，读出来再通过 

com.example.binderdemo.ICallback.Stub.asInterface ，将 binder 对象转成 接口，就下来就是 服务端的处理逻辑了 。

我们具体看一下 writeStrongBinder 和 readStrongBinder 具体是怎么传递 binder 对象的？

```java
/**
 * Write an object into the parcel at the current dataPosition(),
 * growing dataCapacity() if needed.
 */
public final void writeStrongBinder(IBinder val) {
    nativeWriteStrongBinder(mNativePtr, val);
}
```

writeStrongBinder 里面调用了一个 native 方法：nativeWriteStrongBinder(mNativePtr,val)

mNativePtr 是就是 Parcel 在 native 层对应的 Parcel 对象的指针，这里把这个指针 保存到 Java 对象里面了。

![image-20201007194254610](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-07-114255.png)

1. 首先根据指针拿到 native 层的 Parcel 对象。
2. 然后 ibinderForJavaObject(env,obj)，我们传递进来的是 java 层的 binder 对象，这个方法要根据 obj 找到 native 层对应的 binder 对象，然后 把 native 层的 binder 对象 通过 native 层的 writeStrongBinder 方法，写到 native 层的 Parcel 对象里面 









