# Lifecycle

* LifecycleObserver - 标记一个类作为 **生命周期观察者**

* Activity/Fragment - **生命周期拥有者**。

* 将 **观察者** 添加到 生命周期拥有者 当中，当 生命周期拥有者 自身的 生命周期方法调用时，观察者。

```java
for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator = mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
    considerNotify(iterator.next().getValue());
    if (mDispatchInvalidated) {
        break;
    }
}
```

