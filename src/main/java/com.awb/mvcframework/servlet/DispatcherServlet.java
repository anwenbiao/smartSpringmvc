package com.awb.mvcframework.servlet;

import com.awb.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class DispatcherServlet extends HttpServlet {

    //web.xml中的param-name
    private static final String LOCATION = "contextConfigLocation";
    //保存所有的配置信息
    private static Properties properties = new Properties();
    // 保存所有被扫描到的相关的类名
    private List<String> classNames = new ArrayList<>();
    //ioc容器 保存所有初始化的bean
    private Map<String, Object> ioc = new HashMap<>(16);
    //保存所有的URL和方法的映射关系
    private Map<String, Method> handlerMapping = new HashMap<>(16);

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doDispatch(req, resp);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter(LOCATION));

        //2、扫描所有的类
        doSCanner(properties.getProperty("scanPackage"));

        //3、加载所有类的实例
        doLoadClassInstance();

        //4、依赖注入
        doAutoWired();
        
        //5、构造handlerMapping
        initHandlerMapping();

        //6、请求匹配Url  反射调用执行

        System.out.println("mvcframework init close");

    }

    private void doDispatch(HttpServletRequest request,HttpServletResponse response) throws IOException {
        if(this.handlerMapping.isEmpty()){return;}

        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        //匹配不到路径
        if(!handlerMapping.containsKey(url)){
            response.getWriter().write("404");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //获取方法的所有参数
        Class<?>[] parameterTypes = method.getParameterTypes();
        //获取请求的参数
        Map<String,String[]> parameterMap = request.getParameterMap();
        //保存所有的参数值
        Object[] paramValues = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if(parameterType==HttpServletRequest.class){
                paramValues[i] = request;
                continue;
            }else if (parameterType==HttpServletResponse.class){
                paramValues[i] = response;
                continue;
            }else if(parameterType==String.class){
                for (Map.Entry<String, String[]> param : parameterMap.entrySet()) {
                    String value = Arrays.toString(param.getValue())
                            .replaceAll("\\[|\\]", "")
                            .replaceAll("\\s", "");
                    paramValues[i] = value;
                }
            }
        }
        //getSimpleName()相当于获取类明的简称：UserController, getName()获取的值是：com.awb.mvc.action.UserController
        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(this.ioc.get(beanName), paramValues);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


    }


    private void initHandlerMapping() {
        if(ioc.isEmpty()){return;}

        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(Controller.class)){continue;}

            String baseUrl = "";
            //读取controller 上的url配置
            if(clazz.isAnnotationPresent(RequestMapping.class)){
                RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                baseUrl = annotation.value();
            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if(!method.isAnnotationPresent(RequestMapping.class)){continue;}

                RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
                String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
                handlerMapping.put(url, method);
            }
        }

    }

    private void doAutoWired() {
        if(ioc.isEmpty()){return;}
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if(!field.isAnnotationPresent(Autowired.class)){continue;}

                Autowired annotation = field.getAnnotation(Autowired.class);
                String beanName = annotation.value().trim();
                if("".equals(beanName)){
                    beanName = field.getType().getName();
                }
                //设置访问权限
                field.setAccessible(true);
                try {
                    //设置 实例
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            
        }
    }

    private void doLoadClassInstance() {
        if(classNames.isEmpty()){return;}
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if(clazz.isAnnotationPresent(Controller.class)){
                    String beanName = lowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName, clazz.newInstance());
                }else if (clazz.isAnnotationPresent(Service.class)){
                    Service annotation = clazz.getAnnotation(Service.class);
                    String beanName = annotation.value();
                    //自己本身设置了value属性
                    if(!"".equals(beanName.trim())){
                        ioc.put(beanName, clazz.newInstance());
                        continue;
                    }
                    //自己没有设置value属性  按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(), clazz.newInstance());
                    }
                }else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void doSCanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        File[] files = file.listFiles();
        for (File file1 : files) {
            if(file1.isDirectory()){
                doSCanner(scanPackage+"."+file1.getName());
            }else {
                classNames.add(scanPackage+"."+file1.getName().replace(".class","").trim());
            }
        }

    }

    private void doLoadConfig(String location) {
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = this.getClass().getResourceAsStream("/"+location);
            //读取配置文件
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * s首字母转换成小写
     * @param string
     * @return
     */
    private String lowerFirstCase(String string){
        char[] chars = string.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
