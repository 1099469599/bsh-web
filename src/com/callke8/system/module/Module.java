package com.callke8.system.module;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Model;
import com.jfinal.plugin.activerecord.Record;

/**
 * 表数据
 * @author Administrator
 *
 *CREATE TABLE `sys_module` (
  `MODULE_CODE` varchar(255) NOT NULL,
  `MODULE_NAME` varchar(64) NOT NULL,
  `MODULE_TYPE` varchar(2) DEFAULT NULL,
  `PARENT_CODE` varchar(32) DEFAULT NULL,
  `MODULE_DESC` varchar(128) DEFAULT NULL,
  `MODULE_URI` varchar(128) DEFAULT NULL,
  `MODULE_VIEW` tinyint(4) DEFAULT NULL,
  `MODULE_ODR` varchar(32) DEFAULT NULL,
  `ICO_PATH` varchar(256) DEFAULT NULL,
  `second_show` varchar(2) DEFAULT NULL,
  `usual_show` varchar(2) DEFAULT NULL,
  `ispad` varchar(10) DEFAULT NULL,
  `imgname` varchar(60) DEFAULT NULL,
  PRIMARY KEY (`MODULE_CODE`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
 *
 */
@SuppressWarnings("serial")
public class Module extends Model<Module> {
	
	public static Module dao = new Module();
	
	/**
	 * 查询所有的菜单列表
	 * @return
	 */
	public List<Record> getAllModule() {
		
		String sql = "select * from sys_module order by MODULE_ORDER asc";
		
		List<Record> list = Db.find(sql);
		
		return list;
	}
	
	public List<Record> getModuleByParentCode(String moduleCode) {
		
		String sql = "select * from sys_module where PARENT_CODE=?";
		
		List<Record> list = Db.find(sql, moduleCode);
		
		return list;
	}
	
	public boolean deleteByModuleCode(String moduleCode) {
		boolean b = false;
		
		String sql = "delete from sys_module where MODULE_CODE=?";
		
		int count = Db.update(sql, moduleCode);
		
		if(count>0) {
			b = true;
		}
		
		return b;
	}
	
	/**
	 * 根据菜单编码取出菜单对象
	 * 
	 * @param moduleCode
	 * @return
	 */
	public Record getModuleByModuleCode(String moduleCode) {
		
		String sql = "select * from sys_module where MODULE_CODE=?";
		
		Record  module = Db.findFirst(sql, moduleCode);
		
		return module;
	}
	
	public boolean update(Module module) {
		
		boolean b = false;
		int count = 0;
		
		//得到组织代码
		String moduleCode = module.get("MODULE_CODE");
		
		count = Db.update("update sys_module set MODULE_NAME=?,MODULE_DESC=?,MODULE_URI=? where MODULE_CODE=?", module.get("MODULE_NAME"),module.get("MODULE_DESC"),module.get("MODULE_URI"),moduleCode);
		
		if(count == 1) {
			b = true;
		}
		
		return b;
		
	}
	
	public boolean add(Module module) {
		boolean b = false;
		
		if(module.save()) {
			b = true;
		}
		return b;
	}
	
	/**
	 * 用于加载所有的菜单数据到内存中
	 * 
	 * @return
	 */
	public Map loadModuleInfo() {
		Map<String,Record> m = new HashMap<String,Record>();
		
		//先将所有的菜单数据取出
		List<Record> list = getAllModule();
		
		for(Record r:list) {
			m.put(r.get("MODULE_CODE").toString(), r);
		}
		
		return m;
	}
	
}
