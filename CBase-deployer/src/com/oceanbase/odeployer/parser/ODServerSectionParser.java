package com.oceanbase.odeployer.parser;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.command.ODCommand;
import com.oceanbase.odeployer.common.ODConfiguration;
import com.oceanbase.odeployer.common.ODError;
import com.oceanbase.odeployer.common.ODItem;
import com.oceanbase.odeployer.common.ODItemAdder;
import com.oceanbase.odeployer.common.ODServer;
import com.oceanbase.odeployer.util.ODLogger;
import com.oceanbase.odeployer.util.ODUtil;

/**
 * "server.*"类型的配置项
 * <p>自定义解析方法，不继承通用的解析器</p>
 * @author lbz@lbzhong.com 2016/03/30
 * @since OD1.0
 * @author yanfeizhang68@gmail.com 2016/12/30
 * @since OD2.0
 */
public class ODServerSectionParser extends ODItemAdder implements ODISectionParser {
    
    private Set<ODItem> commonItemSet = new HashSet<>();

    private ServerSection serverSection = new ServerSection();
    
    public ODServerSectionParser() {
        commonItemSet.add(ODItem.SERVER_RS_PORT);
        commonItemSet.add(ODItem.SERVER_UPS_PORT);
        commonItemSet.add(ODItem.SERVER_MS_PORT);
        commonItemSet.add(ODItem.SERVER_CS_PORT);
        commonItemSet.add(ODItem.SERVER_VERSION);
        commonItemSet.add(ODItem.SERVER_COMMON_USER);
        commonItemSet.add(ODItem.SERVER_COMMON_NETWORK);
    }

    @Override
    public String getSectionName() {
        return "server";
    }
    
    @Override
    public String getDescription() {
        return "The information of remote nodes." + ODUtil.SEPARATOR +
                "# The node information must begin with 'server.ip', and end with 'server.close'";
    }

    @Override
    public void parseConfigureItems(ODCommand cmd, ODItem item, String value)
            throws Exception {
    // ----------------------------------------------------------- 1. server.common
        if(commonItemSet.contains(item)) { // common
            if(!isExist(item)) { // common类型的配置项不能重复
                if(item == ODItem.SERVER_RS_PORT) {
                    serverSection.rsPort = value;
    //------------------------------------------------upsport/msport/rsport-----------------------                 
                }else if(item == ODItem.SERVER_UPS_PORT){
                    String[] values = ODUtil.split(value, ",");
                    if(values != null && values.length == 2) {
                        if(serverSection.upsPortp == null) {
                            serverSection.upsPortp = values[0];
                            serverSection.upsPortm = values[1];
                        } else {
                            ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
                        }
                    } else {
                        ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, item);
                    }
                }else if(item == ODItem.SERVER_MS_PORT){
                    String[] values = ODUtil.split(value, ",");
                    if(values != null && values.length == 2) {
                        if(serverSection.msPortp == null) {
                            serverSection.msPortp = values[0];
                            serverSection.msPortz = values[1];
                        } else {
                            ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
                        }
                    } else {
                        ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, item);
                    }
                }else if(item == ODItem.SERVER_CS_PORT){
                    String[] values = ODUtil.split(value, ",");
                    if(values != null && values.length == 2) {
                        if(serverSection.csPortp == null) {
                            serverSection.csPortp = values[0];
                            serverSection.csPortn = values[1];
                        } else {                         
                            ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
                        }
                    } else {
                        ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, item);
                    }
    //------------------------------------------end_port-------------------------------------------                
                }else if(item == ODItem.SERVER_VERSION) {
                    if(serverSection.version == null) {
                    	serverSection.version = value;
                        if(!ODDeployer.getInstance().isParameterGeneratorRegistered(value)) {
                            ODConfiguration.printError(ODError.ERROR_EXCEPTION, item, 
                                    "oceanbase.version(" + value + ") is undefined");
                        }
                    } else {
                        ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
                    }
                } else if(item == ODItem.SERVER_COMMON_USER) {
                    String[] values = ODUtil.split(value, ",");
                    if(values != null && values.length == 2) {
                        if(serverSection.commonUsername == null) {
                            serverSection.commonUsername = values[0];
                            serverSection.commonPassword = values[1];
                        } else {
                            ODConfiguration.printError(ODError.ERROR_CONFLICT, item);
                        }
                    } else {
                        ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, item);
                    }
                } else if(item == ODItem.SERVER_COMMON_NETWORK) {
                    serverSection.commonNetwork = value;
                }
            }
        } else { // server节点列表
            if(item == ODItem.SERVER_IP || item == ODItem.SERVER_CLOSE) {
                //保存上一个Server
                if(serverSection.server != null) {
                    serverSection.server.checkData(serverSection.server.ip);
                    Class<? extends ODServer> customServerClass = ODDeployer.getInstance().getCustomServerClass();
                    ODServer server = null;
                    if(customServerClass == null) {
                        server = new ODServer(serverSection.server.ip, serverSection.server.username,
                                serverSection.server.password, serverSection.server.network);
                    } else { // 使用扩展的Server类
                        try {
                            Constructor<?> constructor = customServerClass.getConstructor(String.class, String.class,
                                    String.class, String.class);
                            server = (ODServer) constructor.newInstance(serverSection.server.ip, serverSection.server.username,
                                    serverSection.server.password, serverSection.server.network);
                        } catch (Exception e) {
                            ODLogger.error("instantiate '" + customServerClass + "' fail!", (new Throwable()).getStackTrace());
                        }
                    }
                    if(server != null) {
                        serverSection.serverList.add(server);
                    }
                    //serverSection.server = null;
                }
            }
        // ----------------------------------------------------------- 2. server.ip
            if(item == ODItem.SERVER_IP) {
                boolean isExist = false; // 检查是否重复配置节点
                for(ODServer s: serverSection.serverList) {
                    if(s.ip.equals(value)) {
                        isExist = true;
                        break;
                    }
                }
                if(!isExist) {
                    serverSection.server = new Server();
                    // 判断是否设置了server.common.user和server.common.network
                    // 若已设置，则不需要单个节点配置user和network
                    if(serverSection.commonUsername != null) {
                        serverSection.server.setCommonUser(serverSection.commonUsername, serverSection.commonPassword);
                    }
                    if(serverSection.commonNetwork != null) {
                        serverSection.server.setCommonNetwork(serverSection.commonNetwork);
                    }
                    serverSection.server.setIp(value);
                } else {
                    ODConfiguration.printError(ODError.ERROR_EXCEPTION, "[" + value + "] is existed");
                }
        // ----------------------------------------------------------- 3. server.user
            } else {
                if(serverSection.server != null) {
                    if(item == ODItem.SERVER_USER) {
                        String[] values = ODUtil.split(value, ",");
                        if(values != null && values.length == 2) {
                            serverSection.server.setUser(values[0], values[1]);
                        } else {
                            ODConfiguration.printError(ODError.ERROR_WRONG_VALUE, item);
                        }
        // ----------------------------------------------------------- 4. server.network
                    } else if(item == ODItem.SERVER_NETWORK) {
                        serverSection.server.setNetwork(value);
                    }
                } else {
                    ODConfiguration.printError(ODError.ERROR_MISS_ITEM, ODItem.SERVER_IP, item.toString());
                }
            }
        }
    }

    @Override
    public void before() {
        // do nothing
    }

    @Override
    public void after() {
        addConfigureItem(ODItem.SERVER_RS_PORT, serverSection.rsPort);
        //add upsport/msport/csport
        if(serverSection.upsPortp != null){
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_UPS_PORT_P, serverSection.upsPortp);
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_UPS_PORT_M, serverSection.upsPortm);
        }
        if(serverSection.msPortp != null){
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_MS_PORT_P, serverSection.msPortp);
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_MS_PORT_Z, serverSection.msPortz);
        }
        if(serverSection.csPortp != null){
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_CS_PORT_P, serverSection.csPortp);
        	ODConfiguration.addConfigureItem(ODItem.H_SERVER_CS_PORT_N, serverSection.csPortn);
        }
//        addConfigureItem(ODItem.SERVER_UPS_PORT,serverSection.upsPortp);
//        addConfigureItem(ODItem.SERVER_UPS_PORT,serverSection.upsPortm);
//        addConfigureItem(ODItem.SERVER_MS_PORT,serverSection.msPortp);
//        addConfigureItem(ODItem.SERVER_MS_PORT,serverSection.msPortz);
//        addConfigureItem(ODItem.SERVER_CS_PORT,serverSection.csPortp);
//        addConfigureItem(ODItem.SERVER_CS_PORT,serverSection.csPortn);
        //end
        
        addConfigureItem(ODItem.SERVER_VERSION, serverSection.version);
        // server.common.user, server.common.network 为可选配置项
        if(serverSection.commonPassword != null) {
            ODConfiguration.addConfigureItem(ODItem.H_SERVER_COMMON_USERNAME, serverSection.commonUsername);
            ODConfiguration.addConfigureItem(ODItem.H_SERVER_COMMON_PASSWORD, serverSection.commonPassword);
        }
        if(serverSection.commonNetwork != null) {
            addConfigureItem(ODItem.SERVER_COMMON_NETWORK, serverSection.commonNetwork);
        }
        // 添加服务器节点列表
        if(serverSection.serverList.size() == 0) {
            ODConfiguration.printError(ODError.ERROR_MISS_ITEM, ODItem.SERVER_IP, false);
        } else {
            ODConfiguration.addConfigureItem(ODItem.H_SERVERS, serverSection.serverList);
        }
    }

    /**
     * 获取主机列表，不一定完整
     * @return List
     */
    public List<ODServer> getServerList() {
        return serverSection.serverList;
    }

    // ----------------------------------------------------------- private

    /**
     * 用于识别配置项中的主机
     */
    private class Server {
        private String ip = null;
        private String username = null;
        private String password = null;
        private String network = null;
        private boolean isCommonUser = false;
        private boolean isCommonNetwork = false;

        private void setIp(String ip) {
            if(this.ip == null) {
                this.ip = ip;
            }
        }
    
        private void setUser(String username, String password) {
            if(this.username == null || isCommonUser) {
                this.username = username;
                this.password = password;
                isCommonUser = false;
            } else {
                ODConfiguration.printError(ODError.ERROR_CONFLICT, ODItem.SERVER_USER);
           }
        }
    
        private void setNetwork(String network) {
            if(this.network == null || isCommonNetwork) {
                this.network = network;
                isCommonNetwork = false;
            } else {
                ODConfiguration.printError(ODError.ERROR_CONFLICT, ODItem.SERVER_NETWORK);
            }
        }
    
        private void setCommonUser(String username, String password) {
            if(this.username == null) {
                this.username = username;
                this.password = password;
                this.isCommonUser = true;
            }
        }
    
        private void setCommonNetwork(String network) {
            if(this.network == null) {
                this.network = network;
                this.isCommonNetwork = true;
            }
        }
    
        /** 检查是否设置了ip和username */
        private void checkData(String ip) {
            if(username == null) {
                ODConfiguration.printError(ODError.ERROR_MISS_ITEM, ODItem.SERVER_USER, ip);
            }
            if(network == null) {
                ODConfiguration.printError(ODError.ERROR_MISS_ITEM, ODItem.SERVER_NETWORK, ip);
            }
        }
    }

    /**
     * Server部分的配置属性
     */
    private class ServerSection {
    
        /** 用于解析单个主机节点的临时变量 */
        private Server server = null;
    
        /** 所有主机节点信息列表 */
        private List<ODServer> serverList = new ArrayList<>();
    
        private String rsPort = null;
        
        //add upsport/msport/csport
        private String upsPortp = null;
        
        private String upsPortm = null;
        
        private String msPortp = null;
        
        private String msPortz = null;
        
        private String csPortp = null;
        
        private String csPortn = null;
        //end 
    
        private String version = null;
        
        private String commonUsername = null;
    
        private String commonPassword = null;
    
        private String commonNetwork = null;
    
    }

    @Override
    public void init() {
        // do nothing
    }

}
