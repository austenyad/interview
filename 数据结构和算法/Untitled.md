归并排序和快速排序都用到了分治思想 时间复杂度 O(nlogn)

## 归并排序 

将待排序数组分成前后两部分，然后对前后两部分分别排序，再将排好序的两部分合并在一起，这样整个数组就有序了。

```java
public void srot(int[] array){
  
}
private void mergeSort(int[] array,int lo,int hi){
  if(lo >= hi) return;
  int mid = lo + (hi - lo)/2;
  mergeSort(array,lo,mid);
  mergeSort(array,mid,hi);
  merge(array,lo,mid,hi);
}
private void merge(int[] array,int lo,int mid,int hi){
  int i = lo;
  int j = mid + 1;
  int[] aux = new int[array.length];
 	for(int k = lo;k <= hi;k++){
    aux[k] = array[k];
  }
  
  for(int k = lo;k <= hi;k++){
    if(i > mid) array[k] = aux[j++];
    else if(j > hi) array[k] = aux[i++];
    else if(aux[i] < aux[j]) array[k] = aux[i++];
    else array[k] = aux[j++];
  }
}

```



## 快速排序

待排序数组中下标是从 p 到 r 之间的一组数据，我们选择 p 到 r 之间的任意一个数据作为 pivot（切分点）。

遍历 p 到 r 之间的数据，将小于 pivot 切分点的方在左边，将大于 pivot 切分点放到右边，将 pivot 放在中间。经过这一步骤之后， 数组 p 到 r 之间的数据被分为三部分，前面 p 到 q - 1 之间都是小于 pivot 的，中间是 pivot，后面的 q + 1 到 r 是大于 pivot 的。

递归地，把小于 pivot 元素的子数列和大于pivot 元素的子数列排序。

```java
public void sort(int[] array){
  quickSort( array , 0 , array.length -1 );
}
private void quickSort(int[] array,int lo,int hi){
  if(lo >= hi) return;
  int j = portition(array,lo,hi);
  quickSort(array,lo,j-1);
  quickSort(array,j+1,hi);
}

// 快速排序切分函数
private void portition(int[] array,int lo,int hi){
  int i = lo,j = hi++;
  int v = array[lo];
  
  while(true){
    while(v < array[++i]) if(i == hi) break;
    while(v > array[--j]) if(j == lo) break;
    if(i >= j) break;
    swap(array,i,j);
  }
  swap(array,lo,j);
  return j;
}

```

