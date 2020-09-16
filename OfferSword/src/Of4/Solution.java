package Of4;

import java.util.concurrent.ConcurrentHashMap;

public class Solution {
    public boolean findNumberIn2DArray(int[][] matrix, int target) {
        // 二维数组为 null 或者 二维数组的长度为 0
        if (matrix == null || matrix.length == 0) return false;

        int rows = matrix.length;
        int columns = matrix[0].length;

        int row = rows - 1;
        int column = 0;
        while (row >= 0 && column < columns) {
            int selected = matrix[row][column];
            if (selected == target) return true;
            else if (selected > target) row--;
            else column++;
        }
        return false;
    }

    public static void main(String[] args) {
        int[][] matrix = new int[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = i + j;
            }
        }
        new Solution().findNumberIn2DArray(matrix, 2);

        ConcurrentHashMap<String, String> concurrentHashMap = new ConcurrentHashMap<>();
        concurrentHashMap.put("10","111");
    }
}
