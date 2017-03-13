package com.oceanbase.odeployer.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.alipay.oceanbase.OBGroupDataSource;
import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.util.ODLogger;

/**
 * Oceanbase连接器
 * <p>用于连接数据库</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 */
public final class ODOceanbaseConnector {
    /**
     * 使用OBGroupDataSource的连接方式
     * @param ip MS ip
     * @param port MS 端口号
     * @param user 数据库用户，一般为admin
     * @param password 用户密码，一般为admin
     * @return 数据连接，可能为null
     */
    public static Connection getOBConnection(String ip, String port, String user, String password) {
        Map<String, String> configParams = new HashMap<>();
        configParams.put("username", user);
        configParams.put("password", password);
        configParams.put("clusterAddress", ip + ":" + port);
        OBGroupDataSource obGroupDataSource = new OBGroupDataSource();
        obGroupDataSource.setDataSourceConfig(configParams);
        try {
            obGroupDataSource.init();
            return obGroupDataSource.getConnection();
        } catch (SQLException e) {
            ODLogger.log("[ERROR] Connect oceanbase(" + ip + ":" + port + ") error! Make sure the oceanbase is running.");
            if(ODDeployer.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    /**
     * 使用JDBC连接方式
     * @param ip MS ip
     * @param port MS 端口号
     * @param user 数据库用户，一般为admin
     * @param password 用户密码，一般为admin
     * @return 数据连接，可能为null
     */
    public static Connection getJDBCConnection(String ip, String port, String user, String password) {
        String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://" + ip + ":" + port;
        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, user, password);
        } catch (Exception e) {
            String[] exc = e.toString().split(":");
            String exception = "";
            if(exc.length == 2) {
                exception = exc[1].trim();
            }
            ODLogger.log("[ERROR] Connect oceanbase(" + ip + ":" + port + ") error! Exception: " + exception);
            if(ODDeployer.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
