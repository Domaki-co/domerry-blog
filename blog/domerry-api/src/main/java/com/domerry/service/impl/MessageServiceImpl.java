package com.domerry.service.impl;

import com.domerry.service.MessageService;
import com.domerry.entity.SysMessage;
import com.domerry.mapper.SysMessageMapper;
import com.domerry.utils.IpUtil;
import com.domerry.utils.SensitiveUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final SysMessageMapper messageMapper;

    @Override
    public List<SysMessage> getMessageList() {
        return messageMapper.selectList(null);
    }

    @Override
    public Boolean add(SysMessage sysMessage) {
        String ip = IpUtil.getIp();
        sysMessage.setIp(ip);
        sysMessage.setSource(IpUtil.getIp2region(ip));
        sysMessage.setContent(SensitiveUtil.filter(sysMessage.getContent()));
        messageMapper.insert(sysMessage);
        return true;
    }
}
