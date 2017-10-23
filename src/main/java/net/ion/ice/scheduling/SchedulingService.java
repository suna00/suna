package net.ion.ice.scheduling;

import com.hazelcast.core.HazelcastInstance;
import net.ion.ice.core.cluster.ClusterConfiguration;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.node.Node;
import net.ion.ice.core.node.NodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component("schedulingService")
public class SchedulingService implements InitializingBean{
    public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private NodeService nodeService ;

    @Autowired
    private BeanFactory beanFactory;

    @Autowired
    ClusterConfiguration clusterConfiguration;


    private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

    private final Map<String, ScheduledTask> scheduledTasks =new ConcurrentHashMap<>();

    @PostConstruct
    public void initSchedule(){
        if (this.scheduler != null) {
            this.registrar.setScheduler(this.scheduler);
            logger.info(this.scheduler.toString());
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        List<Node> scheduleNodes = nodeService.getNodeList("scheduleTask", "enable_matching=true") ;
        logger.info("Schedule Register : " + scheduleNodes);
        for(Node scheduleNode : scheduleNodes){
            makeSchedule(scheduleNode) ;
        }

    }

    public void save(ExecuteContext context){
        Node scheduleNode = context.getNode() ;
        if(!scheduleNode.getBooleanValue("enable") && scheduledTasks.containsKey(scheduleNode.getId())){
            scheduledTasks.get(scheduleNode.getId()).cancel();
        }else{
            makeSchedule(scheduleNode);
        }
    }


    private void makeSchedule(Node scheduleNode) {
        if(clusterConfiguration == null) return ;
        if(!(clusterConfiguration.getMode().equals("all") || clusterConfiguration.getMode().equals("cms"))) return ;
        if(!clusterConfiguration.getHazelcast().getCluster().getLocalMember().getAddress().getHost().equals("10.75.7.130")){
            return;
        }

        logger.info("Schedule RUN : " + scheduleNode);

        // Determine initial delay
        long initialDelay = 10000;

        String scheduleType = scheduleNode.getStringValue("type") ;
        String scheduleConfig = scheduleNode.getStringValue("config") ;

        if(scheduledTasks.containsKey(scheduleNode.getId())){
            try {
                scheduledTasks.get(scheduleNode.getId()).cancel();
            }catch(Exception e){}
        }
        ScheduleNodeRunner runnable = new ScheduleNodeRunner(scheduleNode, clusterConfiguration.getHazelcast()) ;

        switch (scheduleType){
            case "cron":{
                scheduledTasks.put(scheduleNode.getId(), this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(scheduleConfig))));
                break ;
            }
            case "delay":{
                long fixedDelay = Long.parseLong(scheduleConfig);
                scheduledTasks.put(scheduleNode.getId(), this.registrar.scheduleFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay)));
                break ;
            }
            case "rate":{
                long fixedRate = Long.parseLong(scheduleConfig);
                scheduledTasks.put(scheduleNode.getId(), this.registrar.scheduleFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay)));
                break ;
            }
        }
    }

    public boolean requiresDestruction(Object bean) {
        synchronized (this.scheduledTasks) {
            return this.scheduledTasks.containsKey(bean);
        }
    }

    public void destroy() {
        synchronized (this.scheduledTasks) {
            Collection<ScheduledTask> allTasks = this.scheduledTasks.values();
            for (ScheduledTask task : allTasks) {
                task.cancel();
            }
            this.scheduledTasks.clear();
        }
        this.registrar.destroy();
    }

}

