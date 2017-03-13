package com.oceanbase.odeployer.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 打印分隔线的工具类
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public class ODPrinter {
	
	// 表格中的横线类型
	public static String TABLE_CELL_EMPTY = "<TABLE_CELL_EMPTY>"; // 无线条
	public static String TABLE_CELL_LINE = "<TABLE_CELL_LINE>"; // 有线条

    /** 总宽度, 要求是奇数 */
    private static int WIDTH = 65;

    /**
     * 星横线 *****************
     */
    public static void printStarLine() {
        line("-");
    }

    /**
     * 点横线 .................
     */
    public static void printDotLine() {
        line(".");
    }

    /**
     * 双横线 =================
     */
    public static void printDoubleLine() {
        line("=");
    }

    /**
     * 加边线的双横线 *==================*
     * @param border 边界符号
     */
    public static void printDoubleLine(String border) {
        line("=", border, "=", false);
    }

    /**
     * 单横线 -----------------
     */
    public static void printSingleLine() {
        line("-");
    }

    /**
     * 加边线的单横线 *------------------*
     */
    public static void printSingleLine(String border) {
        line("-", border, "-", false);
    }
    
    /**
     * 加边线的单横线 *------------------*
     */
    public static void printSingleLine(String border, int width) {
        line("-", border, "-", false, width);
    }

    /**
     * 加文字的单横线 ---- message ----
     * @param message 文字
     */
    public static void printMessageLine(String message) {
        line("-", "", message, true);
    }

    /**
     * 换行
     */
    public static void printEmptyLine() {
        line("");
    }
    
    /**
     * 加边界的文字, 居中
     * @param message 文字
     * @param border 边界符号
     * @param width 总长度
     */
    public static void printMessageCenter(String message, String border, int width) {
        line(" ", border, message, true, width);
    }

    /**
     * 加边界的文字, 居中
     * @param message 文字
     * @param border 边界符号
     */
    public static void printMessageCenter(String message, String border) {
        line(" ", border, message, true);
    }

    /**
     * 加边界的文字, 左对齐
     * @param message 文字
     * @param border 边界符号
     */
    public static void printMessageLeft(String message, String border) {
        line(" ", border, message, false);
    }

    /**
     * 数字倒计时
     * @param total 总时间,单位：秒
     */
    public static void printClocker(int total) {
        for(int i = 0; i < total; i++) {
            int rest = total - i;
            System.out.print(rest);
            ODUtil.sleep(1000);
            ODUtil.clearConsole(String.valueOf(rest).length());
        }
    }
    
    /**
     * 打印表格，并显示行号
     * @param header
     * @param rows
     */
   //mod zhangyf 161103 b:
    //public static void printTableWithRowNumber(List<String> header, List<List<String>> rows) {
    public static void printTableWithRowNumber(String tableName, List<String> header, List<List<String>> rows) {	
    //e:mod
    	header.add(0, "#");
    	int rowNumber = 1;
    	for(List<String> row: rows) {
            if(row != null) {
            	rowNumber++;
            }
    	}
    	int rowNumberWidth = String.valueOf(rowNumber).length();
    	rowNumber = 1;
    	for(List<String> row: rows) {
            if(row != null) {
            	//将行号前面补0对齐
            	row.add(0,
            			ODUtil.charToString("0", rowNumberWidth - String.valueOf(rowNumber).length()) 
            			+ rowNumber);
            	rowNumber++;
            }
    	}
    //mod zhangyf 161103:b
    	//printTable(header, rows);
    	printTable(tableName ,header, rows);
    //e:mod
    }
    
    /**
     * 打印表格，不加表格名称
     * @param header
     * @param rows
     */
    public static void printTable(List<String> header, List<List<String>> rows) {
    	printTable(null, header, rows);
    }

    /**
     * 打印表格
     * @param tablename 表格名称
     * @param header 表头
     * @param rows 若某行为null, 则打印横线
     */
    public static void printTable(String tablename, List<String> header, List<List<String>> rows) {
        String table = "+";
        if(header == null || rows == null) { // 检查参数
            return;
        }
        int columnCount = header.size();
        List<Integer> widths = new ArrayList<>(); // 各列的宽度
        int totalWidth = 1; // 表格的总宽度
        for(int i = 0; i < columnCount; i++) {
            int width = getColumnWidth(header, rows, i);
            totalWidth += width + 1; //3表示两个空格和一个|
            widths.add(width);
        }
        // table title
        if(tablename != null) {
        	printSingleLine("+", totalWidth);
            printMessageCenter(tablename, "|", totalWidth);
        }
        int gap = WIDTH - totalWidth - 1;
        if(gap > 0) {
            // 增加第一列的宽度，使得表格与总宽度对齐
        	if(columnCount > 1) {
        		widths.set(1, widths.get(0) + gap);
        	} else {
        		widths.set(0, widths.get(0) + gap);
        	}
        }
        table += getTableLine(columnCount, widths);
        table += ODUtil.SEPARATOR + "|";
        for(int i = 0; i < columnCount; i++) {
            table += ODUtil.formatString(" " + header.get(i), widths.get(i)) + "|";
        }
        table += ODUtil.SEPARATOR + "+";
        table += getTableLine(columnCount, widths);
        table += ODUtil.SEPARATOR;
        for(List<String> row: rows) {
            if(row != null) {
            	if(!row.get(0).equals(TABLE_CELL_EMPTY) && !row.get(0).equals(TABLE_CELL_LINE)
            			&& row.size() == columnCount) {
            		table += "|";
                    for(int i = 0; i < columnCount; i++) {
                        String col = row.get(i);
                        if(col != null) {
                            col = col.replace("\n", "").trim(); // 去除换行和多余空格
                        }
                        table += ODUtil.formatString(" " + col, widths.get(i)) + "|";
                    }
            	} else {
            		table += "+";
                    table += getTableLine(columnCount, widths, row);
            	}
            } else { // 增加空行
                table += "+";
                table += getTableLine(columnCount, widths);
            }
            table += ODUtil.SEPARATOR;
        }
        table += "+";
        table += getTableLine(columnCount, widths);
        ODLogger.log(table);
    }


    // ----------------------------------------------------------- private

    private static String getTableLine(int columnCount, List<Integer> widths) {
    	return getTableLine(columnCount, widths, null);
    }
    /**
     * 表格中的横线
     * @return 线
     */
	private static String getTableLine(int columnCount, List<Integer> widths, List<String> lineRow) {
        String str = "";
        for(int i = 0; i < columnCount; i++) {
        	if(lineRow != null) {
        		if(i < lineRow.size() && lineRow.get(i).equals(TABLE_CELL_EMPTY)) {
            		str += ODUtil.charToString(" ", widths.get(i)) + "+";
            	} else {
            		str += ODUtil.charToString("-", widths.get(i)) + "+";
            	}
        	} else {
        		str += ODUtil.charToString("-", widths.get(i)) + "+";
        	}
        }
        return str;
    }

    /**
     * 打印线
     * @param mark 线的形式
     */
    private static void line(String mark) {
        ODLogger.log(getLine(mark));
    }

    /**
     * 获取长度为WIDTH的直线
     * @param mark 线单元
     * @return 线
     */
    private static String getLine(String mark) {
        return ODUtil.charToString(mark, WIDTH);
    }
    
    private static void line(String mark, String border, String msg, 
    		boolean alignCenter) {
    	line(mark, border, msg, alignCenter, WIDTH);
    }

    /**
     * 打印线条
     * @param mark 线单元
     * @param border 边界符号
     * @param msg 文字
     * @param alignCenter 文字是否居中，否则左对齐
     * @param width 总宽度
     */
    private static void line(String mark, String border, String msg, 
    		boolean alignCenter, int width) {
        String left;
        String right;
        int leftLen;
        int rightLen;
        if(alignCenter) {
            leftLen = (width - msg.length() - border.length() * 2) / 2;
            rightLen = width - msg.length() - border.length() * 2 - leftLen;
        } else {
            leftLen = 1;
            rightLen = width - msg.length() - leftLen - border.length() * 2;
        }
        left = ODUtil.charToString(mark, leftLen);
        right = ODUtil.charToString(mark, rightLen);
        ODLogger.log(border + left + msg + right + border);
    }

    /**
     * 计算列的宽度, 用于对齐
     * @param header 表头
     * @param rows 行
     * @param columnIndex 列序号
     * @return 第columnIndex列的宽度
     */
    private static int getColumnWidth(List<String> header, List<List<String>> rows, int columnIndex) {
        int width = header.get(columnIndex).length() + 2;
        for(List<String> row: rows) {
            if(row != null && !row.get(0).equals(TABLE_CELL_EMPTY) && !row.get(0).equals(TABLE_CELL_LINE)) {
                int w = row.get(columnIndex).length() + 2;
                if(w > width) {
                    width = w;
                }
            }
        }
        return width;
    }
}
