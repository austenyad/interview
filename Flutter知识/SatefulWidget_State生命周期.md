#### State 生命周期

State 的生命周期，指的是在用户参与的情况下，其关联的 Widget 所经历的，从创建到显示再到更新最后到停止，直至销毁等各个过程阶段。

#### 创建

State 初始化时会一次执行： 构造方法 -> initState -> didChangeDependencies -> build，随后完成页面渲染。

构造方法是 State 生命周期的起点，Flutter 会通过调用 StatefulWidget.createState() 来创建一个 State。

initState，会在 State 对象被插入视图树的时候调用。这个函数在 State 的生命周期中只会被调用一次，所以我们可以在这里做一些初始化工作，比如状态变量默认值。

didChangeDependencies 则用来专门处理 State 对象依赖关系变化，会在 initState() 调用结束后，被 Flutter 调用。

build ，作用是构建视图。经过以上步骤，FrameWork 认为 State 已经准备好了，于是调用 build，作用是构建视图。

#### 更新

Widget 的状态更新，主要由 3 个方法触发： setState、didChangeDependencies 与 didUpdateWidget。

setState：当状态数据发生变化时，我们总是通过调用这个方法告诉 Flutter：我们的数据变了，请使用更新后的数据重建UI。

didChangeDependenceis：State 对象的依赖关系发生变化后，Flutter 会调用这个方法，随后触发组件构建。哪些情况下 State 对象的依赖关系发生变化呢？典型的场景是，系统语言 Locale 或 应用的主题改变时，系统会通知 State 执行。

didUpdateWidget：当 Widget 的配置发生变化时，比如，父 Widget 触发重建（即父 Widget 的状态发生变化时），热重载时，系统调用这个函数。

一旦这三个方法被调用，Flutter 随后就会销毁老 Widget，并调用 build 重建 Widget。

#### 销毁

组件销毁相对比较简单。比如组件被移除，或是页面销毁时候，系统会调用 deactivate 和 dispose 这两个方法，来移除或销毁组件。



具体调用机制：

​	当组件的可见状态发生变化时，deactivate 函数会被调用，这时 State 会被暂时从视图树中移除。

​	值得注意的是，页面切换时，由于 State 对象在视图树中的位置发生了变化，需要先暂时移除后再重写添加，重新触发组件构建，因此这个函数被调用。

​	当 State 被永远地从视图树中移除时，Flutter 会调用 dispose 函数。而一旦到这个阶段，组件就要被销毁了，所以我们可以在这里的资源释放、移除监听、清理环境，等等。



