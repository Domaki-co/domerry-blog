package com.domerry.dto.file;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "删除文件请求参数")
public class FileDeleteDto {

    @ApiModelProperty(value = "文件地址")
    private String url;
}
