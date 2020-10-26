package L26;

public class Solution {

    /**
     *
     * @param nums
     * @return
     */
    public int removeDuplicates(int[] nums) {
        if (nums.length == 0) return 0;
        int o = 0;
        for (int i = 1; i < nums.length; i++) {
            int val = nums[i];
            if (val != nums[o]) {
                o++;
                nums[o] = nums[i];
            }
        }
        return o + 1;
    }

    public static void main(String[] args) {
        Object[] object1 = new Object[10];
        Object[] object2 = new Object[10];
        object1[1] = object2;

    }
}
