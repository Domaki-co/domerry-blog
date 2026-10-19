package com.domerry.dto.album;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "相册密码验证请求参数")
public class AlbumVerifyDto {

    @ApiModelProperty(value = "相册密码")
    private String password;
}
