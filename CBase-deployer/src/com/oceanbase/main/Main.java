package com.oceanbase.main;


import com.oceanbase.odeployer.ODDeployer;
import com.oceanbase.odeployer.common.ODRegisterParameter;


public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
        ODRegisterParameter registerParameter = new ODRegisterParameter();
        ODDeployer deployer = ODDeployer.getInstance();
        ODDeployer.DEBUG = false;
        ODDeployer.PRINT_SHELL = false;
//        ODDeployer.CONNECT = false;
        deployer.init("demo 2.0", registerParameter);
        
//          deployer.start("cn"); //cn 生成模板配置文件
//          deployer.start("ap f"); //ap 
//          deployer.start("dy"); //dy 
//          deployer.start("st st2 ");
        deployer.start(args);
        deployer.destroy();
	}

}
