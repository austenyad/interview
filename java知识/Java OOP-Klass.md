### Java OOP-Klass

JVM 本身是用 C++ 实现的，一个 Java 对象是如何映射到 C 层呢？

> 最简单的做法是为没有个 Java 类生成一个结构相同 C++ 类与之对应。

但 HotSpot JVM 并没有这么做，而是设计了一个 OOP-Klass Model。这里的 OPP 指的是 Ordinary Object Pointer (普通对象指针)，它用来表示对象的实例信息。而 Klass 则是包含数据和方法信息，用来描述 Java 类。

* foo 是一个局部方法中的引用，被保存在虚拟机栈中
* staticValue 静态变量在类加载阶段被保存咋方法区，并被赋值
* localValue 实例变量在创建对象时才会被创建并赋值
* 一个 Java 对象在 JVM 中被分成两个部分：OOP 和 Klass 。其中 OOP 对象保存对象里实例数据，Klass 用来描述类相关信息及静态变量。

一个 Java 对象 在内存中的分布分为 3 部分： 对象头、实例数据、对齐填充。当我们在 Java 代码中，使用 new 创建一个对象的时候，JVM 会在堆中创建一个 instanceOopDesc 类，这个对象中包含了对象头以及实例数据。

instanceOopDesc 的基类为 oopDesc。它的结构如下：

```c++
class oopDesc{
    friend class VMStructs;
    private:
    	volatile markOop _mark;
    	union _metadata{
            wideKlassOop _klass;
            narrowOop _compressed_klass;
        } _metadata;
}
```

> C++ union 共用体，也叫做联合体。
>
> 结构体 和 共用体 的区别在于：结构体的各个成员变量会占用不同的内存，互相之间没有影响；结构体占用的内存空间等于所有成员占用内存的总和（成员之间可能会存在缝隙），共用体占用内存等于最长的成员占用的内存。共用体使用内存覆盖技术，同一时刻只能保存一个成员的值，如果对新的成员赋值，就会把原来的成员值覆盖掉。
>
> 而共用体的所有成员占同一段内存，修改一个成员会影响其余所有成员。、

其中 _mark 和 _metadata 一起组成对象头。\_metadata 主要保存了类元素数据。

_mark 是 markOop 类型数据，一般称它为标记字段（Mark Word）,其中主要存储类对象的 hashCode 、分代年龄、锁标志位，是否偏向锁等。

下图是一张 32 位 Java 虚拟机的 Mark Word 等默认存储结构如下：

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20200618204051.png)

默认情况下，没有线程进行加锁操作，所以锁对象中的 Mark Word 处于无锁状态