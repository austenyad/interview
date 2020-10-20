# 一、Class 文件结构

### Class 字节码 无关性的基石

"**与平台无关**" 的理想最终实现在操作系统的应用层上：Sun 公司以及其他虚拟机提供商发布了许多可以运行在不同平台上的虚拟机，这些虚拟机都可以载入同一种平台无关的字节码，从而实现了程序的 "**一次编写，到处运行**"。

各个不同平台的虚拟机与所有平台都统一使用的程序储存格式 —— 字节码，是构成平台无关性的基石。Java 虚拟机不和包括 Java 在内的任何语言绑定，它 **只与 "Class 文件" 这种特定的二进制文件格式所关联，Class 文件中包含了虚拟机指令集合符号表以及若干其他辅助信息**

虚拟机并不关心 Class 的来源是何种语言，**有了字节码，也解除了 Java 虚拟机和 Java 语言之间的耦合。**

Java 语言中的 各种变量、关键字 和 运算符号 的语义最终都是有多条字节码命令组合而成的，因此字节码命令所能提供的语义描述能力肯定会比 Java 语言本身更强大。因此，有些 Java 语言本身无法有效支持的语言特性不代表 字节码本身无法有效支持，这也为其他实现一些有别于 Java 的语言特性提供了基础。

**Class 文件是一组以 8 位字节为基础的二进制流，各个数据项目严格按照顺序紧凑地排列在 Class 文件中，中间没有任何分割符，这使得整个 Class 文件中存储的内容几乎 全部是程序运行时的必要数据，没有空隙存在。**

当遇到 需要 占用 8 位字节以上空间的数据项时，则会按照高位在前的方式分割成若干个 8 位字节进行存储。这种 顺序称为 "**Big-Endian**"，具体是指高位在地址最低位、最低位字节在地址最高位的顺序来存储数据，而 x86 等处理器则是是使用了相反的 "**Little-Endian**" 顺序来存储数据。



根据 JVM 规范的规定，**Class 文件格式采用了一种类似于 C 语言结构体的伪结构来 存储数据，而这种 伪结构 中只有两种数据类型：**

**无符号数** 和 **表**

##### 无符号数

无符号数 属于基本的数据类型，以 u1、u2、u4、u8 来分别代表 1 个字节、2 个字节、4 个字节 和 8 个字节的无符号数，无符号数可以用来描述 数字、索引引用、数量值 或者 按照UTF-8编码构成的字符串值。 例如 u4 ： 表示能够保存4个字节的无符号整数，其他同理。

##### 表

表是由 **多个无符号数** 或者 **其他表** 作为数据项构成的复合数据类型，所有表都习惯性地以 "_info" 结尾。表用于描述有层次关系的复合结构的数据，整个 Class 文件本质上就是一张表。



### Class 文件格式

```
 ClassFile { 
        u4 magic;  // 魔法数字，表明当前文件是.class文件，固定0xCAFEBABE
        u2 minor_version; // 分别为Class文件的副版本和主版本
        u2 major_version; 
        u2 constant_pool_count; // 常量池计数
        cp_info constant_pool[constant_pool_count-1];  // 常量池内容
        u2 access_flags; // 类访问标识
        u2 this_class; // 当前类
        u2 super_class; // 父类
        u2 interfaces_count; // 实现的接口数
        u2 interfaces[interfaces_count]; // 实现接口信息
        u2 fields_count; // 字段数量
        field_info fields[fields_count]; // 包含的字段信息 
        u2 methods_count; // 方法数量
        method_info methods[methods_count]; // 包含的方法信息
        u2 attributes_count;  // 属性数量
        attribute_info attributes[attributes_count]; // 各种属性
    }
```

* 4）、`constant_pool_count` ：常量池数组元素个数。
* 5）、`constant_pool` ：常量池，是一个储存了 cp_info 信息的数组，每个 Class 文件都有一个与之对应的常量池。（注意：cp_info 数组的索引从 1 开始）
* 6）、`access_flags`: 表示当前类的访问权限，例如： public、private。
* 7）、`this_class 和 super_class`： 存储了指向常量池数组元素的索引，this_class 中索引内容为当前类名，而 super_class 中索引指向其父类类名。
* 8）、`interface_count 和 interfaces`：同上，它们储存的也只是指向常量池数组元素的索引。其类容分别表示当前类实现了多少个接口和对应的接口类名。
* 9）、`fields_count 和 fields`: 表示成员变量的数量和其信息，信息是由 field_info 结构体表示。
* 10）、`methods_count 和 methods`：表示成员函数数量和它们的信息，信息由 method_info 结构体表示。
* 11）、`attributes_count 和 attributes` ：表示当前类的属性信息，每个属性都有一个与之对应的 `attribute_info` 结构体。常见的属性信息如 调试信息、它需要记录某句代码对应源代码的那一行，此外，如函数对应的 JVM 字节码、注解信息也是属性信息。



#### 1.魔数

#### 2.常量池

常量池是重点要掌握的

常量池可以理解为 **Class 文件之中的资源仓库，其它的几种结构或多或少都会最终指向这个资源仓库之中。**

常量池是 Class 文件结构中与其他项 关联最多 的数据类型，也是 占用 Class 文件空间最大的数据项之一，同时它还是 在 Class 文件中第一个出现的表类型的数据项。因此没有充分的了解常量池，后面其他的 Class 表类型数据项的学习会变得举步维艰。

假设一个常量池的容量（偏移地址:0x00000008）为十六进制数 0x0016，即十进制 22，这个代表常量池中有 21 项常量，索引值范围为 1~ 21 。**在 Class 文件格式规范制定时，设计者将 0 项常量空出来是有特殊考虑的，这样做的目的在于 满足后面某些指向常量池的索引值的数据在特定情况下需要表达 “不引用任何常量项” 的含义。**

常量池主要存放两大类常量：**1. 字面量（Literal）和 2. 符合引用（Symbolic References）**

##### 1. 字面量（Literal）

字面量比较接近 Java 语言层面的常量概念，如文本字符串、生命为 final 的常量值等

##### 2. 符号引用（Symbolic References）

而 符合引用 则属于 **编译原理** 方面的概念，包括了 **三类常量**，如下所示：

* 1）、类和接口的 全限定名（Fully Qualified Name）
* 2）、字段的名称和描述符（Descriptor）
* 3）、方法的名称和描述符

此外，**在虚拟机加载 Class 文件的时候会进行动态链接，因此其字段、方法的符合引用不经过运行期转换的话就无法得到真正的内存入口地址，也就是无法直接被虚拟机使用。当虚拟机运行时，需要从常量池获取对应的符号引用，再在类创建或运行时进行解析，并翻译到具体的内存地址只中。**

**constant_pool** 中存储了一个一个的 cp_info 信息，并且每个 cp_info 的第一个字节（即一个 u1 类型的标志位）标识了当前**常量池中 常量项的 类型**，其后才是具体的常量项内容。

下面是，**常量项的类型**，如下表：

| 类型                             | 标志 |                             描述                             |
| -------------------------------- | :--: | :----------------------------------------------------------: |
| CONSTANT_Utf8_info               |  1   |    用于存储 UTF-8 编码的字符串，它真正包含了字符串的内容     |
| CONSTANT_Integer_info            |  3   |                     表示int型数据的信息                      |
| CONSTANT_Float_info              |  4   |                    表示float型数据的信息                     |
| CONSTANT_Long_info               |  5   |                     表示long型数据的信息                     |
| CONSTANT_Double_info             |  6   |                    表示double型数据的信息                    |
| CONSTANT_Class_info              |  7   |                      表示类或接口的信息                      |
| CONSTANT_String_info             |  8   | 表示字符串，但该常量项本身不存储字符串的内容，它仅仅只存储了一个索引值 |
| CONSTANT_Fieldref_info           |  9   |                        字段的符号引用                        |
| CONSTANT_Methodref_info          |  10  |                      类中方法的符号引用                      |
| CONSTANT_InterfaceMethodref_info |  11  |                     接口中方法的符号引用                     |
| CONSTANT_NameAndType_info        |  12  |              描述类的成员域或成员方法相关的信息              |
| CONSTANT_MethodHandle_info       |  15  |                表示方法句柄信息，其和反射相关                |
| CONSTANT_MethodType_info         |  16  |        标识方法类型，仅包含方法的参数类型和返回值类型        |
| CONSTANT_InvokeDynamic_info      |  17  | 表示一个动态方法调用点，用于 invokeDynamic 指令，Java 7引入  |

然后，我们需要了解其中涉及到的重点常量类型。这里我们需要先明白 CONSTANT_String 和 CONSTANT_Utf8 的区别。

##### CONSTANT_String 和 CONSTANT_Utf8 的区别

* `CONSTANT_Utf8  `：真正存储了字符串的内容，其对应的数据结构中有一个字节数组，字符串便酝酿在其中。
* `CONSTANT_String `：本身不包含字符串内容，但其具有一个指向 CONSTANT_Utf8 常量项 的索引。

我们必须了解的是，**在所有常见的常量之中，只要是需要表示字符串的地方其实际都包含有一个指向 CONSTANT_Utf8_info 元素的索引。而一个字符串最大长度即 u2 所能表示的最大值为 65525，但是需要使用 2 个字节保存 null 值，所以一个字符串的最大长度为 65534。**

对于常见的常量项来说一般可以细分为如下 **三个维度**

##### 常量项 UTF-8

常量项 UTF-8 的数据结构如下：

```
  CONSTANT_Utf8_info {
        u1 tag; 
        u2 length; 
        u1 bytes[length]; 
  }
```

其元素含义如下所示：

1）、`tag`：值为 1，表示是 CONSTANT_Utf8_info 类型表。

2）、`length`: length 表示 bytes 的长度，比如 length = 10，则表示接下来的数据是 10 个连续的 u1 类型的数据。

3）、`bytes`: u1 类型的数组，保存真正的常量数据。

