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
> 对比一看，IDE 出现上面的标记，就说明 `Business` 接口是有实现类的。至于到底生成了什么，我们今天先不关心，后面一点一点都剖析到。

文章开头说，Dagger2 的工作范围是编译期，它只是生成了依赖注入相关的代码，而要想正真的对象注入到类中，还需要我们手动调用。具体来看一下：

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

我们在 `Mactivity` 中调用了 `DaggerBusinessman` 实例的 `inject` 方法，对于当前 Dagger2 版本生成的代码可以用 3 种方式调用。我们调用其中最简单的一种，然后调用依赖类的方法。

<img src="/Users/austen/Library/Application Support/typora-user-images/image-20200514080530210.png" alt="image-20200514080530210" style="zoom:50%;" />

依赖类的方法被调用了，显然依赖对象已经被真正的实例化了。

> 上面的流程就是 Dagger2 实例化对象的流程，可能刚接触有点不熟悉，慢慢来，我在一开始也是很懵逼的。

#### 

##### 更简单的方式

上面的例子有个简化的版本，因为我们注入到 `Mactivity` 的 `Cola` 比较简单（它只有一个无参的构造函数），那么在 `Component` 中不在声明 `Module` ,只是简单的在 `Cola` 的无参构造函数上标记 `@Inject` 注解就可以了。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-16-234501.png" alt="image-20200517074454761" style="zoom:50%;" />

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-16-234624.png" alt="image-20200517074619444" style="zoom:50%;" />

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-16-234708.png" alt="image-20200517074659958" style="zoom:50%;" />

同样也可以注入到 `Mactivity` 中。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-16-235035.png" alt="image-20200517075027117" style="zoom:50%;" />

那么问题来了，在 `Component` 中声明 `Module`,同时又给 `Cola` 构造方法上声明 `@Inject` 注解，那么 Dagger 会使用哪一个来进行注入代码的生成呢？

答案是：第一种。

来看一下两种方式生成的代码：

第一种：只在 `Cola` 构造方式上注解 `@Inject` 

<img src="/Users/austen/Library/Application Support/typora-user-images/image-20200517080008933.png" alt="image-20200517080008933" style="zoom:50%;" />

第二种：即在 `Component` 中使用 `Moudle` 也在 `Cola` 的构造方式声明 `@Inject`

<img src="/Users/austen/Library/Application Support/typora-user-images/image-20200517080143693.png" alt="image-20200517080143693" style="zoom:50%;" />

> 不用管具体代码的注入过程，显然在 `Component` 中使用 `Module` 并在 `Cola` 构造方法声明 `@Inject` 都会使用从 `Module` 中取对象的方式来完成注入。

说明：在取对象的过程中，`Module` 的优先级是不 `Inject`  高的。

关于 Dagger 里面最基本的概念讲完了，不知道你理解一些没有，但在 Dagger 生成代码，完成注入的整个过程中，你可以把过程中出现的几个角色想想成人，慢慢去理解它。

我：需要对象

`Compoent`: 中间者或者桥梁，要得到想要的对象得经过它

`Module`: 生成对象的地方

当声明完 `Compoent`后，比如

```java
@Component
public interface Businessman {
    void inject(MainActivity activity);
}

```

`Component` 就会去 `Mactivity` 中去查找 `Mactivity` 中被 `@Inject` 标记的成员变量。

```java

@Component(modules = FactoryModule.class)
public interface Businessman {
    void inject(MainActivity activity);
}
```

当 `Component` 发现，`MainActivity` 中被注解的 `Cola` 它就会去 `Module` 中去查找有没有提供 `Cola` 的方法

```java
//FactoryModule.java
@Provides
public Cola provideProduct() {
    return new Cola();
}
```

那么整个过程就链接起来了。这也表明你要给 `Mactivity` 注入 `Cola` 对象，现在通过 Dagger 是没问题的。

当 `Module` 中没有提供 `Cola` 的方法，那么 `Component` 就会去找 `Cola` 的构造方法，但可能，`Cola` 的构造方法很多，它只会找被 `@Inject` 注解的构造方法。如果没有那么要想注入 `Cola` 对象肯定是有问题的。

总的来说在使用 Dagger 的过程中你要能将你所需要的对象，整个通过上面的流程链接起来。