package com.kato.pro.base.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author zlt
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_menu")
public class SysMenu extends SuperModel<SysMenu> {
	private static final long serialVersionUID = 749360940290141180L;
	private Long parentId;
	private String name;
	private String code;
	private String css;
	private String url;
	private String path;
	private Integer sort;
	private Integer type;
	private Boolean hidden;
	private String pathMethod;
	@TableField(exist = false)
	private List<SysMenu> subMenus;
}
