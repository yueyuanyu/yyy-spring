package com.spring;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public class ApplicationContext {
    //bean 定义的Map
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    //单例bean池
    private Map<String, Object> singleObjectMap = new HashMap<>();
    //beanPostProcessor池
    private List<BeanPostProcessor> beanPostProcessors = new ArrayList<BeanPostProcessor>();

    public ApplicationContext(Class configClass) throws Throwable {
        //读这个类的扫描路径 确定需要扫那些包,完成beanDefinition beanpostprocessor的初始化
        scan(configClass);
        //完成单例池的初始化
        Set<Map.Entry<String, BeanDefinition>> entries = beanDefinitionMap.entrySet();
        for (Map.Entry<String, BeanDefinition> entry : entries) {
            if (entry.getValue().getScope() != null && entry.getValue().getScope().equals("singleton")) {
               singleObjectMap.put(entry.getKey(),doCreateBean(entry.getKey(),entry.getValue()));
            }
        }

    }


    public Object getBean(String beanName) {
        if (!beanDefinitionMap.containsKey(beanName)){
            throw new RuntimeException();
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        Object bean=null;
        if (beanDefinition.getScope().equals("singleton")){
             bean = singleObjectMap.get(beanName);
            if (bean==null) {
                bean=doCreateBean(beanName,beanDefinition);
            }
        }else{
            bean=doCreateBean(beanName,beanDefinition);
        }

        return bean;
    }

    private Object doCreateBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();
        Object instance = null;
        try {
            instance = clazz.getConstructor().newInstance();

            //进行依赖注入
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(AutoWired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
            }
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessors) {
                beanPostProcessor.postProcessAfterInitialization(instance,beanName);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return instance;
    }


    private void scan(Class configClass) throws Throwable {
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAno = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String scanPath = componentScanAno.value();

            //对扫描路径做处理以获取待扫描的文件
            scanPath = scanPath.replace(".", "/");

            //java类加载器分bootstrap(找/lib下的) 扩展类(找/lib/ext下的) 应用程序(找用户自己指定的) 三类
            //因此用ApplicationContext的类加载器是用的应用程序加载器 因此相对路径和要扫的文件一致
            ClassLoader classLoader = ApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(scanPath);
            //此时拿到的是一个文件夹,需要遍历此文件夹下的文件 即yyy目录下的待扫描文件
            File file = new File(resource.getFile());
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    //要加载这些类需要截取当前文件的路径中包名的部分
                    String absolutePath = f.getAbsolutePath();
                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    String packagePath = absolutePath.replace("\\", ".");

                    //加载这些类,判断有没有component注解需要被纳入容器管理的
                    Class<?> loadClass = classLoader.loadClass(packagePath);
                    if (loadClass.isAnnotationPresent(Component.class)) {
                        Component componentAno = loadClass.getAnnotation(Component.class);
                        String beanName = componentAno.value();
                        if ("".equals(beanName)) {
                            beanName = Introspector.decapitalize(loadClass.getSimpleName());
                        }
                        //如果类是beanPostprocessor的实现类则加入到listzhong
                        if (BeanPostProcessor.class.isAssignableFrom(loadClass)) {
                            BeanPostProcessor o = (BeanPostProcessor) loadClass.getConstructor().newInstance();
                            beanPostProcessors.add(o);
                        } else {
                            //是普通的类构建beanDefinition
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(loadClass);
                            //判断他的scope是否有scope注解
                            if (loadClass.isAnnotationPresent(Scope.class)) {
                                Scope scopeAno = loadClass.getAnnotation(Scope.class);
                                String value = scopeAno.value();
                                beanDefinition.setScope(value);
                            } else {
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName, beanDefinition);
                        }
                    }
                }

            }
        }
    }
}
