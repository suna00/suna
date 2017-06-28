# Spring boot 동적으로 Bean 추가하기

#### 개요
Spring boot 환경에서 dynamic 하게 ApplicationContext 에 Bean 을 추가할 수 있습니다. 예제에서는 `DataSource` bean 을 추가합니다.

#### 0. 준비물
- `org.apache.commons.commons-dbcp2:2.1.1+`


#### 1. Dynamic 하게 Bean 추가하기 

`ApplicationContext` 에 `BeanDefinition` 을 추가함으로 현재 컨텍스트에  빈을 CRUD 할 수 있음

```
    @PostConstruct
    public void loadDataSource () {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry)  context.getAutowireCapableBeanFactory();

        /* 
        	1. dsMap 에는 <키, 데이터 정보맵> 과 같은 형식으로 데이터가 있음
            2. "데이터 정보맵" 에는 <데이터소스키, 데이터소스값> 이 <String, String> 으로 있음 
        */
        this.dsMap.forEach((k,v) -> {

            // Bean 만들고 context 에 추가
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(BasicDataSource.class);
            v.forEach((ds_key, ds_val) -> {
                builder.addPropertyValue(ds_key, ds_val);
            });
            
			/* 
            	registerBeanDefinition 의 첫번째 파라미터는 name 으로
                사용 시에 @Qualifier 의 value 로 사용될 수 있음
            */
            BeanDefinition def = builder.getBeanDefinition();
            if(!registry.containsBeanDefinition(k)) registry.registerBeanDefinition(k, def);
        });
    }
```

#### 2. 가져다 쓰기

-  방법 1. `ApplicationContext` 의 `getBean` 메소드 사용하기
-  방법 2. `@Authowired`, `@Qualifier` 사용하기

가령, bean 의 이름이 `sampleDs1`, `sampleDs2` 라고 가정했을 때 아래와 같이 사용할 수 있음

```
@Controller
public class NodeController {
    private static Logger logger = LoggerFactory.getLogger(NodeController.class);

    @Autowired
    @Qualifier(value = "sampleDs1")
    DataSource ds1;

    @Autowired
    @Qualifier(value = "sampleDs2")
    DataSource ds2;

    @RequestMapping(value = "/node/testDs")
    public void test() {
        logger.info("=== SimpleDataSource Sample :: " + String.valueOf(ds1));
        logger.info("=== SimpleDataSource Sample :: " + String.valueOf(ds2));
    }
}
```

#### 99. 트러블슈팅

1. `DataSource` 관련 설정을 하지 않았는데, boot 에서 빈이 중복된다거나 `driver` 를 지정하지 않았다는 에러를 출력 =>  스프링 자동 설정 제거하기
> a. `@SpringBootApplication` 에 `@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})` 추가
> 
> b. `application.xml` 에 `spring.datasource.url`, `spring.datasource.initializer` 추가
> ```
> spring:
>  profiles:
>    active: ${profile}
>  datasource:
>    url: ${dataSourceUrl}
>    initialize: false
> ```

#### 100. 참고
- 경로 : `/src/main/sample/DatabaseConfig.java`

#### History
- 2017.06.28 : 최초 작성
