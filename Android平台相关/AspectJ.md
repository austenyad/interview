#### JointPoint

是AspectJ中最关键的一个概念，什么是JPoints呢？JPoints就是程序运行时的一些*执行点*。那么，一个程序中，哪些执行点是JPoints呢？比如：

1.  一个函数的调用可以是一个JPoint。
2. 设置一个变量，或者读取一个变量，也可以是一个JPoint。
3. for循环可以看做是JPoint。

理论上说，一个程序中很多地方都可以被看做是JPoint，但是AspectJ中，只有如表1所示的几种执行点被认为是JPoints：

| **Join Points**           |                                  |                                                              |
| ------------------------- | -------------------------------- | ------------------------------------------------------------ |
| **method call**           | 函数调用                         | 比如调用Log.e()，这是一处JPoint                              |
| **method execution**      | 函数执行                         | 比如Log.e()的执行内部，是一处JPoint。注意它和method call的区别。method call是调用某个函数的地方。而execution是某个函数执行的内部。 |
| **constructor call**      | 构造函数调用                     | 和method call类似                                            |
| **constructor execution** | 构造函数执行                     | 和method execution类似                                       |
| **field get**             | 获取某个变量                     | 比如读取DemoActivity.debug成员                               |
| **field set**             | 设置某个变量                     | 比如设置DemoActivity.debug成员                               |
| **pre-initialization**    | Object在构造函数中做得一些工作。 | 很少使用，详情见下面的例子                                   |
| **initialization**        | Object在构造函数中做得工作       | 详情见下面的例子                                             |
| **static initialization** | 类初始化                         | 比如类的static{}                                             |
| **handler**               | 异常处理                         | 比如try catch(xxx)中，对应catch内的执行                      |
| **advice execution**      | 这个是AspectJ的内容，稍后再说    |                                                              |

### ajc - compiler and bytecode weaver for the AspectJ and Java languages

`ajc` [*`Options`*] [*`file...`* | @*`file...`* | -argfile *`file...`*]

ajc --- 使用 ajc 编译器可将 AspectJ 编制文件（.aj）和 java（.class）文件，它可以在编译和字节码编制，并支持增量构建，它也可以在运行时使用 “加载时编制” 来编译植入字节码。

Options --- 指定要编译的源文件。



