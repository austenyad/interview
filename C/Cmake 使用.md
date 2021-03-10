Cmake 使用

* `cmake_minimum_required(VERSION 3.4.1)` 指定 cmake 最低支持的版本
* `aux_source_directory(. DIR_SRCS)` 查找当前目录下的所有源文件 并且源文件名称列表保存到 `DIR_SRCS` 变量，但是不能查找子目录。
* `add_library` 

1. 添加一个库

   ** 添加一个库文件，名为 \<name> 。

   ** 指定 STATIC，SHARED，MODULE 参数来指定库的类型。STATIC ： 静态库；SHARED：动态库

   MODULE：在使用 dyld 的系统有效，若不使用 dyld ，等同于 SHARED。

   ** EXCLUED_FROM_ALL ：表示该库不会被默认构建。

   ** source1 source2 ..... sourceN：用来执行库的源文件。

   ```cmake
   add_library(<name> [STATIC | SHARED | MODULE] [EXCLUED_FROM_ALL] source1 source2 ... sourceN)
   ```

2.  导入预编译库

   ** 导入一个已经存在的预编译库，名为 \<name>。

   ** 一般配合 set_target_properties 使用

```
add_library(<name> <SHARED|STATIC|MOUDLE|UNKNOWN> IMPORTED)

#比如
add_library(test SHARED IMPORTED)
#告诉库的路径在哪里
set_tartget_properties(test #指明目标库名
					PROPERTIES IMPORTED_LOCALION # 指明要设置的参数
					库路径/${ANDROID_ABI}/libtest.so # 导入库的路径
)
```

3. 常用 set 命令

```
#设置可执行文件的输出路径（EXCUTABLE_OUTPUT_PATH 是全局变量）
set(EXECUTABLE_OUTPUT_PATH [output_path])

#设置库文件的输出路径（LIBRARY_OUTPUT_PATH 是全局变量）
set(LIBRARY_OUTPUT_PATH [output_path])
#设置 C++ 编译参数（CMAKE_CXX_FLAGS 是全局变量）
set(CMAKE_CXX_FLAGS "-Wall std=c++11")
# 设置源文件集合（SOURCE_FILES 是本地变量及自定义变量）
set(SOURCE_FILES main.cpp test.cpp ...)

```

3 include_dircetories 

```
#可以用相对路径或绝对路径，也可以用自定义的变量值
inlucde_directories(./include ${MY_INCLUDE})
```

上面命令：

* 设置头文件目录
* 相当于 g++ 选项中的 -l 参数。

4. add_executable

```cmake
add_executable(<name> ${SRC_LIST})
```

* 添加可执行文件

5. target_link_libraries

```
target_link_libraries(<name> lib1 lib2 lib3)

#如果出现互相依赖的静态库，CMAKE 会允许依赖图中包含循环依赖，如：
add_library(A STATIC a.c)
add_library(B STATIC b.c)
target_link_libraries(A B)
target_link_libraries(B A)
add_executable(main main.c)
target_link_libraries(main A)

```

* 将若干个库链接到目标文件
* 链接的顺序应当符合 gcc 链接顺序规则，被链接的库存放在依赖它的库的后面，即如果上面的命令中，lib1 依赖 lib2 ，lib2 又依赖于 lib3 ，则在上面命令中必须严格按照 lib1 lib2 lib3 的顺序排列，否则会报错。