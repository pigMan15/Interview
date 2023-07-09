package com.spring;

import jdk.jfr.AnnotationElement;

import java.beans.Introspector;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PigmanApplicationContext {

    private Class configClass;

    /**
     * bean定义
     */
    private Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    /**
     * 单例池
     */
    private Map<String, Object> singletonObjects = new HashMap<>();

    /**
     *  处理器集合
     */
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public PigmanApplicationContext(Class configClass) {
        this.configClass = configClass;


        //扫描
        scan(configClass);

        /**
         * 创建bean（单例且不为懒加载类型）
         */
        for (Map.Entry<String, BeanDefinition> entry:  beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();

            if (beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }

        }

    }

    /**
     * 扫描包路径下的非懒加载单例bean
     * @param configClass
     */
    private void scan(Class configClass) {
        if (configClass.isAnnotationPresent(ComponentScan.class)) {

            /**
             * 获取扫描包下的所有类文件
             */
            ComponentScan componentScan = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScan.value();
            path = path.replace(".", "/");

            ClassLoader classLoader = PigmanApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(path);
            File file = new File(resource.getFile());

            if (file.isDirectory()) {
                for (File f: file.listFiles()) {
                    String absolutePath = f.getAbsolutePath();

                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    absolutePath = absolutePath.replace("/", ".");

                    try {

                        Class<?> clazz = classLoader.loadClass(absolutePath);

                        /**
                         * 寻找@Component注解的Bean对象
                         */
                        if (clazz.isAnnotationPresent(Component.class)) {

                            /**
                             * 处理PostProcessor实现类的Bean
                             */
                            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }

                            /**
                             * 根据类及注解，构造BeanDefinition
                             */
                            Component component = clazz.getAnnotation(Component.class);
                            String beanName = component.value();
                            if ("".equals(beanName)) {
                                beanName = Introspector.decapitalize(clazz.getSimpleName());
                            }
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(clazz);

                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scope = clazz.getAnnotation(Scope.class);
                                String value = scope.value();
                                beanDefinition.setScope(value);
                            } else {
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinitionMap.put(beanName, beanDefinition);

                        }

                    } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }


                }
            }


        }
    }

    /**
     * 根据beanName获取Bean对象
     * @param beanName
     * @return
     */
    public Object getBean(String beanName) {
       if (!beanDefinitionMap.containsKey(beanName)) {
           throw new NullPointerException();
       }
       BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        /**
         * 如果为单例bean，从单例池获取已创建对象；为原型（多例）bean，则每次获取时创建新对象
         */
        if (beanDefinition.getScope().equals("singleton")) {
           Object singleTonObject = singletonObjects.get(beanName);
           if (singleTonObject == null) {
               singleTonObject = createBean(beanName, beanDefinition);
               singletonObjects.put(beanName, singleTonObject);
           }
           return singleTonObject;
       } else {
           Object prototypeBean = createBean(beanName, beanDefinition);
           return prototypeBean;
       }
    }

    /**
     *
     * @param beanName
     * @param beanDefinition
     * @return
     */
    private Object createBean(String beanName, BeanDefinition beanDefinition) {

        Class clazz = beanDefinition.getType();

        Object instance = null;

        try {
            instance = clazz.getConstructor().newInstance();

            /**
             * 创建bean后 进行依赖注入
             */
            for (Field field: clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);
                    field.set(instance, getBean(field.getName()));
                }
            }

            /**
             *
             */
            if (instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }

            /**
             * 前置处理器
             */
            for (BeanPostProcessor beanPostProcessor: beanPostProcessorList) {
                beanPostProcessor.postProcessBeforeInitialization(instance, beanName);
            }

            /**
             * bean创建完成，初始化
             */
            if (instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }

            /**
             * 后置处理器 AOP
             */
            for (BeanPostProcessor beanPostProcessor: beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance, beanName);
            }


        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return instance;
    }

}
