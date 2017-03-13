package com.oceanbase.odeployer.util;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.common.ODServerName;
import com.oceanbase.odeployer.parser.ODISectionParser;
import com.oceanbase.odeployer.parser.ODOceanbaseSectionParser;

/**
 * 公共的工具类
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODUtil {

    /** 换行符 */
    public static String SEPARATOR = System.getProperty("line.separator");

    /**
     * 切分字符串为List, 以逗号分隔
     * @param value
     * @return
     */
    public static List<String> splitToList(String value) {
        List<String> list = new ArrayList<>();
        String[] values = value.split(",");
        for(String s: values) {
            s = s.trim();
            if(s.length() > 0) {
                list.add(s);
            }
        }
        return list;
    }

    /**
     * 打印shell命令
     * @param shell
     */
    public static void printShell(String shell) {
        System.out.println("============================= Shell =============================");
        System.out.println(shell.replace(";", "\n"));
    }
    
    /**
     * 解析异常信息
     * @param e
     * @return
     */
    public static String parseException(Exception e) {
        if(ODDeployer.DEBUG) {
            e.printStackTrace();
        }
        return e.toString();
    }
    
    /**
     * 切分字符串，去除空格
     * @param str
     * @param regex
     * @return
     */
    public static String[] split(String str, String regex) {
        String[] s = str.split(regex);
        for(int i = 0; i < s.length; i++) {
            s[i] = s[i].trim();
        }
        return s;
    }
    
    /**
     * 转换时间单位
     * @param timems ms
     * @return
     */
    public static String parseTime(long timems) {
        long rates[]   = {3600000, 60000, 1000, 1};
        String units[] = {"h",     "min", "s",  "ms"};
        int i = 0;
        for(String unit: units) {
            long rate = rates[i];
            if(timems >= rate) {
                return trimFloat(((float)timems)/rate) + " " + unit;
            }
            i++;
        }
        return "0 ms";
    }
    
    /**
     * 截取两位小数
     * @param f
     * @return
     */
    public static String trimFloat(float f) {
        String s = String.valueOf(f);
        String ss[] = s.split("\\.");
        if(ss.length == 2) {
            String left = ss[0];
            String right = ss[1];
            if(right.length() > 2) {
                right = right.substring(0, 2);
            }
            return left + "." + right;
        }
        return s;
    }
    
    /**
     * 当前时间
     * 格式：yyyy-MM-dd HH:mm
     * @return
     */
    public static String getSystemTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new Date());
    }
    
    /**
     * 拼接重复的字符串
     * @param str
     * @param len
     * @return
     */
    public static String charToString(String str, int len) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < len; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 消除命令控制台最后一行的最后len个字符
     * @param len
     */
    public static void clearConsole(int len) {
        System.out.print(charToString("\b", len));
        System.out.print(charToString(" ", len));
        System.out.print(charToString("\b", len));
    }
    
    /**
     * 输出字符串数组
     * @param strs
     * @return
     */
    public static String toString(String[] strs) {
        if(strs != null) {
            StringBuilder sb = new StringBuilder();
            for(String s: strs) {
                    sb.append(s + " ");
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * 写文件，若文件已存在，将覆盖原有内容
     * @param filename
     * @param text
     * @throws Exception
     */
    public static void writeFile(String filename, String text) throws Exception {
        String dir = filename.substring(0, filename.lastIndexOf("/"));
        if(dir != null) {
            File fileDir = new File(dir);
            fileDir.mkdirs();
        }
        File file = new File(filename);
        FileWriter fw = null;
        try {
            file.createNewFile();
            fw = new FileWriter(file, false);
            fw.write(text);
            fw.close();
        } catch (Exception e) {
            throw e;
        } finally {
            if(fw != null) {
                fw.close();
            }
        }
    }

    /**
     * 格式化字符串，右边补空格
     * @param str
     * @param len
     * @return 若str长度小于len, 则补充空格, 否则不作处理
     */
    public static String formatString(String str, int len) {
        int gap = len - str.length();
        if(gap > 0) {
            return str + charToString(" ", gap);
        }
        return str;
    }

    /**
     * 生成配置文件模板
     * @param version
     * @param sectionParserList
     * @return
     */
    public static String buildConfigurationTemplate(String version, List<ODISectionParser> sectionParserList,
    		List<String> oceanbaseVersions) {
        StringBuilder sb = new StringBuilder();
        int width = 70;
        sb.append("# " + charToString("=", width) + SEPARATOR +
                "# Oceanbase Deployer V-" + version + " <Configuration file>" + SEPARATOR +
                "# @date:   " + getSystemTime() + SEPARATOR +
                "# @notice: This configuration template file is generated automatically." + SEPARATOR +
                "#          The whole line or several words can be commented out by using" + SEPARATOR +
                "#          the character of \"#\"." + SEPARATOR +
                "# " + charToString("=", width) + SEPARATOR);
        for(ODISectionParser parser: sectionParserList) {
            String name = parser.getSectionName();//不同的sectionParser调用不同的getSectionName方法
            sb.append(SEPARATOR);
            sb.append(SEPARATOR);
            sb.append("# ").append(charToString("-", width)).append(SEPARATOR)
                    .append("# Section: ").append(name).append(SEPARATOR)
                    .append("# ").append(charToString("-", width)).append(SEPARATOR)
                    .append("# ").append(parser.getDescription()).append(SEPARATOR);
            //----------------------- 1. 赋值格式和说明
            List<Pair<String, Pair<String, String>>> vdList = ODItem.getValuePatternAndDescriptionList(name);
            //通过sectionname获取到相关的item、pattern以及description
            int maxLen = 0;
            for(Pair<String, Pair<String, String>> vd: vdList) {
                int len = vd.first.length();
                if(len > maxLen && len < 25) {
                    maxLen = len; // 用于对齐
                }
            }
            for(Pair<String, Pair<String, String>> vd: vdList) {
                if(isCloseItem(vd.first)) {
                    sb.append("# ").append(vd.first).append(SEPARATOR);
                } else {
                    sb.append("# ").append(formatString(vd.first, maxLen)).append(" = ").append(vd.second.first);
                    String decription = vd.second.second;
                    if(decription != null && decription.length() > 0 && !decription.equals("null")) {
                    	int patterLen = 0; 
                    	if(vd.second.first != null) {
                    		patterLen = vd.second.first.length();
                    	}
                        sb.append(", ").append(formatDescription(decription, patterLen, maxLen));
                    }
                    sb.append(SEPARATOR);
                }
            }
            sb.append("# ").append(charToString("-", width)).append(SEPARATOR);
            //----------------------- 2. 默认值
            List<Pair<String, String>> dvList = ODItem.getDefaultValueList(name);
            if(parser instanceof ODOceanbaseSectionParser && oceanbaseVersions.size() > 0) { // 对oceanbase.*类型配置项特殊处理
            	for(int i = 0; i < oceanbaseVersions.size(); i++) {
            		String ov = oceanbaseVersions.get(i);
            		if(i != 0) {
            			sb.append("# ").append(charToString("-", width)).append(SEPARATOR);
            		}
            		for(Pair<String, String> dv: dvList) {
            			if(isNameItem(dv.first)) {
            				sb.append(formatString(dv.first, maxLen)).append(" = ").append(ov).append(SEPARATOR);
            			} else if(isCloseItem(dv.first)) {
                            sb.append(dv.first).append(SEPARATOR);
                        } else {
                            sb.append(formatString(dv.first, maxLen)).append(" = ").append(dv.second).append(SEPARATOR);
                        }
                    }
            	}
            } else {
                for(Pair<String, String> dv: dvList) {
                    if(isCloseItem(dv.first)) {
                        sb.append(dv.first).append(SEPARATOR);
                    } else {
                        sb.append(formatString(dv.first, maxLen)).append(" = ").append(dv.second).append(SEPARATOR);
                    }
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * 是否是*.name类型的配置项
     * @param item
     * @return
     */
    public static boolean isNameItem(String item) {
    	if(item != null && (item.length() - item.replace(".", "").length() == 1)
    			&& item.endsWith(".name") && !item.startsWith(".")) {
    		return true;
    	}
    	return false;
    }
    
    /**
     * 是否是*.close类型的配置项
     * @param item
     * @return
     */
    public static boolean isCloseItem(String item) {
    	if(item != null && (item.length() - item.replace(".", "").length() == 1)
    			&& item.endsWith(".close") && !item.startsWith(".")) {
    		return true;
    	}
    	return false;
    }

    /**
     * 去除尾部的字符",", "#"或"|"
     * @param str
     * @return
     */
    public static String removeLastChar(String str) {
        if(str != null) {
            if(str.endsWith(",") || str.endsWith("|") || 
                    str.endsWith("#") || str.endsWith("/")) {
                return str.substring(0, str.length() - 1);
            }
        }
        return str;
    }

    /**
     * List是否存在
     * @param list
     * @param str 忽略大小写
     * @return
     */
    public static boolean isExist(List<String> list, String str) {
        for(String s: list) {
          if(s.equalsIgnoreCase(str)) {
              return true;
          }
        }
        return false;
    };

    /**
     * 将字符串List转换成OBServerName类型的List
     * @return
     */
    public static List<ODServerName> stringToServerNameList(List<String> tmp) {
        if(tmp != null) {
            List<ODServerName> servernames = new ArrayList<>();
            for(String sn: tmp) {
                servernames.add(ODServerName.toServerType(sn));
            }
            return servernames;
        }
        return null;
    }

    /**
     * 等待
     * @param ms 毫秒
     */
    public static void sleep(int ms) {
        if(ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException ignored) {
            	//do nothing
            }
        }
    }
    
	public static void sleep(double d) {
		// TODO Auto-generated method stub
	    if(d >= 0) {
	        try {
	                Thread.sleep((long) d);
	        } catch (InterruptedException ignored) {
	            	//do nothing
	        }
	    }else{
	   ODLogger.error("the time was error! please input the time more than 0s", new Throwable().getStackTrace());
	    	d = 0.5;
	    	try {
                Thread.sleep((long) d);
            } catch (InterruptedException ignored) {
            	//do nothing
            }
	    }
	}
	
	/**
	 * 检查string是否为null 或者为"",如果是，则返回true
	 * @param str
	 * @return   最好还是一个一个字符检查比较保险
	 */
	public static boolean stringEmpty(String str)
	{
		if(str != null) str = str.trim();
		if(str == null || "".equals(str) )
			return true;
		return false;
		
	}
    
    //分割以空格（空格数不定）为分界的字符串，返回数组
  	public static String[] getArgumentList(String argument)
  	{
  		List<String> arrayString = new ArrayList<String>();
  		String newStr = argument.replace(' ','#');
  		newStr += '#';//"###12##2###3#"
  		String s = "";
  		for(int i = 0;i < newStr.length();i++)//将参数行进行转化，转化为字符串数组存放命令名、参数等
  		{
  			if(newStr.charAt(i) != '#')
  				s += newStr.charAt(i);//charAt用来索引字符串的字符
  			else if(!s.trim().equals(""))//trim用来返回一个去除掉首尾空格的字符串副本
  			{
  				arrayString.add(s);
  				s = "";
  			}
  		}
  		return arrayString.toArray(new String[arrayString.size()]);//将字符串变为字符数组
  	} 
    
    // ----------------------------------------------------------- private
    
    private static String formatDescription(String description, int patterLen, int blankWidth) {
    	int w = 40;
    	if(description != null && description.length() + patterLen > w) {
			return SEPARATOR + "#" + charToString(" ", blankWidth + 4) + description;
    	}
    	return description;
    }


}
