package com.ackerman.util;


import com.alibaba.dubbo.rpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class DubboFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(DubboFilter.class);
    private static final Set<String> whiteIps = new HashSet<String>();

    static {
        whiteIps.add("220.250.64.225");
    }
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String clientIp = RpcContext.getContext().getRemoteHost();
        System.out.println("???");

        if(whiteIps.contains(clientIp)){
            return invoker.invoke(invocation);
        }
        else{
            return new RpcResult();
        }

    }
}
