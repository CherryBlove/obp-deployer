package com.oceanbase.odeployer.common;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODPrinter;
import com.oceanbase.odeployer.util.ODShell;
import com.oceanbase.odeployer.util.ODUtil;
import com.oceanbase.odeployer.util.Pair;

/**
 * 执行SQL操作
 * <p>增加超时机制, 自适应输出表数据.</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODQuery {

    private Connection conn;

    private Statement stmt;

    /** 新线程，用于计算超时 */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public ODQuery(String ip) {
        this(ip, ODShell.getMsZPort(), "admin", "admin");
    }

    public ODQuery(String ip, String msPort, String user, String password) {
        conn = ODOceanbaseConnector.getJDBCConnection(ip, msPort, user, password);
        if(conn != null) {
            try {
                stmt = conn.createStatement();
            } catch (Exception e) {
                if(ODDeployer.DEBUG) {
                    e.printStackTrace();
                }
                ODLogger.log("[ERROR] createStatement [" + ip + ":" + msPort +"] fail!");
            }
        }
    }

    /**
     * 执行非查询操作
     * @param sql SQL
     * @param timeout 超时时间，单位：秒
     * @return 是否执行成功
     */
    public ODError executeUpdate(final String sql, int timeout) {
        ODError ret = ODError.SUCCESS;
        if(stmt != null) {
            Callable<Integer> call = new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return stmt.executeUpdate(sql); // 执行SQL
                }
            };
            Future<Integer> future = executor.submit(call);
            try {
                int sqlRet = future.get(timeout * 1000, TimeUnit.MILLISECONDS);
                if(sqlRet <= 0 && !sql.startsWith("create") && !sql.startsWith("drop")) {
                    ODLogger.log("[ERROR] Execute sql(" + sql + ") fail!");
                    ret = ODError.ERROR;
                }
            } catch (TimeoutException te) {
                ODLogger.log("[WARN] Execute sql(" + sql + ") timeout!");
                ret = ODError.ERROR;
            } catch (Exception e) {
                ODLogger.log(ODUtil.parseException(e));
                ret = ODError.ERROR;
            }
        } else {
            ret = ODError.ERROR;
        }
        return ret;
    }
    
    /**
     * 查询操作, 并且打印表格
     * @param tablename 显示的表格名称
     * @param sql SQL
     * @param timeout 超时时间，单位：秒
     * @return 打印表格
     */
    public ODError executeQuery(String tablename, final String sql, int timeout) {
        return executeQuery(tablename, sql, timeout, true).first;
    }
    
    public ODError executeQuery(final String sql, int timeout) {
        return executeQuery(null, sql, timeout, true).first;
    }
    
    public Pair<ODError, List<List<String>>> executeQuery(final String sql, 
    		int timeout, boolean isPrint) {
    	return executeQuery(null, sql, timeout, isPrint);
    }

    /**
     * 查询操作
     * @param sql SQL
     * @param timeout 超时时间，单位：秒
     * @param isPrint 是否打印表格
     * @return List<List<String>> 行值
     */
    public Pair<ODError, List<List<String>>> executeQuery(String tablename, final String sql, 
    		int timeout, boolean isPrint) {
        ODError ret = ODError.SUCCESS;
        List<List<String>> rows = new ArrayList<>();
        if(stmt != null) {
            Callable<ResultSet> call = new Callable<ResultSet>() {
                @Override
                public ResultSet call() throws Exception {
                    return stmt.executeQuery(sql); // 执行SQL
                }
            };
            Future<ResultSet> future = executor.submit(call);
            try {
                ResultSet rs = future.get(timeout * 1000, TimeUnit.MILLISECONDS);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                // 表头
                List<String> header = new ArrayList<>();
                if(isPrint) {
                    for(int i = 1; i <= columnCount; i++) {
                        header.add(metaData.getColumnLabel(i));
                    }
                }
                // 行
                while (rs.next()) {
                    List<String> row = new ArrayList<>();
                    for(int i = 1; i <= columnCount; i++) {
                        // 如果某一列值为空，则填入""
                        if(rs.getObject(i) != null) {
                            row.add(rs.getObject(i).toString());
                        } else {
                            row.add("");
                        }
                    }
                    rows.add(row);
                }
                rs.close();
                if(isPrint) {
                	if(tablename != null 
                			&& tablename.equalsIgnoreCase("__all_server") 
                			&& sql.toLowerCase().contains("__all_server")) {
                		// 转换__all_server表
                		Pair<List<String>, List<List<String>>> table = tranformAllServerTable(header, rows);
                		ODPrinter.printTableWithRowNumber(tablename,table.first, table.second);
                	} else {
                		ODPrinter.printTable(tablename, header, rows);
                	}
                }
            } catch (TimeoutException te) {
                ODLogger.log("[WARN] Query sql(" + sql + ") timeout!");
                ret = ODError.ERROR;
            } catch (Exception e) {
                ret = ODError.ERROR;
                ODLogger.log(ODUtil.parseException(e));
            }
        } else {
            ret = ODError.ERROR;
        }
        return new Pair<ODError, List<List<String>>>(ret, rows);
    }
    
    private Pair<List<String>, List<List<String>>> tranformAllServerTable(List<String> header, 
    		List<List<String>> rows) {
    	//| cluster_id | svr_type     | svr_ip        | svr_port | inner_port | svr_role |
		List<List<String>> newRows = new ArrayList<>();
		List<String> clusterIds = new ArrayList<>();
		for(List<String> r: rows) {
			if(!clusterIds.contains(r.get(0))) {
				clusterIds.add(r.get(0));
			}
		}
		String[] servers = {"rootserver", "updateserver", "mergeserver", "chunkserver"};
		for(int i = 0; i < clusterIds.size(); i++) {
			boolean removeClusterId = false;
			String clusterId = clusterIds.get(i);
			if(i > 0) {
				newRows.add(null);
			}
			for(String s: servers) {
				boolean removeServerName = false;
				for(List<String> r: rows) {
					if(clusterId.equals(r.get(0)) && s.equals(r.get(1))) {
						if(!removeClusterId) {
							removeClusterId = true;
						} else {
							r.set(0, "");
						}
						if(!removeServerName) {
							removeServerName = true;
						} else {
						}
						newRows.add(r);
					}
				}
			}
		}
		return new Pair<List<String>, List<List<String>>>(header, newRows);
    }

    /** 断开连接 */
    public void close() {
        try {
            if(null != stmt) {
                stmt.close();
            }
            if(null != conn) {
                conn.close();
            }
            executor.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
