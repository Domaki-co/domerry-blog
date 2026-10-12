package com.domerry.vo.moment;

import com.domerry.entity.SysMoment;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author: quequnlong
 * @date: 2025/2/5
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MomentPageVo extends SysMoment {

    private String nickname;

    private String avatar;

}
