## 什么是依赖注入，为什么要使用 Dagger2？

开篇：我想让你想一想依赖是什么？是不是在面向对象的世界里面，一个类使用一个类去完成某一项任务，是我们每一天都要使用的方式。尤其是在面向对象这种编程范式中，你要完成一项任务，类与类之间的协作肯定是避免不了的。依赖就是正在使用的类依靠另一个类的这种类与类之间的关系。

下面在讲解这种依赖关系时，当前对象即要使用被依赖类的类称为 Client 类，被依赖的类称 Dependency。你仔细思考，是不是每天都在面对这种关系，处理两个类之间的关系。而我们说的依赖注入 Dependency Injection (简称 DI)，它是一种具体的编程技巧。感觉很高大上的概念，但它其实很简单，我用一句化来说明：不通过 new() 的方式在类内部创建依赖对象，而是将依赖的对象在外部创建好后通过构造方法注入（*Constructor Injection*）、方法的参数注入（*argument Injection* ）。

下面还是通过一个例子来解释一下。我们在使用 MVP 模式的过程中，我们知道 P 层「Presenter」,现在我们的 P 层依赖 UserManger 对象

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-153217.png" style="zoom:50%;" />

现在我们只管 LoginModel 类和 UserManger 之间的依赖关系。

```java
//非注入方式
public class LoginPresenter{
  private UserManger userManger;
  
  public LoginPresenter(){
    this.userManger = new UserManger();
  }
  
  public void login(String moblie,String pwd){
    userManger.login(moblie,pwd);
   //....逻辑省略
  } 
}

public class UserManger{
  public void login(String moblie, String pwd) { 
    //.... 
  }  
}

// 使用LoginPresenter
LoginPresenter presenter = new LoginPresenter();


//依赖注入的方式
public class LoginPresenter{
  private UserManger userManger;
  //通过构造方法进行依赖注入
  public LoginPresenter(UserManger userManger){
    this.userManger = userManger;
  }
  
  public void login(String moblie,String pwd){
    userManger.login(moblie,pwd);
   //....逻辑省略
  } 
}

//使用LoginPresenter
UserManger userManager = new UserManger();
LoginPresenter presenter = new LoginPresenter(userManager);
```

你思考一下是不是就是简单的通过外部注入对象的方式，代码立即变得扩展性更强了。我们可以灵活的替换这个依赖类。这个代码还可以进一步优化，当业务复杂起来，你可以把 UserManger 定义成接口，就又成了所谓的面向接口编程了。

依赖注入这个概念听起来很高大尚，其实就是个三毛钱的东西，操作起来非常简单。但就像上面说的，一但对象可以从外部注入，我可以定义接口，不一定使用的就的这个特定的 UserManger 对象，瞬间代码的扩展性就变的好了很多。

但当对象之间的依赖关系很复杂的时候，使用手动依赖注入，事情会变得异常复杂。比如：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-05-11-153249.png" style="zoom:50%;" />

这个一个 MVP 的场景，我现在就是想在 LoginActivity 中使用 LoginPresenter 对象。

```java
// LoginActivity 
 public void onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_login)
  
     OkHttpClient okhttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .build();
   
     Retrofit retrofit = new Retrofit.Builder()
                .client(okhttpClient)
                .baseUrl("https://api.github.com")
                .build();
   
		UserApiService userApiService = retrofit.create(UserApiService.class);
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    UserManager userManager = new UserManager(preferences, userApiService);
    LoginModel loginModel = new LoginModel(userManager);
   
   	// 创建 Presenter 对象 在 Activity 中
    LoginPresenter presenter = new LoginPresenter(loginModel,this);
                 
  }

```

我就只是想在 Activity 里面创建 Presenter 对象，至于吗！！！太夸张了吧！！！对于 LoginActivity 本身来讲，不过是要一个 LoginPresenter 而已，然而它还需要知道 LoginPresenter 的 Dependency 是什么，LoginPresener 的 Dependency 的 Dependency 又是什么，然后先在 LoginActivity 中 new 出一堆东西，才不需要的它需要的、与它密切相关的对象创建出来。

1. 这违反了设计原则的 [迪米特原则（知道最少）]() ：一个类应该只关心它所关心的，只与它密切的类交流。
2. 可以预见的是上面有很多类，App 其他地方也需要它们：OKHttp、Retrofit、SharedPreferences、UserManger、也有可能在重用 Presenter 逻辑的情况下，其他地方也需要 LoginPresenter。像上面的使用方式，那么在其他地方也就需要 new 出这些对象，造成大量的代码重复，和不必须的对象生成。

要解决上面的问题，我们的**依赖注入框架**就登场了，想一想过往的开发你是否用到过依赖注入框架......，我保证你肯定用过，**ButterKnife** 依赖注入框架，只不过它是专门关注与 View 对的注入的。我们今天要说的依赖注入框架是应用于普遍对象的。了解更多的人可能知道，后端开发中依赖注入框架是比不可少的，**Spring IOC** 便是。

那么什么是依赖注入框架呢？很容易理解，它就是帮助我们从手动注入到自动完成注入（当然这需要一些配置），你就把它想象成以工厂类，这个工厂类可以给你提供你要的 Dependency，并且当 Dependency 创建时会自动创建 Dependency 所依赖的对象，依次类推。当你需要的 Dependency 创建完后就会注入的你当前的类中。刚开始接触依赖注入框架，你先不用了解框架是怎么创建依赖对象....依赖对象的依赖对象。生成对象的是一个工厂类，这个工厂类帮我们完成了在创建依赖对象过程中碰到的任何对象，记住它就是个工厂类。

前面说了真么多，我们要讲的依赖注入框架正真要登场了：**Dagger2** （我不知道你有么有注入在 ButterKnife 的github官网中 Jake Wharton 大神提到了 Dagger2，因为像 ButterKnife 的框架是参考 Dagger2 完成的。）Dagger2 是一个编译时的依赖注入框架，它不使用任何反射和运行时字节码生成技术，所有的分析都是在编译时完成，并通过生成 Java 代码来完成依赖注入。

[目前 Dagger2 已经被 Google 官方钦定了，构建一个好的应用的最佳做法之一就是使用 Dgger2 来进行依赖对象的管理。](https://developer.android.com/training/dependency-injection) 所有我们从下一篇就开始一起来了解 Dagger2 的使用。





