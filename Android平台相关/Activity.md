## Activity

* `onCreate()` 在创建启动时调用；
* `onStart()` 处于可见状态时调用；
* `onResume()` Activity 显示在 UI 顶层时被调用；
* `onPause()` Activity 不在 UI 顶层，但依然可见（如弹窗）；
* `onStop()` Activity 处于不可见状态时调用；
* `onDestory()` 当 Activity 退出时调用。

#### Activity 的不同生命周期流程

1. 正常流程周期。

启动：Activity: onCreate --->  onStart ---> onResume

销毁：Activity: onPause ---> onStop ---> onDestroy

2. 当 Activity 被其他 Activity 覆盖了一部分，或者手机被锁屏时，会判定位可见状态，但不在 UI 顶层时，会调用 onPuase 函数。当覆盖部分被去除或者屏幕解锁后，AMS 会调用该 Activity 的 onResume 方法来再次进入运行状态。
3. 当 Activity 跳转到新的 Activity 或者按 Home 键回到主屏幕时，会被压栈，处于不可见状态，会调用 onPause ---> onStop。当返回上一个 Activity 时，系统会调用 onRestart ---> onStart ---> onResume 再次进入到运行状态。
4. 当 Activity 出入被覆盖状态或者后台不可见状态时，当更高优先级的 App 需要内存且系统内存不足时会杀死此 Activity。当用户退回当前 Activity 时，系统会调用onCreate --->  onStart ---> onResume 进入运行状态。
5. 当使用 Back 键退出 Activity 时，系统会调用 onPause ---> onStop ---> onDestory。