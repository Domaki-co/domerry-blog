package com.domerry.dto.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "资源验证码校验请求参数")
public class ResourceVerifyDto {

    @ApiModelProperty(value = "验证码")
    private String code;

    @ApiModelProperty(value = "资源id")
    private Long id;
}
