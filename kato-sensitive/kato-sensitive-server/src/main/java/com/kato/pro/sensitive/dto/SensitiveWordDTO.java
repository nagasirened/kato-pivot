package com.kato.pro.sensitive.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

/**
 * 敏感词导入导出DTO
 */
@Data
public class SensitiveWordDTO {

    @ExcelProperty("敏感词")
    @ColumnWidth(20)
    private String word;

    @ExcelProperty("敏感等级")
    @ColumnWidth(15)
    private String level;

    @ExcelProperty("分类")
    @ColumnWidth(15)
    private String category;

    @ExcelProperty("状态")
    @ColumnWidth(10)
    private String status;

    @ExcelProperty("备注")
    @ColumnWidth(30)
    private String remark;
}
