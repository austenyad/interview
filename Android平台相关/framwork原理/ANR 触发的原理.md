# ANR 触发的原理

* 了解 ANR 的触发原理
* 了解应用的大致启动流程
* 了解线程的消息循环机制
* 了解应用和系统服务通信过程



AMS 里面弹的 AMS 在 system_server 进程

![image-20201011143948938](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-10-11-063950.png)

* Service Timeout
* BroastQueue Timeout
* ContentProvider Timeout
* InputDispatching Timeout 输入事件超时

系统让应用干一件事，结果应用在系统规定的时间内没有干完，系统就认为 ANR 了

