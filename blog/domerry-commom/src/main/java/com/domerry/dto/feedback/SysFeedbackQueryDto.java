package com.domerry.dto.feedback;

import com.domerry.entity.SysFeedback;
import lombok.Data;

/**
 * @author: quequnlong
 * @date: 2025/1/12
 * @description:
 */
@Data
public class SysFeedbackQueryDto extends SysFeedback {

    private String source;
}
