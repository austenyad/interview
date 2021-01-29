# Drawable

### 1. Bitmap、View 和 Drawable 三者之间的关系？

Bitmap： 仅仅就是一个位图，可以理解为一张图片在内存中的映射。

View：View 最大的两个作用，一个是 `draw`  也就 canvas 的 `draw` 方法，还有个作用就是 **测量大小**。

Drawable：**它本身和 Bitmap 没有关系**，你可以把它理解为一个绘制工具，和 View 的第一个作用是一模一样的，你能用 View 的 canvas 画出来的东西 你用 drawable 一样可以画出来，**不一样的是 drawable 仅仅能绘制，但是不能测量大小** ，但 View 可以。换句话说 Drawable 承担了 View 的一般工作。

###  2. Drawable 是 View 的一半 那还用 drawable 干啥？

主要目的还是复用，假设你要自定义一组 View ，注意是一组，不是一个，那么你就可以把一组 View 中共同的部分提取成一个 Drawable ，这样 View 就可以复用这个 Drawable 了。不用重复写 canvas.draw 方法。

