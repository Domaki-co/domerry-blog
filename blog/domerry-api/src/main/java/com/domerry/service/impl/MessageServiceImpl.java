package com.domerry.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.domerry.service.MessageService;
import com.domerry.entity.SysMessage;
import com.domerry.entity.SysUser;
import com.domerry.exception.ServiceException;
import com.domerry.mapper.SysMessageMapper;
import com.domerry.mapper.SysUserMapper;
import com.domerry.utils.HtmlUtil;
import com.domerry.utils.IpUtil;
import com.domerry.utils.SensitiveUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final SysMessageMapper messageMapper;

    private final SysUserMapper sysUserMapper;

    @Override
    public List<SysMessage> getMessageList() {
        return messageMapper.selectList(null);
    }

    @Override
    public Boolean add(SysMessage sysMessage) {
        // 身份一律以服务端会话为准，客户端传来的 nickname/avatar 不采信
        SysUser user = sysUserMapper.selectById(StpUtil.getLoginIdAsLong());
        if (user == null) {
            throw new ServiceException("登录用户不存在");
        }
        sysMessage.setNickname(user.getNickname());
        sysMessage.setAvatar(user.getAvatar());

        String ip = IpUtil.getIp();
        sysMessage.setIp(ip);
        sysMessage.setSource(IpUtil.getIp2region(ip));
        // 入库前先剥离 HTML，避免留言内容被后台 v-html 渲染时执行脚本
        sysMessage.setContent(SensitiveUtil.filter(HtmlUtil.toPlainText(sysMessage.getContent())));
        messageMapper.insert(sysMessage);
        return true;
    }
}
