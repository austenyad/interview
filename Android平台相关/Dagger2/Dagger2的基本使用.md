## Dagger2的基本使用

这一篇就开始了解 Dagger2 的使用了，做好准备。

现在我需要产品，我是**消费者**，不能直接去工厂里面拿产品需要一个**中间人**即商家去和**厂家**协商，我才能最后拿到我想要的产品。

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-152959.png)

在 Dagger2 中，也有上面类似的模型，它是通过注解来表明上面的供应关系的。

1. 对于消费者来说，我要什么产品，要告诉商家。在类中表现为：类现在需要一个依赖对象，类要把这个消息告诉 Dagger。我们只需要在所需要的对象上面用 **@Inject** 注解这个对象即可。

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-153018.png)

图：表明我们的 MainActivity 类中需要依赖 Product 对象。

2. 消费者需要什么产品，消费者自己已经表明，但是目前商家还不知道，并且在我们的类中连商家这个角色也没有。那么在 Dagger2 中，充当中间商家的是用 **@Component** 注解的一个接口。

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-153028.png)

声明完商家还不行，商家必须知道是那个消费者，商家要为那个消费者服务。

![](/Users/austen/Desktop/interview/Android平台相关/Dagger2/Screenshot/6.png)

这样商家就会知道是谁需要产品，并且为他服务。商家就会去消费者 MainActivity 里面去找被 **@Inject** 注解的类。这里给商家声明谁是消费者，是固定写法：`void inject(消费者)；` 先不用知道为什么这样写。

3. 目前想想是不是就剩工厂角色还没有，没有工厂角色为商家提供产品。你现在可以试着 Make Project 一下项目；

![](/Users/austen/Desktop/interview/Android平台相关/Dagger2/Screenshot/7.png)

报错了，错误信息描述：在没有用 **@Inject** 注解其构造方法 或者 没有一个 **@Provides** 注解的方法的情况下，Product 对象不能被提供。

其实上面已经告诉我们怎样去提供 Product 对象，有两种方式。我们分别都说一下。

###### 3.1 声明工厂角色：

工厂角色在 Dagger2 中也是一个类，只不过是用 **@Module** 注解的类。然后像上面错误信息所提示的那样声明一个被 **@Provides** 注解的方法，并提供对象，因为工厂是正真生产对象的。

![](/Users/austen/Desktop/interview/Android平台相关/Dagger2/Screenshot/8.png)

还有要告诉商家，那个工厂提供对象。要在 Businessman **Component** 中声明，并且编译项目：

![](/Users/austen/Desktop/interview/Android平台相关/Dagger2/Screenshot/9.png)

这时 FactoryModule 提供的对象已经通过 Businessman 交给 MainActivity。我们就可以在 MainActivity 中使用 Product 对象了。

![](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-153056.gif)

在 Make Project 的时候你会神奇的发现，IDE 中 product 这个变量从没有被引用变为引用了。这时就表明你的注入代码没问题。

###### 3.2 声明构造

从上面第一次编译的错写信息来看，还有一种方式来完成。用 **@Inject** 注解 Proudect 类的构造方法。首先将 **Businessman** 中 `Component` 与 `Module` 的关系断开（断开的目的就是看看只用给 Product 声明构造的方式是否注入成功）。

![image-20200512072359652](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-232402.png)

我们将这个商家与厂家的关系断开。然后给 `Product` 类的构造函数上添加 `@Inject` 注解。

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-233251.png" alt="image-20200512073143253" style="zoom:50%;" />、

然后在 `Make Project` 一下。声明构造方法也可以。

#### 注入到类中的依赖对象使用

到这里还没玩，还需要显示的调用注入的方法。为什么还要显示的调用 `void inject()` 方法呢，这个问题我想在说一下：首先 Dagger2 是通过编译器生成代码来进行依赖注入的，它并不是在运行时初始化对象的，所有的工作都是在编译期完成。

> <img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-234459.png" alt="image-20200512074434911" style="zoom:30%;" /> 

现在你可以点击 `MainActivity` 类中 `Produce` 成员变量，IDE 提示 ` instance.product = product` 这个变量被外部类的一个 `instance` 对象引用了，然后被赋值 `product` 。点进去看一下，你会发现有个方法：

> <img src="/Users/austen/Library/Application Support/typora-user-images/image-20200512074944588.png" alt="image-20200512074944588" style="zoom:33%;" /> 

先不用管 Dagger2 是怎么在编译期生成这样的一个方法。首先它是一个方法，这个方法被调用后 `Mactivity ` 中 `product` 这个对象才会正真的被赋值，那么这个方法是谁调用的呢？

答：没有人调用！！！ 所以需要去主动调用这个方法，`product` 成员变量才会被赋值。你只要理解 Dagger2 的工作范围是在编译期，它只是在编译器，根据我们定义的 `@Inject` 、`@Component` 、`@Module` 相关代码生成依赖注入的代码，正真的初始化对象肯定是在运行期，运行期不属于 Dagger2 的工作范围，那么就需要我们来介入。

> ![image-20200512080526221](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-12-000529.png)

很明显 `Product` 对象已经被注入到 `MainActivity` 当前。

```java
DaggerBusinessman.builder()
                .factoryModule(new FactoryModule())
                .build()
                .inject(this);
```

上面代码就是主动调用注入的方法，这里会最终调用到刚才最终赋值 `instance.product = product` 的这个语句，`product` 对象才被初始化完成。