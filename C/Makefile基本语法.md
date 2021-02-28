# MakeFile 语法基础

1. 什么是 makefile

* 在一个工程中它的源文件很多，其按类型、功能、模块分别被放在若干个目录中，makefile 就是定义了一系列规则来指定，那些文件需要先编译好，那些文件需要重新编译，以及如何进行链接等操作。

* makefile 就是 “自动化编译” 脚本，告诉 make 命令如何编译和链接。默认情况下，make 命令会在当前目录下寻找文件。

2. makefile 文件中包含有那些内容？

* 显示规则：说明了如何生成一个或多个目标文件。
* 隐晦规则：由于 make 有自动推倒的功能，所以隐晦的规则让我们可以简略的书写 makefile。
* 变量定义：在 makefile 中可以定义一系列的变量，这些变量都是字符串，很像 C 中的宏定义，当执行的时候会被替换。
* 文本指示：它包括三个部分：1）在一个 makefile 引用另一个 makefile。2）根据某些情况指定 makefile 中的有效部分，就像 C 中的预编译。3）定义一个多行的命令。
* 注释：只有行注释 使用 “#” ，如果在 makefile 中要使用到 “#” 可以使用 “' \\#”

3. makefile 中的规则

* target ：目标文件。可以是 ObjectFile 也可以是 执行文件，还一个是标签（Label）。一般来说，我们的目标文件是一个，但也有可能是多个文件，如果是过个文件的话可以用空格隔开。另外 target可以使用通配符。
* prerequisites：就是生成目标文件所依赖的文件，或者依赖的目标。
* command ：就是 make 需要执行的命令，如果命令和 target 和 prerequisites 在同一行的话，可以使用 ";" 作为分割。如果不在同一行，必须要以 Tab 键开头。另外如果命令太长，我们可以使用 “\\” 作为换行符。

makefile 规则其实就是告诉 make 两件事情：1) 文件的依赖关系 2）如果生成目标文件。也就是说：target 这一个或者多个目标文件，依赖 prerequisites 指定的文件，它的生成规则定义在 command 中，说白了就是 prerequisites 中如果有一个以上的文件比 target 要新的话，target 就会被认为是过时的，需要重新生成，那么 command 命令就会被执行，从而生成新的 target 。

![image-20210227182102728](/home/austenyang/.config/Typora/typora-user-images/image-20210227182102728.png)

makefile 中变量是使用

如果我们的工程需要加入一个新的 .o 文件，那么我们需要修改三个地方，上面示例当中的 makefile 并不复杂，但是 makefile 变的复杂，那么我们就可能忘掉需要加入的地方，导致编译失败，为了 makefile  便于维护，在 makefile 中我们可以使用变量，makefile 的变量也就是一个字符串，可以理解为 C 中的宏定义。

![image-20210227183902091](/home/austenyang/.config/Typora/typora-user-images/image-20210227183902091.png)

* makefile 中引入其他的 makefile，在我们的实际项目中，可能会把源文件按类型分成很多个模块，每个模块有个单独的 makefile 文件，那么我们编译的时候就需要引入其他模块的 makefile ，在 makefile 中可以使用 include 关键字来引入其他的 makefile ， include 语法格式：include <<fileName>> 

![image-20210227184253849](/home/austenyang/.config/Typora/typora-user-images/image-20210227184253849.png)

#### 环境变量 MAKEFILES

如果当前环境中定义了环境变量 MAKEFILES，那么 make 会把这个变量中的值做一个类似于 include 的动作。这个变量中的值是其他的 Makefile ，用空格分隔。只是，它和 include 不同的是，从这个环境变量引入的 Makefile 的 “目标” 不会起作用，如果环境变量总定义的文件错误，make 也不会理会。但是建议不要使用这个环境变量，因为只要这个编译一被定义，那么当你使用 make 时，所有的 Makefile 都会受到影响。也许有时候 Makefile 出现了奇怪的事，那么可以查看当前环境变量中有没有定义这个变量。

#### Makefile 中的预定义变量

![image-20210227185148261](/home/austenyang/.config/Typora/typora-user-images/image-20210227185148261.png)

#### Makefile 中的自动变量

![image-20210227185332680](/home/austenyang/.config/Typora/typora-user-images/image-20210227185332680.png)

#### Makefile 中的自定义函数

分为 不带参数 和 带参数

![image-20210227185553850](/home/austenyang/.config/Typora/typora-user-images/image-20210227185553850.png)

#### make 的工作流程

GUN 的 make 工作时的执行步骤如下：

1. 读入所有的 Makefile。
2. 读入被 include 的其他 Makefile。
3. 初始化文件中的变量。
4. 推倒隐晦规则，并分析所有规则。
5. 为所有的目标文件创建依赖关系链。
6. 根据依赖关系，决定那些目标要重新生成。
7. 执行生成命令。

1~5 第一个阶段

6~7 第二个阶段

在第一个阶段中如果定义的变量被使用了，那么 make 会将变量展开在使用的位置，但是 make 并不会完全的马上展开，如果变量出现在依赖关系的规则中，那么只有当这条依赖被决定要使用的时候，变量才会被展开。

