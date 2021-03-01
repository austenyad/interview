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

# ViewModelStore

ViewModleStoreOwner - 实现此接口的职责是在配置更改期间保留拥有的ViewModelStore，并在该作用域将要销毁时调用 ViewModelStore#clear。

ViewModelStore - 存储 ViewModel，当 ViewModelStore 实例由于 Actvity 配置改变而导致重建，重新创建好的新的 Activity 实例 仍然具有相同的 旧实例 ViewModelStore。

```java
// 当已知将立即为新配置创建新实例时，由系统调用，作为由于配置更改而破坏活动的一部分。
public Object onRetainNonConfigurationInstance() {
    return null;
}
```

```java
// 检索先前由{@link onRetainNonConfigurationInstance（）}返回的非配置实例数据
可以通过对新实例的初始{@link onCreate}和{@link onStart}调用来使用它，从而使您可以从先前的实例中提取任何有用的动态状态。
@Nullable
public Object getLastNonConfigurationInstance() {
    return mLastNonConfigurationInstances != null
            ? mLastNonConfigurationInstances.activity : null;
}
```

ViewModel - 

Activity#isChangingConfigurations

```java
# ComponentActivity 
getLifecycle().addObserver(new LifecycleEventObserver() {
    @Override
    public void onStateChanged(@NonNull LifecycleOwner source,
            @NonNull Lifecycle.Event event) {
        if (event == Lifecycle.Event.ON_DESTROY) {
            if (!isChangingConfigurations()) {
                getViewModelStore().clear();
            }
        }
    }
});

    /** true if the activity is being destroyed in order to recreate it with a new configuration */
    /*package*/ boolean mChangingConfigurations = false;

#ViewModel
    /**
     * This method will be called when this ViewModel is no longer used and will be destroyed.
     * <p>
     * It is useful when ViewModel observes some data and you need to clear this subscription to
     * prevent a leak of this ViewModel.
     */
    @SuppressWarnings("WeakerAccess")
    protected void onCleared() {
    }

```