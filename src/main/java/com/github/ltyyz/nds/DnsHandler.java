package com.github.ltyyz.nds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.dns.*;

import java.util.HashMap;
import java.util.Map;

public class DnsHandler extends SimpleChannelInboundHandler<DatagramDnsQuery> {

    private Map<String, byte[]> domainIpMapping = new HashMap<>();

    public DnsHandler() {
        // 本地加一些临时数据，正确做法是入库
        domainIpMapping.put("www.qq.com.", new byte[]{111, (byte) 161, (byte) 64, 40});
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DatagramDnsQuery query) {
        DatagramDnsResponse response = new DatagramDnsResponse(query.recipient(), query.sender(), query.id());
        try {
            DefaultDnsQuestion dnsQuestion = query.recordAt(DnsSection.QUESTION);
            response.addRecord(DnsSection.QUESTION, dnsQuestion);
            System.out.println("查询域名：" + dnsQuestion.name());

            ByteBuf buf;
            if (domainIpMapping.containsKey(dnsQuestion.name())) {
                buf = Unpooled.wrappedBuffer(domainIpMapping.get(dnsQuestion.name()));

            } else {
                // 不在 ipMapping表中的 域名
                buf = Unpooled.wrappedBuffer(new byte[] { 127, 0, 0, 1});
            }

            DefaultDnsRawRecord queryAnswer = new DefaultDnsRawRecord(dnsQuestion.name(),
                    DnsRecordType.A, 10, buf);
            response.addRecord(DnsSection.ANSWER, queryAnswer);

        } catch (Exception e) {
            System.out.println("查询域名异常：" + e);
        } finally {
            ctx.writeAndFlush(response);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }
}
