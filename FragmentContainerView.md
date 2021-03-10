# FragmentContainerView

* 在 xml 中通过 name 属性来添加一个 Fragment，添加过程中会执行一次操作：

1. 创建一个 Fragment 实例
2. 调用 Fragment#onInflate 方法
3. 执行 FragmentTransactaion 去把实例化好的 Fragment 添加到合适的 FragmentManager

* 在 xml 中使用 tag 属性，给创建的 Fragment 添加一个 TAG，这时就可以通过 FragmentManager#findFragmentByTag() 来获取已经添加到 FragmentManager 中的 Fragment 实例。
* 