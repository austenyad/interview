在每个线程内部都有一个名为 `threadLocals` 的成员变量，该变量的类型为 HashMap ，其中 key 为我们定义的 ThreadLocal 变量的 this 引用，value 则为我们使用 set 方法设置的值。每个线程的本地变量存放在线程自己的内存变量 threadLocals 中，如果当前线程一直不消亡，那么本地变量会一直存在，所以可能会造成内存泄漏，因此使用完毕后要记得调用 ThreadLocal 的 remove 方法删除对应线程的 threadLocals 中的本地变量。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-07-30-001142.png" alt="image-20200730081140612" style="zoom:50%;" />



### ThreadLocal 不支持继承性

```java
  // 1. 创建线程变量
   static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    public static void main(String[] args) {
        //2 设置线程变量
        threadLocal.set("Hello World");
        //3 启动子线程
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                //4 子线输出线程的变量值
                System.out.println("thread:"+ threadLocal.get());
            }
        });
        thread.start();

        //5. 主线程输出线程变量的值
        System.out.println("main:"+ threadLocal.get());
    }

```

输入结果：

main:Hello World

thread:null

也就是说，同一个 ThreadLocal 变量在父线程中被设置后，在子线程中是获取不到的。根据上面 ThreadLocal 的源码介绍，这属于正常现象。

那么有没有办法让子线程能访问到父线程中的值呢？

