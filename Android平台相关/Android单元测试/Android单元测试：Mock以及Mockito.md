#### Mock 的概念

Mock 的概念，其实很简单，所谓的 Mock 就是创建一个类的虚拟对象，在测试环境中，用来替换掉真实对象，以达到两大目的：

1. 验证这个对象的某些方法的调用情况，调用多少次，参数是什么等等；
2. 指定这个对象的某些方法的行为，返回特定的值，或者是执行特定的动作。

#### Mockito 使用

##### 1. 验证方法的调用

* 验证一个对象的某个 method 得到调用的方法：

```java
Mockito.verify(mockUserManager)
    .performLogin("xiaochuang","xiaochuang password");
```

这句话的作用是，验证 mockUserManager 的 performLogin 得到调用，参数是 “xiaochuang” 和 "xiaochuang password"。

其实更精确的说法是，这行代码验证的是， mockUserManager 的 performLogin() 方法得到益处调用。

* 验证方法调用多次：

```java
  Mockito.verify(mockUserManager,Mockito.times(1))
                .performLogin("xiaochuang","xiaochuang password");
```

主要是 Mockio.times(1) 这个参数的作用，如果参数是 Mockio.times(3) 表示 performLogin 得到了三次调用。对于调用次数的验证，除了可以验证固定的多少次，还可以验证最多，最少，从来没有等等，方法分别是：`atMost(count)、atLeast(cont)、never()` 等等, 都是 Mockito 的静态方法，其实大部分时候我们会 static import Mockito 这个类的所有静态方法，这样就不用每次都加上 `Mockito.` 前缀了。

* 很多时候你并不关心被调用方法的参数具体是什么，或者是你也并不知道是传什么参数，你只关心这个方法得到调用就行。这种情况下，`Mockito` 提供了一系列的 `any` 方法，表示任何的参数都行：

```java
Mockito.verify(mockUserManager)
        .performLogin(Mockito.anyString(), Mockito.anyString());
```

`anyString()` 表示任何一个字符串都可以。null 也可以的！

类似的 `anyString()` ,还有 `anyInt`、`anyLong`、`anyDouble` 等等。 `anyObject` 表示任何对象，`any(clazz)` 表示任何 clazz 的对象。

更变态的是还有 `anyCollection` 、`anyCollectionOf(clazz)`、anyList(Map,set)，`anyListOf(clazz) ` 等等。

##### 2. 指定 mock 对象的某些方法的行为

指定某个方法的返回值，或者是执行特定的动作。

第一点：指定 mock 对象的某个方法返回特定的值。

我们现在假设上面的例子 `LoginPresenter`  的 `login` 方法是如下实现的：

```java
public void login(String username, String password) {
    if (username == null || username.length() == 0) return;
    //假设我们对密码强度有一定要求，使用一个专门的validator来验证密码的有效性
    if (mPasswordValidator.verifyPassword(password)) return;  //<==

    mUserManager.performLogin(null, password);
}
```

这里，我们有个 `PasswordValidator` 来验证密码的有效性，但是这个类的 `verifyPassword()` 方法运行需要很久，比如需要网络。这个时候在测试的环境下我们想简单处理，指定让它直接返回 `true` 或 `false` 。你可能会想这样做可以吗？真的好吗？回答是肯定的，因为我们要测试的是 `login()` 这个方法，让它返回特定值的写法如下：

`M`