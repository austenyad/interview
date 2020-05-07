Android gcc :

/root/dev/sdk/ndk/android-ndk-r17c/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-gcc

NDK_CIFG_arm

/root/dev/sdk/ndk/android-ndk-r17c/platforms/android-21/arch-arm

/root/dev/sdk/ndk/android-ndk-r17c/sysroot/usr/include

/root/dev/sdk/ndk/android-ndk-r17c/sysroot/usr/include/arm-linux-androideabi

--sysroot

-isystem







##### ndk 编译动态库（.so 文件）

```shell
$NDK_GCC_arm $NDK_CFIG_arm -fPIC -shared get.c -o libget.so
```

##### ndk 编译静态库 (.a 文件)

编译静态库：

1. 先要编译 交叉编译 .o 文件 。
2. 在将 .o 文件交叉编译 为 .a 文件。

```shell
$NDK_GCC_arm NDK_CFIG_arm -fPIC -c get.c -o get.o
```

**静态库必须使用：arm-linux-androideabi-ar**

NDK_AR_arm：

/root/dev/sdk/ndk/android-ndk-r17c/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin/arm-linux-androideabi-ar

```shell
$NDK_AR_arm rcs -o libget.a get.o #所有的 .o 问价 *.o
```

