package com.awb.mvc.action;

import com.awb.mvcframework.annotation.Autowired;
import com.awb.mvcframework.annotation.Controller;
import com.awb.mvcframework.annotation.RequestMapping;
import com.awb.mvcframework.annotation.RequestParam;
import com.awb.service.UserService;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("getUser")
    public void getUser(@RequestParam("id") String id, HttpServletResponse response){

        String user = userService.getUser(id);
        try {
            response.getWriter().write(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
