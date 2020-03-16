### Scoped Storage

* 内部存储：`/data` 目录。一般使用 `getFilesDir()` 或 `getCacheDir()` 方法获取应用内部路径，读写该路径下的文件不需要申请存储空间读写权限，且卸载应用时自动删除。
* 外部存储：`/storage` 或 `/mnt` 目录。一般使用 `getExternalStorageDirectory()` 方法获取的路径来存取文件。