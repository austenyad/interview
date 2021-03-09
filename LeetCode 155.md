## LeetCode 15

给定两个字符串 S 和 T，从 S 中选择字母，使得刚好和 T 相等，有多少种选法。

### 解法一：递归之分治

S 中的每个字母就是两种可能 **选中** 或 **不选中**。我们用递归的常规思路，将大问题化成小问题，也就是分治的思想。

如果我们求 `S[0,S_len - 1]` ，中能选出多少个 `T[0,T_len - 1]`，个数记为 `n` 。那么分两种情况，

*  `S[0] == T[0]` ，需要知道两种情况

  * 从 S 中选择当前的字母，此时 S 跳过这个字母，T 也跳过这个字母。

  去求 `S[1,S_len - 1]`中能选出多少个 `T[1,T_len - 1]`，记为 `n1`

  * S 不选当前字母，此时 S 跳过这个字母，`T` 不跳过这个字母。（T 就不能跳过）

  去求 `S[1,S_len - 1]` 中能选出多少个 `T[0,T_len - 1]` ,个数记为 `n2`

* `S[0] != T[0]`

  S 只能不选当前字母，此时 S 跳过这个字母，T 不跳过字母

  去求 `S[1,S_len - 1]` 中能选出多少个 `T[0,T_len - 1]`，这个记为 `n1`

也就是说如果求 `S[0,S_len - 1]` 中能选出多少个 `T[0,T_len - 1]` ，个数记为 n。转换为数学公式就是

`if(S[0] == T[0])`

```java
if(S[0] == T[0]){
    n = n1 + n2;
}else{
    n = n1;
}
```

推广到一般情况，我们可以写出递归的部分代码。

```java
public int numDistinct(String s,String t){
    return numDistinctHelper(s,0,t,0);
}

private int numDistinctHelper(String s,int s_start,String t,int t_start){
    int count = 0;
    if(s.chatAt(s_start) == t.chatAt(t_start)){
        count = numDistinctHelper(s,s_start + 1,t,t_start + 1)
           + numDistinctHelper(s,s_start + 1，t,t_start);
    }else{
        count = numDistinctHelper(s,s_start + 1,t,t_start + 1);
    }
    return count;
}
```

递归出口的话，因为我们的 S 和 T 的开始下标都是增长的。

如果 `S[s_start,S_len - 1]` 中，`s_start` 等于 `S_len` ，意味着 S 是空串，从空串中选字符串 T，那么肯定是 0。

如果 `T[t_start,T_len - 1]` 中，`t_start` 等于了 `T_len` ，意味者 `T` 是空串，从 `S` 中选则空字符串，只需要不选择 S 中所有字母，所以选法是 1。

综上，代码总体就是下边的样子

```java
public int numDistinct(String s,String t){
    return numDistinctHelper(s,0,t,0);
}

private int numDistinctHelper(String s,int s_start,String t,int t_start){
 
      //T 是空串，选法就是 1 种
    if(t_start == t.length()){
        return 1;
    }
       //S 是空串，选法是 0 种
    if(s_start == s.length()){
        return 0;
    }
    int count = 0;
    //当前字母相等
    if(s.chatAt(s_start) == t.chatAt(t_start)){
        //从 S 选择当前的字母，此时 S 跳过这个字母, T 也跳过一个字母。
        count = numDistinctHelper(s,s_start + 1,t,t_start + 1)
               //S 不选当前的字母，此时 S 跳过这个字母，T 不跳过字母。
           + numDistinctHelper(s,s_start + 1，t,t_start);
    }else{
         //当前字母不相等
        //S 只能不选当前的字母，此时 S 跳过这个字母， T 不跳过字母。
        count = numDistinctHelper(s,s_start + 1,t,t_start + 1);
    }
    return count;
}
```

上面求解这个问题的思路是没有问题的，遗憾的是，这个解法对于 S 太长的情况下会超时。

`

原因就是应为递归函数中，我们每次调用了递归函数，这会使得我们重复递归的过程很多，解决方案就很简单了，`Memoization` 技术，把每次的结果利用一个 map 保持起来，在求解之前，先看看 map 中有没有，有的话直接拿出来就可以了。

map 的 key 的话就标识当前的递归，`s_start` 和 `t_start` 联合表示，利用字符串 `s_start+@+t_start`

`value` 就保存每次递归返回的 `count`

```java
private int numDistinctHelper(String s,int s_start,String t,int t_start,HashMap<String,Integer> map){
    //T 是空串，选法就是 1 种
    if(t_start == t.length()){
        return 1;
    }
      // S 是空串，选法是 0 种
    if(s_start == s.length()){
        return 0;
    }
    
    String key = s_start + "@" + t_start;
    // 判断之前有没有求解过这个解
    if(map.containsKey(key)){
        return map.get(key);
    }
    int count = 0;
    //当前字母相等
    if(s.chatAt(s_start) == t.chatAt(t_start)){
        // 从 S 中选中当前字母，此时 S 跳过这个字母，T 也跳过这个字母
       count = numDistinctHelper(s,s_start + 1,t,t_start + 1,map) + 
           // S 不选择当前字母，此时 S 跳过这个字母，T 不跳过。
           numDistincHelper(s,s_start + 1,t,t_start,map);
    }else{
        //当前字母不相等
        // S 只能不选择当前字母，此时 S 跳过这个字母，T 不跳过字母。
        count = numDistinctHelper(s,s_start + 1,t,t_start,map);
    }
    
}
```

**注意：为什么这个递归退出的条件有先后顺序关系，我在提交的时候，就将 退出条件调转，答案竟然是错的。**