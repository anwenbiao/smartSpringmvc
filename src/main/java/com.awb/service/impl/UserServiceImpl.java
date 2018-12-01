package com.awb.service.impl;

import com.awb.mvcframework.annotation.Service;
import com.awb.service.UserService;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public String getUser(String id) {
        return  "service -- getUser:"+id;
    }
}
