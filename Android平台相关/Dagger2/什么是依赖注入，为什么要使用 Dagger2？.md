## 什么是依赖注入，为什么要使用 Dagger2？

开篇：我想让你想一想依赖是什么？是不是在面向对象的世界里面，一个类使用一个类去完成某一项任务，是我们每一天都要使用的方式。尤其是在面向对象这种编程范式中，你要完成一项任务，类与类之间的协作肯定是避免不了的。依赖就是正在使用的类依靠另一个类的这种类与类之间的关系。

下面在讲解这种依赖关系时，当前对象即要使用被依赖类的类称为 Client 类，被依赖的类称 Dependency。你仔细思考，是不是每天都在面对这种关系，处理两个类之间的关系。而我们说的依赖注入 Dependency Injection (简称 DI)，它是一种具体的编程技巧。感觉很高大上的概念，但它其实很简单，我用一句化来说明：不通过 new() 的方式在类内部创建依赖对象，而是将依赖的对象在外部创建好。通过构造方法注入（*Constructor Injection*）、方法的参数注入（*argument Injection* ）。

下面还是通过一个例子来解释一下。我们在使用 MVP 模式的过程中，我们知道 P 层「Presenter」会依赖 M 层「Model」,这里是做登录功能，我们有个用户管理的类 UserManager 类，而 UserManger 它是处理与用相关的业务。而我们的 M 层需要依赖 UserManger 去完成登录操作。我简单梳理一下依赖关系。

<img src="/Users/austen/Desktop/interview/Android平台相关/Dagger2/Screenshot/Screen Shot 2020-05-07 at 07.44.24.png" style="zoom:40%;" />

现在我们只管 LoginModel 类和 UserManger 之间的依赖关系。

```java
//非注入方式
public class LoginModle{
  private UserManger userManger;
  
  public LoginModel(){
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

// 使用LoginModle
LoginModel modle = new LoginModle();


//依赖注入的方式
public class LoginModle{
  private UserManger userManger;
  //通过构造方法进行依赖注入
  public LoginModel(UserManger userManger){
    this.userManger = userManger;
  }
  
  public void login(String moblie,String pwd){
    userManger.login(moblie,pwd);
   //....逻辑省略
  } 
}

//使用LoginModle
UserManger userManager = new UserManger();
LoginModle model = new LoginModle(userManager);
```

你思考一下是不是就是简单的通过外部注入对象的方式，代码立即变得扩展性更强了。我们可以灵活的替换这个依赖类。这个代码还可以进一步优化，当业务复杂起来，你可以把 UserManger 定义成接口，基于基于接口而非实现编程。