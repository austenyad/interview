## Dagger2的基本使用

这一篇就开始了解 Dagger2 的使用了，Dagger2 中会使用比较多的内部注解在编译时生成依赖注入的代码，它的**工作区间就是在程序的编译构建期**，要让编译期生成的依赖注入代码起作用，就需要程序在运行期调用，生成的依赖注入代码，这个调用是需要我们手动完成的。你先要知道关于 Dagger2 整体的工作原理，时刻去揣摩他内部的工作原理，这个我觉得很重要。

#### Dagger2 中最重要的三个注解

**@Inject**

**@Component**

**@Module**

下面我们通过一个示例模型来说明，好让你能好理解一些。

现在我需要产品，我是**消费者**，我要去买一瓶神仙快乐水，不能直接去工厂里面拿产品需要一个**中间人**即商家去和**厂家**协商，我才能最后拿到我想要的产品。上面三个注解，在下图中对于的关系是：

![image-20200513074544568](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-12-234546.png)

1.  `@Inject` 声明我需要什么，也就是我要依赖谁。（我要可乐，然后用可乐解渴）
2. `@Compoent` 中的间商家，它会去与厂家协商，并且它知道消费者需要什么。（需要可乐啊！！！）
3. `@module` 提供可乐的厂家。

很简单吧，下面我们具体看一下，怎么通过代码将对象注入到类中的。还是三个步骤：

1. 什么我需要什么：

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-12-235820.png" alt="image-20200513075818346" style="zoom:50%;" align="left"/>

2.  什么完我需要可乐，但是这时候还没有商家，需要商家告诉商家我要可乐。

> <img src="/Users/austen/Library/Application Support/typora-user-images/image-20200513080205531.png" alt="image-20200513080205531" style="zoom:50%;" align="left" />

解释：被 `@Compoent` 什么的商家**必须是接口**，在这个接口中什么了消费者谁（我），也声明了提供可乐的厂家是谁。***消费者：***`void inject(MainActivity activity)` 接口中这个方法就声明消费者是 `MainActivity` ,***厂家*：** `@Compoent(modules = FactoryModule.class)` 在这个 `@Component` 注解中 `moudles` 的参数 `FactoryModule` 就是生产可乐从厂家。（果然你看 `@Component` 是中间者，起来消费者和厂家联系的媒介）

3. 这时就是剩正真生产可乐的厂家，第二步中 `@Compoent(moudles = FactoryMoudle.class)` 注解参数中声明的就是这个类。

   > ![image-20200514070023788](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-230025.png)

用 `@Module` 注解表明生产对象的类，在这个类中所有提供对象的方法都要被 `@Provides` 注解。

上面就是关于给 `Mactivity` 通过 Dagger2 注入 `Cola` 对象的代码，现在需要编译一下项目，Dagger2 就会生成依赖注入的代码。

> 编译前
>
> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-231122.png" alt="image-20200514071050647" style="zoom:50%;" align="left" />
>
> 编译后
>
> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-231321.png" alt="image-20200514071315547" style="zoom:50%;" align="left" />
>
> 对比一看，IDE 出现上面的标记，就说明 `Business` 接口是有实现类的。多于到底生成了什么，我们今天先不关心，后面一点一点都剖析到。

文章开头说，Dagger2 的工作范围是编译期，它只是生成了依赖注入相关的代码，对于程序运行时注入的调用，还需要我们手动调用。具体来看一下：

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-232013.png" alt="image-20200514072001205" style="zoom:50%;" />

从图中看，要注入的对象 `cola` 被 `MainActivity_MembersInjector` 类中的一个语句引用，并且赋值。`instance.cola = cola` ,这个表达式的左边 `instance` 肯定是 `MainActivity` 的实例，右边一定是 Dagger2  生成的 `Cola` 类的实例，通过这个表达式，`MainActivity` 中的 `cola` 就被赋值了。我们点进入看一下：

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-232701.png" alt="image-20200514072653305" style="zoom:50%;" />

只看 `injectCola(MainActivity instance,Cola cola) ` 这个方法，很明显当这个方法被调用时，`MainActivity` 中的 `cola` 才会被正真的实例化完成。那么是谁调用这个方法呢？我们继续看。

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-233352.png" alt="image-20200514073343813" style="zoom:50%;" />

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-233450.png" alt="image-20200514073445099" style="zoom:50%;" />

（记住我们这里只看方法的调用关系，别陷入太深）这不是 `Businessman` 接口的实现类吗！！！看 Dagger2    在背后为了依赖注入生成的代码。我们在代码中所有被 `@Component` 注解的接口，生成的实现类的名字都是在原有的接口名之前加上 **Dagger**，例如：`DaggerBusinessman` 。

从图中看调用关系：

`DaggerBusinessman#inject(MainActivity activity)` ->

`DaggerBusinessman#injectMainActivity(MainActivity instance)`->

`MainActivity_MembersInjector#injectCola(MainActivity instance, Cola cola)`

那么当调用 `DaggerBusinessman#inject(MainActivity activity)`  这个方法时，`MainActivity_MembersInjector#injectCola(MainActivity instance, Cola cola)` 里面的 `instance.cola = coloa` 就会被调用赋值。

再仔细想一想，`DaggerBusinessman#inject(MainActivity activity)` 方法不就是

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-13-235213.png" alt="image-20200514075212369" style="zoom:50%;" />

`Businessman` 接口方法中 `void inject(MainActivity activity) ` 的抽象方法实现吗！！！

我们手动调用程序在运行时，实例化 `cola` 对象的代码就是，调用 `DaggerBussinessman#inject(MainActivity activity)` 方法。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-14-000104.png" alt="image-20200514080057203" style="zoom:50%;" />

我们在 `Mactivity` 中掉用了 `DaggerBusinessman` 实例的 `inject` 方法，对于当前版本生成的代码可以用 3 种方式调用。我们调用其中最简单的一种，然后调用依赖类的方法。

<img src="/Users/austen/Library/Application Support/typora-user-images/image-20200514080530210.png" alt="image-20200514080530210" style="zoom:50%;" />

依赖类的方法被调用了，显然依赖对象已经被真正的实例化了。

> 上面的流程就是 Dagger2 实例化对象的流程，可能刚接触有点不熟悉，慢慢来，我在一开始也是很懵逼的。

