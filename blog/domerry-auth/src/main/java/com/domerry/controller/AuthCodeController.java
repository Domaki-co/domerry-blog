package com.domerry.controller;

import com.domerry.common.Result;
import com.domerry.constant.AuthCodeConstants;
import com.domerry.utils.RedisUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Api(tags = "认证管理")
public class AuthCodeController {

    private final RedisUtil redisUtil;

    /**
     * 第三方登录回调只往 URL 里放一次性 code，token 由前端主动换取，
     * 这样长期凭证不会出现在浏览器历史 / Referer 里，会话固定攻击也无从下手。
     * 接口本身无需登录态（此刻前端还没有 token），安全性由 code 的短时效 + 用后即焚保证。
     */
    @ApiOperation(value = "用第三方登录一次性 code 换取 token")
    @PostMapping("/auth/exchangeCode")
    public Result<String> exchangeCode(@RequestParam(required = false) String code) {
        if (code == null || code.trim().isEmpty()) {
            return Result.error("缺少登录凭证");
        }
        String key = AuthCodeConstants.THIRD_PARTY_LOGIN_CODE + code;
        Object token = redisUtil.get(key);
        if (token == null) {
            return Result.error("登录凭证无效或已过期，请重新登录");
        }
        // 用 delete 的返回值做仲裁：并发的两次换取里只有一个能删成功，另一个必须失败，
        // 否则同一个 code 会被重复消费。key 在 get 与 delete 之间过期同样落到这里。
        if (!Boolean.TRUE.equals(redisUtil.delete(key))) {
            return Result.error("登录凭证无效或已过期，请重新登录");
        }
        return Result.success(token.toString());
    }
}
