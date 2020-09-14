1）ArrayMap 对象的数据存储格式如下图所示：

* mHashes 是记录所有 key 的 hashcode 值所组成的数组，是从小到大的排列方式；
* mArray 是存放键值对 key-value 所组成是数组，是 mHashes 大小的 2 倍。

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-13-130230.png" alt="image-20200913210229104" style="zoom:50%;" />

2) ArrayMap 有两个非常重要的静态成员变量 `mBaseCache` 和 `mTwiceBaseCache`，用于 ArrayMap 所在进程的全局缓存功能：

* `mBaseCache` 用于缓存大小为4的ArrayMap
* `mTwiceBaseCache`用于缓存大小为8的ArrayMap
* mBaseCacheSize 和 mTwiceBaseCacheSize 分别记录 `mBaseCache` 和 `mTwiceBaseCache` 缓存的 ArrayMap 的个数，个数都不能超过 10 个。

为了减少频繁地创建和回收Map对象，ArrayMap采用了两个大小为10的缓存队列来分别保存大小为4和8的Map对象。为了节省内存有更加保守的内存扩张以及内存收缩策略。 接下来分别说说缓存机制和扩容机制。

### 2.2 缓存机制

ArrayMap 是为了Android 优化而设计的 Map 对象，使用场景比较高频，很多场景可能起初都是数据很少，为了减少频繁的创建和回收，特意设计了两个缓存池，分别缓存大小为 4 和 8 的 ArrayMap对象。要理解这种缓存机制，哪就需要看看内存分配（allocArrays）和内存释放（freeArrays）

#### 2.2.1 freeArrays

```java
private static void freeArrays(final int[] hashes, final Object[] array, final int size) {
    if (hashes.length == (BASE_SIZE*2)) {
        synchronized (ArrayMap.class) {
            if (mTwiceBaseCacheSize < CACHE_SIZE) {
                array[0] = mTwiceBaseCache;
                array[1] = hashes;
                for (int i=(size<<1)-1; i>=2; i--) {
                    array[i] = null;
                }
                mTwiceBaseCache = array;
                mTwiceBaseCacheSize++;
                if (DEBUG) Log.d(TAG, "Storing 2x cache " + array
                        + " now have " + mTwiceBaseCacheSize + " entries");
            }
        }
    } else if (hashes.length == BASE_SIZE) { // 当要释放数组的大小为 4 时，进入
        synchronized (ArrayMap.class) {
            if (mBaseCacheSize < CACHE_SIZE) { // 大小为 4 的缓存池，数量小于10个 则将其放入缓存池
                array[0] = mBaseCache;//array[0]指向原来的缓存池
                array[1] = hashes;//
                for (int i=(size<<1)-1; i>=2; i--) {
                    array[i] = null; //清空其他数据
                }
                mBaseCache = array; //mBaseCache指向新加入缓存池的array
                mBaseCacheSize++;
                if (DEBUG) Log.d(TAG, "Storing 1x cache " + array
                        + " now have " + mBaseCacheSize + " entries");
            }
        }
    }
}
```

最初 mTwiceBaseCache 和 mBaseCache 缓存池都没有数据，在 `freeArrays` 释放内存时，如果同时满足释放的 array 大小等于 4 或 8 ，且对应的缓存池个数未达到上限，则会把 array 放到缓存池中。

加入的方式是将第0个元素指向原有的缓存池，第一个元素指向hashes数组的地址，第二个元素以后全部置为 null。再把缓存池的头部指向新的 array 的位置，并且将缓存池大小进行加1操作，具体如下图：

![image-20200913211600808](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-13-131602.png)

#### 2.2.2 allocArrays

```java
@UnsupportedAppUsage(maxTargetSdk = 28) // Allocations are an implementation detail.
private void allocArrays(final int size) {
    if (mHashes == EMPTY_IMMUTABLE_INTS) {
        throw new UnsupportedOperationException("ArrayMap is immutable");
    }
    if (size == (BASE_SIZE*2)) {
        synchronized (ArrayMap.class) {
            if (mTwiceBaseCache != null) {
                final Object[] array = mTwiceBaseCache;
                mArray = array;
                mTwiceBaseCache = (Object[])array[0];
                mHashes = (int[])array[1];
                array[0] = array[1] = null;
                mTwiceBaseCacheSize--;
                if (DEBUG) Log.d(TAG, "Retrieving 2x cache " + mHashes
                        + " now have " + mTwiceBaseCacheSize + " entries");
                return;
            }
        }
    } else if (size == BASE_SIZE) { //当要申请的新数组大小为 4 时，从缓存池中去取已经存在的数组
        synchronized (ArrayMap.class) {
            if (mBaseCache != null) {//缓存池不为空，可以取数组
                final Object[] array = mBaseCache;
                mArray = array; // 从缓存池中取出 mArray
                mBaseCache = (Object[])array[0]; // 将缓存池指向上一条缓存地址
                mHashes = (int[])array[1];// 从缓存中取出 mHashes 
                array[0] = array[1] = null;
                mBaseCacheSize--;//缓存池大小减1
                if (DEBUG) Log.d(TAG, "Retrieving 1x cache " + mHashes
                        + " now have " + mBaseCacheSize + " entries");
                return;
            }
        }
    }

    mHashes = new int[size];
    mArray = new Object[size<<1];
}
```

当 allocArrays 分配内存时，如果所需要分配新数组的大小为 4 或 8 ，且对应的缓存池不为空，则会从相应的缓存池中取出缓存的 mArrays 和 mHash 。从缓存池取出缓存方式是将当前缓存池赋值给 mArray，将缓存池指向上一条缓存地址，将缓存池的第一个元素赋值为 mHashes，在把 mArrays 地第0个和第1个位置的元素置为null，并将该缓存池大小执行减1操作，具体如下图：

<img src="https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-13-154520.png" alt="image-20200913234518331" style="zoom:50%;" />







其中 `mHashes` 是一个整形数组，用于存储所有 key 的哈希值；而 `mArray` 用于存储 key 和 value 。

### 构造方法

ArrayMap 暴露了三个构造方法，如下面代码所示，最终都会调用到有两个参数的构造方法，但是该构造方法被 @hide 标记了，因此不能直接调用。通过构造函数的调用关系可以看到 `mIdentityHashCode` 始终是 `false` 的，也就是允许哈希冲突。

```java
public ArrayMap() {
    this(0, false);
}
public ArrayMap(int capacity) {
    this(capacity, false);
}

/** {@hide} */
public ArrayMap(int capacity, boolean identityHashCode) {
    mIdentityHashCode = identityHashCode;

    // If this is immutable, use the sentinal EMPTY_IMMUTABLE_INTS
    // instance instead of the usual EmptyArray.INT. The reference
    // is checked later to see if the array is allowed to grow.
    if (capacity < 0) {
        mHashes = EMPTY_IMMUTABLE_INTS;
        mArray = EmptyArray.OBJECT;
    } else if (capacity == 0) {
        mHashes = EmptyArray.INT;
        mArray = EmptyArray.OBJECT;
    } else {
        allocArrays(capacity);
    }
    mSize = 0;
}

    public ArrayMap(ArrayMap<K, V> map) {
        this();
        if (map != null) {
            putAll(map);
        }
    }
```

在构造方法中，除了对 `capactity <= 0` 的情况特殊处理外，主要调用了 `allocArrays` 方法来创建数组并赋值给 `mHashes` 和 `mArray`。

allocArrays 方法代码如下：

```java
@UnsupportedAppUsage(maxTargetSdk = 28) // Allocations are an implementation detail.
private void allocArrays(final int size) {
    if (mHashes == EMPTY_IMMUTABLE_INTS) {
        throw new UnsupportedOperationException("ArrayMap is immutable");
    }
    if (size == (BASE_SIZE*2)) {
        synchronized (ArrayMap.class) {
            if (mTwiceBaseCache != null) {
                final Object[] array = mTwiceBaseCache;
                mArray = array;
                mTwiceBaseCache = (Object[])array[0];
                mHashes = (int[])array[1];
                array[0] = array[1] = null;
                mTwiceBaseCacheSize--;
                if (DEBUG) Log.d(TAG, "Retrieving 2x cache " + mHashes
                        + " now have " + mTwiceBaseCacheSize + " entries");
                return;
            }
        }
    } else if (size == BASE_SIZE) {
        synchronized (ArrayMap.class) {
            if (mBaseCache != null) {
                final Object[] array = mBaseCache;
                mArray = array;
                mBaseCache = (Object[])array[0];
                mHashes = (int[])array[1];
                array[0] = array[1] = null;
                mBaseCacheSize--;
                if (DEBUG) Log.d(TAG, "Retrieving 1x cache " + mHashes
                        + " now have " + mBaseCacheSize + " entries");
                return;
            }
        }
    }

    mHashes = new int[size];
    mArray = new Object[size<<1];
}
```

乍一看 allocArrays 的前面一部分代码可能有点懵，可以先不要陷入细节里面。这里我们只要知道，如果要 ArrayMap 的容量是 `BASE_SIZE` 或者 `BASE_SIZE` 的 2 倍，那么就优先利用已经缓存或的数组，如果没有缓存数组或者申请的数组长度不符合这两种情况，再创建新数组。至于数组怎么被缓存和复用的，后面会详细解释。

通过最后两行代码可以看到 mArray 的容量是 mHashes 的 2 倍，这和 ArrayMap 如何存储 key 和 value 有关系，随着对 put 方法的研究，所有疑惑都会解开。

### put 

put 方法是重写自 Map 接口的，用于存入一个键值对。在 key 存在时会更新 value 的值返回旧的 value，在 key 不存在时就插入 key 和 value 返回 null。

```java
@Override
public V put(K key, V value) {
    final int osize = mSize;
    final int hash;
    int index;
  // 在 mHashes 数组中查找对应的 hash 值的下标
    if (key == null) {
        hash = 0;
        index = indexOfNull();
    } else {
      //前面构造方法可以看到，mIdentityHashCode 始终为 false，因此 hash = key.hashcode();
        hash = mIdentityHashCode ? System.identityHashCode(key) : key.hashCode();
        index = indexOf(key, hash);
    }
  // index >= 0 表示数组下标有效，即存在这个key对应的 hashcode.
    if (index >= 0) {
       //index 为 key 的 hash 在 mHashes 数组中的下标
      // 在 mArray 数组中，键所在的下标为 index * 2,值所在下标为 index*2+1
        index = (index<<1) + 1;
        final V old = (V)mArray[index];
        mArray[index] = value;
      // 返回旧值
        return old;
    }
	// 在 mHashes 中没有对应的key，执行插入
  // index 为应该要插入的位置
    index = ~index;
    if (osize >= mHashes.length) {
      //数组容量不足，需要先扩容
      //扩容的策略为 如果当前大小为 BASE_SIZE*2 = 8，那么扩容为原来的1.5倍 //如果 当前大小小于8但是大于4，那么扩容后数组大小为8；
      // 如果当前数组大小小于4，那么扩容为4
        final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
                : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);

        if (DEBUG) Log.d(TAG, "put: grow from " + mHashes.length + " to " + n);

        final int[] ohashes = mHashes;
        final Object[] oarray = mArray;
      // 通过 allocArrays 创建数组并赋值给 mHashes 和 mArray
        allocArrays(n);

        if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
          // 存在并发修改抛出异常
            throw new ConcurrentModificationException();
        }

        if (mHashes.length > 0) {
            if (DEBUG) Log.d(TAG, "put: copy 0-" + osize + " to 0");
          // 从旧数组拷贝到新数组
            System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
            System.arraycopy(oarray, 0, mArray, 0, oarray.length);
        }
		// 释放数组空间。
        freeArrays(ohashes, oarray, osize);
    }

    if (index < osize) {
        if (DEBUG) Log.d(TAG, "put: move " + index + "-" + (osize-index)
                + " to " + (index+1));
      // 在数组中间插入，需要移动插入位置后面的元素
        System.arraycopy(mHashes, index, mHashes, index + 1, osize - index);
        System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
    }

    if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
        if (osize != mSize || index >= mHashes.length) {
           //存在并发修改，抛出异常
            throw new ConcurrentModificationException();
        }
    }
  //插入新值到 mHashes 和 mArray
    mHashes[index] = hash;
    mArray[index<<1] = key;
    mArray[(index<<1)+1] = value;
    mSize++;
    return null;
}
```

put 方法的主要逻辑为：**先根据 key 的哈希值在 mHashes 数组中查找这个哈希值是否存在，如果存在且 mArray 对应的位置也存在该 key，那么更新 value 并返回旧值的 value；否则，就执行插入，必要是进行数组的扩容。**

### 查找为 null 的 key

对于 key 为 null 时的查找，调用了 `indexOfNull` 方法，该方法代码如下：

```java
@UnsupportedAppUsage(maxTargetSdk = 28) // Use indexOf(null)
int indexOfNull() {
    final int N = mSize;

    // Important fast case: if nothing is in here, nothing to look for.
  //如果没有数据，直接返回
    if (N == 0) {
        return ~0;
    }
	// 在 mHashes 数组的 0~N-1范围内，使用二分查找法查找0是否存在。
    int index = binarySearchHashes(mHashes, N, 0);

    // If the hash code wasn't found, then we have no entry for this key.
  // 没有找到
    if (index < 0) {
        return index;
    }

    // If the key at the returned index matches, that's what we want.
  // mHashes 数组中存在 0
  // 并且 mArray 对应的位置也是 null，返回该下标
    if (null == mArray[index<<1]) {
        return index;
    }

    // Search for a matching key after the index.
  // 在 mHashes 数组中存在 0 ,但是 mArray 对应的位置 key 不是 null
  // 存在哈希冲突，继续向后查找
    int end;
    for (end = index + 1; end < N && mHashes[end] == 0; end++) {
        if (null == mArray[end << 1]) return end;
    }
//mHashes 数组中存在 0，但 mArray 对应的位置 key 不是 null
  // 存在哈希冲突，继续向前查找
    // Search for a matching key before the index.
    for (int i = index - 1; i >= 0 && mHashes[i] == 0; i--) {
        if (null == mArray[i << 1]) return i;
    }

    // Key not found -- return negative value indicating where a
    // new entry for this key should go.  We use the end of the
    // hash chain to reduce the number of array entries that will
    // need to be copied when inserting.
  // 没有找到，把第一个等于hash的下标取反后返回
    return ~end;
}
```

代码中先调用 `binarySearchHashes` 通过二分查找法在 `mHashes` 中查找是否存在 0（null 对应的哈希值），二分查找法 `binarySearchHashes` 代码如下:

```java
private static int binarySearchHashes(int[] hashes, int N, int hash) {
    try {
        return ContainerHelpers.binarySearch(hashes, N, hash);
    } catch (ArrayIndexOutOfBoundsException e) {
        //CONCURRENT_MODIFICATION_EXCEPTIONS=true
        if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
            throw new ConcurrentModificationException();
        } else {
            throw e; // the cache is poisoned at this point, there's not much we can do
        }
    }
}
```

可以看到调用了 `ContainerHelpers.binarySearch`，该方法源码如下：

```java
static int binarySearch(int[] array, int size, int value) {
    int lo = 0;
    int hi = size - 1;

    while (lo <= hi) {
        final int mid = (lo + hi) >>> 1;
        final int midVal = array[mid];

        if (midVal < value) {
            lo = mid + 1;
        } else if (midVal > value) {
            hi = mid - 1;
        } else {
            return mid;  // value found
        }
    }
    return ~lo;  // value not present
}
```

这个方法在 分析 SparseArray 时已经解释过它的妙处了，当没有找到目标值时，**会将第一个大于目标值的小标取反后返回，这样调用方对返回结果再次取反就可以得到这个下标值，这样做同时也可以保证 mHashes 是有序的**

如果在 `mHashes` 中找到了 hash，但是在 mArray 中对应位置没有找到 key，那么还需要进一步进行搜索，为了方便阅读，我把源码再贴一遍：

```java
// mHashes 数组中存在 0，但 mArray 对应的位置 key 不是 null
// 存在哈希冲突，继续向后查找
int end;
for (end = index + 1; end < N && mHashes[end] == 0; end++) {
    if (null == mArray[end << 1]) return end;
}

// mHashes 数组中存在 0，但 mArray 对应的位置 key 不是 null
// 存在哈希冲突，继续向前查找
for (int i = index - 1; i >= 0 && mHashes[i] == 0; i--) {
    if (null == mArray[i << 1]) return i;
}

// 没有找到，把第一个等于 hash 的下标取反后返回
return ~end;
```

我们可以举一个例子来屡一下上面的代码逻辑，假设 mHashes 中的元素为 [-2,-1,0,0,0,0,3,4]（也就是说四个key的哈希值都是0），要查找哈希值是0，那么通过二分查找法，会先返回下标3，也就是第二个0，如果 mArray 中对应位置的key 不是null，就会执行上面代码的逻辑，先向后搜索，假设直到最后一个0依然没有在 mArray 中找到 null，那么此时 **end=6**,也就是元素 3 的位置。

接下来向前搜索，假设也没有找到 null，那么此时就返回 -7（对 6 按位取反的结果）。

那么为什么不返回第一等于哈希值的下标呢？

因为当插入新 key 时，需要移动插入位置以及后面的元素，在上面例子中，如果返回的下标是6，这时只要移动 3 和 4 两个元素就可以了，如果返回第一个 0 的下标，那么需要移动的元素就是 6 个，所以这么做是为了减少插入新key时向后移动元素的数量。

**综上，对于 key 不存在的情况，indexOfNull 会返回一个负值，这个负值是将 mHashes 中第一个大于 key 的哈希值的下标取反后得到。**

通过上面的代码可以看出 **ArrayMap 使用了线性探测法处理哈希冲突。**

### 查找不是 null 的 key

对于不为 null 的 key ，调用 `indexOf(key,hash)` 来查找，该方法代码如下：

```java
int indexOf(Object key, int hash) {
    final int N = mSize;

   	// 没有数据直接返回
    if (N == 0) {
        return ~0;
    }

  // 使用二分查找法在 mHashes 数组中查找 hash
    int index = binarySearchHashes(mHashes, N, hash);

   // hash 没有找到，不存在映射对 
    if (index < 0) {
        return index;
    }

   
 // hash 存在且 mArray 对应位置的 key 匹配
    if (key.equals(mArray[index<<1])) {
        return index;
    }
 // hash 存在但是 key 不相匹配，继续向后搜索
    // Search for a matching key after the index.
    int end;
    for (end = index + 1; end < N && mHashes[end] == hash; end++) {
        if (key.equals(mArray[end << 1])) return end;
    }

    // hash 存在但是key 不匹配，继续向前搜索
    for (int i = index - 1; i >= 0 && mHashes[i] == hash; i--) {
        if (key.equals(mArray[i << 1])) return i;
    }

// 没有找到符合的key，返回负数。同时把第一个不等于hash的下标返回
    // 以便下次插入时尽量少的移动元素
    return ~end;
}
```

indexOf 和 indexOfNull 的逻辑是一样的，区别就是在比较 key 时是通过 `equals` 方法进行的。

### 更新已存在 key 对应的 value

当 index >= 0 时，也就是 ArrayMap 中已经存在相同 key 的映射，只需要更新值就可以了，

```java
if (index >= 0) {
    index = (index<<1) + 1;
    final V old = (V)mArray[index];
    mArray[index] = value;
    return old;
}
```

上面几行代码的重点是，对于 **HashCode 在 mHashes 数组中的下标为 index 的 key ,对应的 value 在 mArray 数组中的下标为 index*2+1**

这样我们就搞清楚了 ArrayMap 到底是怎么存储 hashcode、key 和 value 的，它们之间的关系如下图所示：

![image-20200913191912411](https://note-austen-1256667106.cos.ap-beijing.myqcloud.com/2020-09-13-111914.png)

### 插入新的映射

在 put 方法中，当 key 不存在时，在执行插入前，就有这么一句代码：

```java
index = ~index;
```

前面看到，对于当 key 不存时，查找时返回一个负数，**这个负数就是 mHashes 数组中第一个大于 key 的哈希值的下标按位取反后的值。这里通过再次取反，就得到了这个下标，也就是 key 要插入的位置。**

确定了新映射要插入的位置，如果 mHashes 容量充足，直接把新映射的 hashcode、key、value 加入到数组中就可以了。

插入新值的代码如下：

```java
if (index < osize) {
  //在数组中间插入，需要移动待插入位置以及后面的元素
    System.arraycopy(mHashes, index, mHashes, index + 1, osize - index);
    System.arraycopy(mArray, index << 1, mArray, (index + 1) << 1, (mSize - index) << 1);
}

if (CONCURRENT_MODIFICATION_EXCEPTIONS) {
    if (osize != mSize || index >= mHashes.length) {
        throw new ConcurrentModificationException();
    }
}
// 插入新值到 mHashes 和 mArray
mHashes[index] = hash;
//key 的下标为 2*index
//value 的下标为 2*index+1
mArray[index<<1] = key;
mArray[(index<<1)+1] = value;
mSize++;
```

这部分代码比较容易理解，不过在这是数组大小已经满足需求了，对于不满足的情况需要先进行扩容。

### 数组的扩容

数组扩容部分的代码如下：

```java
if (osize >= mHashes.length) {
    final int n = osize >= (BASE_SIZE*2) ? (osize+(osize>>1))
            : (osize >= BASE_SIZE ? (BASE_SIZE*2) : BASE_SIZE);
    final int[] ohashes = mHashes;
    final Object[] oarray = mArray;
    allocArrays(n);

    if (CONCURRENT_MODIFICATION_EXCEPTIONS && osize != mSize) {
        throw new ConcurrentModificationException();
    }

    if (mHashes.length > 0) {
        if (DEBUG) Log.d(TAG, "put: copy 0-" + osize + " to 0");
        System.arraycopy(ohashes, 0, mHashes, 0, ohashes.length);
        System.arraycopy(oarray, 0, mArray, 0, oarray.length);
    }

    freeArrays(ohashes, oarray, osize);
}
```

扩容策略为：

1. 如果当前 ArrayMap 的 size 大于 8（BASE_SIZE*2），那么就扩容为原来的 1.5 倍
2. 如果 size 小于 8 但是大于 4 （BASE_SIZE），那么扩容后的数组为 8
3. 如果 size 小于 4，那么扩容后为 4。

扩容后的容量大小确定后，通过 allocArrays 方法创建数组并把新数组赋值给 mHashes 和 mArray。

这么分析构造方法时，已经看到了这个方法，不过只看了大概逻辑，这个仔细分析下，该方法代码如下：

```java
private void allocArrays(final int size) {
    if (mHashes == EMPTY_IMMUTABLE_INTS) {
        throw new UnsupportedOperationException("ArrayMap is immutable");
    }
    //优先利用缓存的数组
    if (size == (BASE_SIZE*2)) {
        synchronized (ArrayMap.class) {
            if (mTwiceBaseCache != null) {              
                final Object[] array = mTwiceBaseCache;
                mArray = array;              
                mTwiceBaseCache = (Object[])array[0];             
                mHashes = (int[])array[1];              
                array[0] = array[1] = null;               
                mTwiceBaseCacheSize--;
                return;
            }
        }
    } else if (size == BASE_SIZE) {
        synchronized (ArrayMap.class) {
            if (mBaseCache != null) {
                //1.
                final Object[] array = mBaseCache;
                mArray = array;
                //2.
                mBaseCache = (Object[])array[0];
                //3.
                mHashes = (int[])array[1];
                //4.
                array[0] = array[1] = null;
                //5.
                mBaseCacheSize--;
                return;
            }
        }
    }
    //没有缓存的数组或者缓存的数组长度不满足条件
    mHashes = new int[size];
    //mArray 的容量是 size 的 2 倍
    mArray = new Object[size<<1];
}
```

