package com.ygyin.coop.util;

import java.awt.*;

/**
 * 用于计算颜色 RGB 相似度的工具类
 */
public class ColorUtil {

    private ColorUtil() {}

    /**
     * 计算两个颜色的相似度
     *
     * @param color1 第一个颜色
     * @param color2 第二个颜色
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(Color color1, Color color2) {
        int r1 = color1.getRed();
        int g1 = color1.getGreen();
        int b1 = color1.getBlue();

        int r2 = color2.getRed();
        int g2 = color2.getGreen();
        int b2 = color2.getBlue();

        double distance = Math.sqrt(Math.pow(r1 - r2, 2) + Math.pow(g1 - g2, 2) + Math.pow(b1 - b2, 2));

        // 计算相似度
        return 1 - distance / Math.sqrt(3 * Math.pow(255, 2));
    }

    /**
     * 根据十六进制颜色代码计算相似度
     *
     * @param hexColor1
     * @param hexColor2
     * @return 相似度（0到1之间，1为完全相同）
     */
    public static double calculateSimilarity(String hexColor1, String hexColor2) {
        Color color1 = Color.decode(hexColor1);
        Color color2 = Color.decode(hexColor2);
        return calculateSimilarity(color1, color2);
    }

    /**
     * 颜色矫正
     * @param color
     * @return
     */
    public static String colorCorrection(String color) {
        if (color.length()==7)
            color=color.substring(0,4)+"0"+color.substring(4,7);

        return color;
    }
//    // 示例代码
//    public static void main(String[] args) {
//        // 测试颜色
//        Color color1 = Color.decode("0xFF0000");
//        Color color2 = Color.decode("0xFE0101");
//        double similarity = calculateSimilarity(color1, color2);
//
//        System.out.println("颜色相似度: " + similarity);
//
//        double hexSimilarity = calculateSimilarity("0xFF0000", "0xFE0101");
//        System.out.println("Hex's color similarity: " + hexSimilarity);
//    }
}
