package L437;

import Base.TreeNode;

public class Solution2 {
    public int pathSum(TreeNode root, int sum) {
        if (root == null) return 0;

        return dfs(root, sum) + dfs(root.left, sum) + dfs(root.right, sum);
    }

    private int dfs(TreeNode node, int sum) {
        if (node == null)  return  0;
        sum = sum - node.val;
        int result = sum == 0 ? 1 : 0;
        return result + dfs(node.left, sum) + dfs(node.right, sum);
    }


}
