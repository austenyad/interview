### 什么是 Cmake

* 在 AS 2.2 级以上，构建原生库的默认工具是 Camek。
* Cmake 是一个狂平台的构建工具，可以用简单的语句来描述 **所有平台** 的安装（编译过程）。能够输出各种各样的 makefile 或者 project 文件。Cmake 并不直接构建最终的软件，而是产生其他工具的脚本（如 makefile），然后在依据这个工具构建应用。
* Cmake 是比 make 更加高级的编译配置工具，它可以根据不同平台、不同编译器，生成相应的 maekfile 或 vcproj 项目，从而达到跨平台的目的。Android Studio 利用 Cmake 生成的是 ninja。ninjia 是一个小型的关注速度的构建工具系统。我们不需要关心 ninja 的脚本，知道怎么配置 Cmake 就可以了。
* Cmake 其实是一个跨平台的支持各种不同脚本的一个工具。

## Cmake 源文件

* Cmake 的源码文件可以包含命令、注释、空行和换行。
* 以 Cmake 编写的源文件以 CmakeLists.txt 命名或以 .cmake 为扩展名。
* 可以通过 add_subdirectory() 命令把子目录的 Cmake 源文件添加进来。
* Cmake 源文件中所有有效的语句都是命令，可以是内置命令或自定义的函数/宏命令。

#### Cmake 注释

* Cmake 单行注释 ：#注释内容
* 多行注释： #[[多行注释]]

#### cmake 变量

* Cmake 中所有变量都是 String 类型。`set()` 和 `unset()` 命令来声明和移除。

声明变量：`set(变量名 变量值)` 

花括号引用变量：${var}

`set(var 123)`

引用变量 message 命令来打印 

`message("var = ${var}")`

#### cmke 中的列表（list）

* 列表也是字符串，可以把列表看做一个特殊的变量，这个变量可以有多个值。
* 语法格式：`set(列表名 值1 值2 值3 ... 值N)`  或者 `set(列表名 "值1;值2;值3;.. 值N")`
* 列表的引用：`${列表名}`

```cmake
set(list 1 2 3 4 5 6 7)
set(list2 "11;22;33;44;55;66;77;88")
message(list = ${list})
message(list2 = ${list2})
```

#### Cmake 流程控制-操作符

| 类型 | 名称                                                         |
| ---- | ------------------------------------------------------------ |
| 一元 | EXIST,COMMAND,DEFINED                                        |
| 二元 | EQUAL,LESS,LESS_EQUAL,GREATER,GREATER_EQUAL,STREQUAL,STRLESS,STRLESS_EQUAL,STRGREATER,STRGREATER_EQUAL,VERSION_EQUAL,VERIOSN_LESS,VERSION_LESS_EQUAL,VERSION_GREATER,VERSION_GREATER_EQUAL,MATCHE |
| 逻辑 | NOT,AND,OR                                                   |

**注意：这些操作符都是大小写敏感的。**

* 带括号的表达式 > 一元操作符 > 二元操作符 > 逻辑操作符

#### Cmake 流程控制-布尔常量值

| 类型  | 值                                                           |
| ----- | ------------------------------------------------------------ |
| true  | 1,ON,YES,TRUE,Y,非0的值                                      |
| false | 0,OFF,NO,FALSE,N,IGNORE,NOTFOUND，空字符串，以 -NOTFOUND 结尾的字符串 |

#### Cmake 流程控制-条件命令

* 语法格式

```cmake
if(表达式)
	COMMAND(ARGS ...)
elseif(表达式)
	COMMAND(ARGS ...)
else(表达式)
	COMMAND(ARGS ...)
endif(表达式)
```

* elseif 和 else 部分是可选的，也一个有多个 elseif 部分，缩进和空格对语句解析没有影响。

#### Cmake 流程控制-循环命令

* 语法格式

```
while(表达式)
	COMMAND(ARGS ...)
endwhile(表达式)
```

* brreak() 命令可以跳出整个循环，continue() 可以跳出当前循环。

```cmake
set(a "")
while(NOT a STREQUAL "xxx")
    set(a ${a}x)
    message(a = ${a})
endwhile()
```

#### Cmake 流程控制-循环遍历

* 语法格式 一

```
foreach(循环遍历 参数1 参数2 ... 参数N)
	COMMAND(ARGS ...)
endforeach(循环变量)
```

* 每次迭代设置循环变量为参数
* foreach 也支持 break() 和 continue() 命令跳出循环。

```cmake
foreach(item 1 2 3)
    message("item = ${item}")
endforeach(item) 
```

* 语法格式 二

```
foreach(循环变量 RANGE total)
	COMMAND(循环变量)
endforeach(循环变量)
```

* 循环范围从 0 到 total。

```cmake
foreach(item RANGE 7)
    message(item = ${item})
endforeach(item)
```

* 语法格式 三

```
foreach(循环变量 RANGE start end step)
	COMMAND(ARGS ...)
endforeach(循环变量)
```

* 循环范围从 start 到 end，循环递增量为 step。

```cmake
foreach(item RANGE 1 9 2)
    message(item = "${item}")
endforeach(item)
```

* 语法格式 四

* foreach 还支持对列表的循环

```
foreach(循环变量 IN LISTS 列表)
    COMMAND(ARGS ...)
endforeach(循环变量)
```

```cmake
set(list "1;2;3;4;5;6;7;8;98")
foreach(item IN LISTS list)
    message(item ${item})
endforeach(item)
```

#### Cmake 自定函数

* 自定义函数命令格式

```
function(<name> [arg1 [arg2 [arg3]]])
	COMMAND()
endfunction(<name>)
```

* 函数命令调用格式

`name(实参列表)`

```cmake
function(func a b c)
    message("a = ${a}")
    message("b = ${b}")
    message("c = ${c}")
    message("ARGC = ${ARGC}") # ARGC : Cmake 内置变量 表示传入参数个数
    message("ARGV = ${ARGV}") # ARGC : Cmake 内置变量 表示 所有参数列表
    message("ARGV0 = ${ARGV0}") # ARGV0 : Cmake 内置变量 获取参数列表中的值
    message("ARGV1 = ${ARGV1}") # ARGV1 : Cmake 内置变量 获取参数列表中的值
    message("ARGV2 = ${ARGV2}") # ARGV2 : Cmake 内置变量 获取参数列表中的值
    message("ARGV3 = ${ARGV3}") # ARGV3 : Cmake 内置变量 获取参数列表中的值 为空
endfunction(func)

func(1 2 3)
```

#### Cmake自定义宏命令

* 自定义宏命令格式

```cmake
macro(<name> [arg1 [arg2 [arg3 ...]]])
	COMMAND()
endmacro(<name>)
```

* 宏命令调用格式

name(实参列表)

```cmake
macro(ma x y)
    message("call macro ma")
    message("x = ${x}")
    message("y = ${y}")
endmacro(ma)

ma("hello" "world")
```

**注意： 函数有自己的作用域  **而 **宏命令和调用者的作用域是一样的**

#### Cmake 中变量的作用域

* 全局层：cache 变量，在整个项目范围可见，一般在 set 定义变量时，指定 CACHE 参数就能定义为 cache 变量。
* 目录层：在当前目录 CmakeLists.txt 中定义，以及在该文件包含的其他 camke 源文件中定义的变量。
* 函数层：在命令函数中定义的变量，属于函数作用域内的变量。

**函数层变量会覆盖目录层变量，目录层变量会覆盖全局层变量**