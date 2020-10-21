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
        u2 minor_version; // Class文件的次版本号
        u2 major_version; // Class文件的主版本号
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

字面量比较接近 Java 语言层面的常量概念，如文本字符串、声明为 final 的常量值等

##### 2. 符号引用（Symbolic References）

而 符合引用 则属于 **编译原理** 方面的概念，包括了 **三类常量**，如下所示：

* 1）、类和接口的 全限定名（Fully Qualified Name）
* 2）、字段的名称和描述符（Descriptor）
* 3）、方法的名称和描述符

此外，**在虚拟机加载 Class 文件的时候会进行动态链接，因此其字段、方法的符合引用不经过运行期转换的话就无法得到真正的内存入口地址，也就是无法直接被虚拟机使用。当虚拟机运行时，需要从常量池获取对应的符号引用，再在类创建或运行时进行解析，并翻译到具体的内存地址只中。**

常量池中每一项都是一个表，**constant_pool** 中存储了一个一个的 cp_info 信息，并且每个 cp_info 的第一个字节（即一个 u1 类型的标志位）标识了当前**常量池中 常量项的 类型**，其后才是具体的常量项内容。

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
        u1 bytes[length]; // 字节数组
  }
```

其元素含义如下所示：

1）、`tag`：值为 1，表示是 CONSTANT_Utf8_info 类型表。

2）、`length`: length 表示 bytes 的长度，比如 length = 10，则表示接下来的数据是 10 个连续的 u1 类型的数据。

3）、`bytes`: u1 类型的数组，保存真正的常量数据。

```c++
CONSTANT_MethodType_info {
	u1 tag; // 10
	u2 class_index; // 指向声明方法的 类描述符的索引项 
	u2 name_and_type_index; // 指向名称及类型描述符的 索引项
}
```



字段类型 常量项类型

```c
CONSTANT_Filed_info{
	u1 tag; // 9
    u2 class_index; // 指向声明字段的类或者接口描述符 CONSTANT_Class_info 的索引项
    u2 name_and_type_index; // 指向字段描述符 CONSTANT_NameAndType 的索引项
}
```

类 常量项类型（描述类或接口信息）

```c
CONSTANT_Class_info{
	u1 tag; // 7
    u2 name_index; // 指向全限定名常量项索引
}
```

#### 常量项 Integer、Long、Float、Double

常量项 Integer、Long、Float、Double 对应的数据结构如下所示：

```c
CONSTANT_Integer_info{
	u1 tag;// 3
	u4 bytes; // 按高位在前的 int 值
}
```

```c
CONSTANT_Long_info{
	u1 tag;// 5;
    u8 bytes; //  按高位在前的 long 值
}
```

```c
CONSTANT_Float_info{
	u1 tag; // 4
    u4 bytes; // 按高位在前存储的 float 值
}
```

```c
CONSTANT_Double_info{
	u1 tag; // 6
    u8 bytes;// 按高位在前存储的 double 值
}
```

可以看到 **在每一个非基本类型的常量项之中，除了其 tag 之外，最终包含的内容都是字符串。正是因为这种互相引用的模式，才能有效的节省 Class 文件的空间。**

### 3 信息描述规则

对于 JVM 来说，其 **采用了字符串的形式来描述数据类型、成员变量及成员函数 这三类（描述符）** 因此，在讨论接下来各个 Class 表项之前，我们需要了解下 JVM 中的信息描述规则。下面我们来对此进行探讨。

# 1、数据类型

根据数据类型通常包含有 **原始数据类型、引用类型（数组）**，它们的描述规则分别如下所示：

1)、原始数据类型

Java 类型的 `byte 、char、short、int、long、float、double、boolean` => `B、C、S、I、J、F、D、Z`。

2）、引用类型数据

ClassName => `L + 全路径类名（其中的 "." 替换为 "/" ,最后加分号）` ,例如 String => `Ljava/lang/String；`

3）、数组（引用类型）

不同类型的数组 => "[该类型对应的描述名" ，例如 `int 数组 => [I` , `String 数组 => [Ljava/lang/String;` ,`二维数组 int 数组 => [[I`。

# 2、成员变量

在 JVM 规范之中，成员变量即 Field Descriptor 的描述规则如下所示：

# 3、成员函数描述规则

在 JVM 规范之中，成员函数即 Method Descriptor 的描述规则如下所示：

在注释 1 处， **MethodDescripor 由两个部分组成，括号内是参数的数据类型描述符，表示 0 个或多个 ParameterDescriptor，最后是返回值类型数据描述。** **void 的描述符规则为 V**。例如 `void hello(String str)` 的函数 => `(Ljava/lang/String;)V` 

可以看到，filed_info 与 method_info 都包含有 **访问标志、名字引用、描述信息、属性数量与存储属性** 的数据结构。对于 method_info 所描述的成员函数来说，它的内容经过编译之后得到 Java 字节码会保存在属性之中。

注意 ：**类构造器为 \<clinit> 方法、 而实例构造器为 \<init>  方法**



# 属性

只要不与已有属性名重复，任何人 实现的编译器都可以向属性表中写入自己定义的属性信息，Java 虚拟机运行时会忽略掉它所不认识的属性

**attribute_info** 的数据结构伪代码如下所示：

```jvm 代码
attribute_info{
	u2 attribute_name_index;
	u4 attribute_length;
	u1 info[attribure_lenght];
}
```

attribute_info 中的各个元素的含义如下所示：

* attribute_name_index ： 为 CONSTANT_Utf8 类型的常量项索引，表示属性名
* `attribute_lenght:` 属性的长度
* `info:` 属性具体的内容

## 1、attribute_name_index 

attribute_name_index 所指向的 Utf-8 字符串即为属性名称，而 **属性的名称是被用来区分属性的。** 所有的属性名称如下所示：

1）、`Code`  仅出现在 **method_info** 中，描述函数内容，即该函数内容编译后得到的虚拟机指令，try/catch 语句对应的异常处理表等待。

2）、`Exceptions:` 当函数抛出异常或错误时， method_info 会保存此属性。

3）、`Signature:` **JDK 1.5 中新增的属性，用于支持泛型情况下方法的签名，由于 Java 的泛型采用擦除来实现，为了避免类型信息上被擦除后导致签名混乱，需要这个属性来记录泛型中的相关信息。** 

4）、`SourceFile:` 包含一个指向 Utf-8 常量项的索引，即 Class 对应的源码文件名。

5）、`StackMapTable:` 在 JDK 1.6 发布后增加到 Class 文件规范中，这个是一个复杂的变长属性。

6）、`LineNumberTable:` Java 源码的行号与字节码指令的对应关系。

7）、`LocalVariableTable:` 方法的局部变量描述。

8）、`LocalVriableTypeTable`: JDK 1.5 中新增的属性，它使用特征签名代替描述符，是为了引入泛型语法之后能描述泛型参数化类型而添加的。

上述表格中，我们可以发现，不同类型的属性可能会出现在 ClassFile 中不同的成员变量，**当 JVM 在解析 Class 文件时会效验 Class 成员应该禁止携带有哪些类型的属性。** 此外 **属性也可以包含子属性，例如 Code 属性中包含有 LocalVariableTable**。

# 2、Code_attribute

首先，要注意 **并非所有的方法都必须存在这个属性，例如接口或抽象类中的方法就不存在 Code 属性。**

Code_attribute 的数据结构伪代码如下所示：

```
Code_attribute{
	u2 attribute_name_index;
	u2 attribute_lenght;
	u2 max_stack;
	u2 max_locals;
	u4 code_length;
	u1 code[code_length];
	u2 exception_table_length;
	
	{
		u2 start_pc;
		u2 end_pc;
		u2 handler_pc;
		u2 catch_type;
	}exception_table[exception_table_length];
	
	u2 attributes_count;
	attribute_info attributes[attributes_count];
}
```

JVM 在执行方法时，每个方法在执行的同时都会创建一个栈帧（Stack Frame）用于存储局部变量表、操作数栈、动态链接、方法出口等信息。每个方法从调用直至执行完成的过程，就对应着一个栈帧在虚拟机栈中入栈到出栈的过程。

1. `max_stack:` 代表了 **操作数栈（Operand Stacks）** 深度的最大值。在方法执行的任意时刻，操作数栈都不会超过这个深度。虚拟机在运行时需要根据这个值来分配栈帧（Stack Frame）中操作栈深度。
2. `max_locals:` 代表局部变量表所需的存储空间。max_locals 的单位是 Slot（内存大小单位），Slot 是虚拟机为局部变量分配内存所使用的最小单位。对于 byte、char、short、int、float、boolean 和 returnAddress 等长度不超过 32 位的数据类型，每个局部变量占用 1 个 Slot，而 double 和 long 这两种 64 位的数据类型则需要 2 个 Slot 来存放。方法参数（**包含实例方法中隐藏的 this**）、显示异常处理器的参数（Exception Handler Parameter，就是 try-catch 语句中的 catch 块所定义的异常）、方法体中定义的局部变量都需要局部变量表来存放。
3. `code_length:` 方法编译后的字节码长度。
4. `code:` 用于存储字节码指令的一系列字节流。既然叫字节码，那么每个指令就是 u1 类型的单字节。一个 u1 数据类型的取值范围为 0x00 - 0xFF，对应 0 - 255 ,也就是一共可以表达 256 条指令。
5. `exception_table_length:` 表示 exception_table 的长度。
6. `exception_table:` 每个成员为一个 ExceptionHandler ,并且一个函数可以包含多个 try-catch 语句，一个 try-catch 语句对应 exception_table 数组中的一项。

7. `start_pc, end_pc：` 如果当字节码在 [`start_pc`,`end_pc`) 之间出现类型为 `catch_type` 或其子类的异常（`catch_type` 为指向一个 CONSTANT_Class_info 型常量索引），则转到 第 handler_pc 行处理。
8. `handler_pc:` 表示 ExceptionHandler 的起点，为 code[] 的索引值。
9. `catch_type:` 为 CONSTANT_Class 类型常量项的索引，表示处理的异常类型。当 `catch_type` 值为 0 时，代表任意异常情况都需要转向到 `handler_pc` 处进程处理。此外，编译器使用异常表而不是简单的跳转命令来实现 Java 异常及 finally 处理机制。



### JVM 指令码

我们了解 **常量池、属性、field_info、method_info** 等等一系列的源码文件组成结构中，**它们仅仅是一种静态的内容，这些信息并不能驱使 JVM 执行我们在源码中编写的函数。**

从前面可知，Code_attribute 中的 code 数组存储了一个函数源码经过编译后得到的 JVM 字节码，其中仅包含如下 **两种** 类型的信息：

* 1）、	JVM 指令码： **用于指示 JVM 执行的动作，例如加操作、减操作、new 对象。其长度为 1 个字节，所以 JVM 指令码的个数不会超过 255 个（0xFF）。**
* 2）、JVM 指令码后的零至多个操作数： **操作数可以存储在 code 数组中，也可以存储在操作数栈（Operand Stack）中。**

**一个 Code 数组里指令和参数的组织格式** 如下所示：

`1 字节指令码  0或多个参数（N字节 N>=0）`

可以看到，Java 虚拟机的指令由一个字节长度的，代表着某种特定操作含义的数字（称为操作码，Opcode）以及跟随其后的零至多个代表此操作所需参数（称为操作数，Operands）而构成。此外大多数指令都不包含操作数，只有一个操作码。

如果不考虑异常处理的话，那么 Java 虚拟机的解释器可以使用下面这个伪代码当做 **最基础的执行模型** 来理解，如下所示：

```java
do{
    自动计算 PC 寄存器的值加 1；
    根据PC寄存器的指示位置，从字节码流中取出操作码；
    if(字节码存在操作数)从字节码流中取出操作数；
    执行操作码所定义的操作；
}while(字节码流长度 > 0)
```

由于 Java 虚拟机的操作码长度只有一个字节，所以 Java 虚拟机的指令集 **对于特定的操作只提供有限的类型相关指令去支持它。** 例如 **在 JVM 中，大部分的指令都没有支持整数类型 byte、char 和 short，甚至没有任何指令支持 boolean 类型。因此，我们在处理 boolean 、byte、short 和 char 类型的数组时，需要转换为与之对应的 int 类型的字节码指令来处理。**

众所周知，JVM 是基于栈而非寄存器的计算模型，并且，基于栈的实现能够带来很好的跨平台特性，因为寄存器指令往往和硬件挂钩。但是，**由于栈是一个 FILO 的结构，因此，对于同样的操作，基于栈的实现需要更多指令才能完成。此外，由于 JVM 需要实现跨平台特性，因此栈是在内存实现的，而寄存器则位于 CPU 的高速缓存区，因此，基于栈的实现其实速度相比寄存器的实现要慢的很多。要深入了解 JVM 的指令集，我们就必须从 JVM 运行时的栈帧讲起。**

## 1、运行时的栈帧

**栈帧（Stack Frame）是用于支持虚拟机进行方法调用和方法执行的数据结构，它是虚拟机运行时数据区的虚拟机栈（Virtual Machine Stack）的栈元素。**

栈帧中存储了方法的  **局部变量表、操作数栈、动态链接和方法返回地址、栈数据区** 等信息。**每个方法从调用开始至执行完成的过程，都对应着一个栈帧在虚拟机栈里面从入栈到出栈的过程。**

一个线程中的方法调用链可能会很长，很多方法都同时处于执行状态。**对于 JVM 的执行引擎来说，在活动线程中，只有位于栈顶的栈帧才是有效的，称为当前栈帧（Current Stack Frame）,与这个栈帧相关联的方法称为 当前方法（Current Method）。执行引擎运行的所有字节码指令都值针对当前栈帧进行操作，** 而 **栈帧的结构** 如下图：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/20201021182308.png" style="zoom: 80%;" />



Java 中一个方法调用时会产生一个栈帧（Stack Frame），而此方法便位于栈帧之内。而 Java 方法的栈帧 主要包含 3 个部分：

1. 局部变量表
2. 操作数栈
3. 栈帧数据区（常量池引用）

栈帧数据区，即常量池引用在前面我们已经深入了解过了，但是还有两个重要部分我们需要了解，一个是操作数栈，另一个是局部变量区。通常来说，**程序需要将局部变量表的元素加载到操作数栈中，计算完成之后，然后再存储会局部变量表。**

下面我们来看一操作数栈是怎么运转的。

## 2、操作数栈

操作数栈为了 **存放计算的操作数和返回结果。在执行每一条指令前，JVM 要求该指令的操作数已经被压入到操作数栈中，并且，在执行指令时，JVM 会将所需要的操作数弹出，并将计算结果压入操作数栈中。**

对于操作数栈相关的指令有如下 **三类：**

**1）、直接作用于操作数栈的指令：**

* `dup:` 复制栈顶元素，常用于复制 new 指令所生成的未初始化的引用。
* `pop:` 舍弃栈顶元素，常用于舍弃调用指令的返回结果。