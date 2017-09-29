package net.ion.ice.scheduling;

import net.ion.ice.core.node.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.NamedBeanHolder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Schedules;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.IntervalTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.ScheduledMethodRunnable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;


@Component("schedulingService")
public class SchedulingService {
    public static final String DEFAULT_TASK_SCHEDULER_BEAN_NAME = "taskScheduler";

    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private TaskScheduler scheduler;

    @Autowired
    private NodeService nodeService ;

    private StringValueResolver embeddedValueResolver;

    private String beanName;

    @Autowired
    private BeanFactory beanFactory;

    private ApplicationContext applicationContext;

    private final ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

    private final Map<Object, Set<ScheduledTask>> scheduledTasks =
            new IdentityHashMap<Object, Set<ScheduledTask>>(16);

    @PostConstruct
    public void initSchedule(){
        if (this.scheduler != null) {
            this.registrar.setScheduler(this.scheduler);
            logger.info(this.scheduler);
//            nodeService.getNodeList("schedule", "useYn_matching=Y") ;
        }

    }


    private void finishRegistration() {

        if (this.registrar.hasTasks() && this.registrar.getScheduler() == null) {
            Assert.state(this.beanFactory != null, "BeanFactory must be set to find scheduler by type");
            try {
                // Search for TaskScheduler bean...
                this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, false));
            }
            catch (NoUniqueBeanDefinitionException ex) {
                logger.debug("Could not find unique TaskScheduler bean", ex);
                try {
                    this.registrar.setTaskScheduler(resolveSchedulerBean(TaskScheduler.class, true));
                }
                catch (NoSuchBeanDefinitionException ex2) {
                    if (logger.isInfoEnabled()) {
                        logger.info("More than one TaskScheduler bean exists within the context, and " +
                                "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                                "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                                "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                                ex.getBeanNamesFound());
                    }
                }
            }
            catch (NoSuchBeanDefinitionException ex) {
                logger.debug("Could not find default TaskScheduler bean", ex);
                // Search for ScheduledExecutorService bean next...
                try {
                    this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, false));
                }
                catch (NoUniqueBeanDefinitionException ex2) {
                    logger.debug("Could not find unique ScheduledExecutorService bean", ex2);
                    try {
                        this.registrar.setScheduler(resolveSchedulerBean(ScheduledExecutorService.class, true));
                    }
                    catch (NoSuchBeanDefinitionException ex3) {
                        if (logger.isInfoEnabled()) {
                            logger.info("More than one ScheduledExecutorService bean exists within the context, and " +
                                    "none is named 'taskScheduler'. Mark one of them as primary or name it 'taskScheduler' " +
                                    "(possibly as an alias); or implement the SchedulingConfigurer interface and call " +
                                    "ScheduledTaskRegistrar#setScheduler explicitly within the configureTasks() callback: " +
                                    ex2.getBeanNamesFound());
                        }
                    }
                }
                catch (NoSuchBeanDefinitionException ex2) {
                    logger.debug("Could not find default ScheduledExecutorService bean", ex2);
                    // Giving up -> falling back to default scheduler within the registrar...
                    logger.info("No TaskScheduler/ScheduledExecutorService bean found for scheduled processing");
                }
            }
        }

        this.registrar.afterPropertiesSet();
    }

    private <T> T resolveSchedulerBean(Class<T> schedulerType, boolean byName) {
        if (byName) {
            T scheduler = this.beanFactory.getBean(DEFAULT_TASK_SCHEDULER_BEAN_NAME, schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        DEFAULT_TASK_SCHEDULER_BEAN_NAME, this.beanName);
            }
            return scheduler;
        }
        else if (this.beanFactory instanceof AutowireCapableBeanFactory) {
            NamedBeanHolder<T> holder = ((AutowireCapableBeanFactory) this.beanFactory).resolveNamedBean(schedulerType);
            if (this.beanFactory instanceof ConfigurableBeanFactory) {
                ((ConfigurableBeanFactory) this.beanFactory).registerDependentBean(
                        holder.getBeanName(), this.beanName);
            }
            return holder.getBeanInstance();
        }
        else {
            return this.beanFactory.getBean(schedulerType);
        }
    }


    public void postProcessMergedBeanDefinition(RootBeanDefinition beanDefinition, Class<?> beanType, String beanName) {
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    public Object postProcessAfterInitialization(final Object bean, String beanName) {
        Class<?> targetClass = AopUtils.getTargetClass(bean);
//        if (!this.nonAnnotatedClasses.contains(targetClass)) {
//            Map<Method, Set<Scheduled>> annotatedMethods = MethodIntrospector.selectMethods(targetClass,
//                    new MethodIntrospector.MetadataLookup<Set<Scheduled>>() {
//                        @Override
//                        public Set<Scheduled> inspect(Method method) {
//                            Set<Scheduled> scheduledMethods = AnnotatedElementUtils.getMergedRepeatableAnnotations(
//                                    method, Scheduled.class, Schedules.class);
//                            return (!scheduledMethods.isEmpty() ? scheduledMethods : null);
//                        }
//                    });
//            if (annotatedMethods.isEmpty()) {
//                this.nonAnnotatedClasses.add(targetClass);
//                if (logger.isTraceEnabled()) {
//                    logger.trace("No @Scheduled annotations found on bean class: " + bean.getClass());
//                }
//            }
//            else {
//                // Non-empty set of methods
//                for (Map.Entry<Method, Set<Scheduled>> entry : annotatedMethods.entrySet()) {
//                    Method method = entry.getKey();
//                    for (Scheduled scheduled : entry.getValue()) {
//                        processScheduled(scheduled, method, bean);
//                    }
//                }
//                if (logger.isDebugEnabled()) {
//                    logger.debug(annotatedMethods.size() + " @Scheduled methods processed on bean '" + beanName +
//                            "': " + annotatedMethods);
//                }
//            }
//        }
        return bean;
    }

    protected void processScheduled(Scheduled scheduled, Method method, Object bean) {
        try {
            Assert.isTrue(method.getParameterTypes().length == 0,
                    "Only no-arg methods may be annotated with @Scheduled");

            Method invocableMethod = AopUtils.selectInvocableMethod(method, bean.getClass());
            Runnable runnable = new ScheduledMethodRunnable(bean, invocableMethod);
            boolean processedSchedule = false;
            String errorMessage =
                    "Exactly one of the 'cron', 'fixedDelay(String)', or 'fixedRate(String)' attributes is required";

            Set<ScheduledTask> tasks = new LinkedHashSet<ScheduledTask>(4);

            // Determine initial delay
            long initialDelay = scheduled.initialDelay();
            String initialDelayString = scheduled.initialDelayString();
            if (StringUtils.hasText(initialDelayString)) {
                Assert.isTrue(initialDelay < 0, "Specify 'initialDelay' or 'initialDelayString', not both");
                if (this.embeddedValueResolver != null) {
                    initialDelayString = this.embeddedValueResolver.resolveStringValue(initialDelayString);
                }
                try {
                    initialDelay = Long.parseLong(initialDelayString);
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid initialDelayString value \"" + initialDelayString + "\" - cannot parse into integer");
                }
            }

            // Check cron expression
            String cron = scheduled.cron();
            if (StringUtils.hasText(cron)) {
                Assert.isTrue(initialDelay == -1, "'initialDelay' not supported for cron triggers");
                processedSchedule = true;
                String zone = scheduled.zone();
                if (this.embeddedValueResolver != null) {
                    cron = this.embeddedValueResolver.resolveStringValue(cron);
                    zone = this.embeddedValueResolver.resolveStringValue(zone);
                }
                TimeZone timeZone;
                if (StringUtils.hasText(zone)) {
                    timeZone = StringUtils.parseTimeZoneString(zone);
                }
                else {
                    timeZone = TimeZone.getDefault();
                }
                tasks.add(this.registrar.scheduleCronTask(new CronTask(runnable, new CronTrigger(cron, timeZone))));
            }

            // At this point we don't need to differentiate between initial delay set or not anymore
            if (initialDelay < 0) {
                initialDelay = 0;
            }

            // Check fixed delay
            long fixedDelay = scheduled.fixedDelay();
            if (fixedDelay >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                tasks.add(this.registrar.scheduleFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay)));
            }
            String fixedDelayString = scheduled.fixedDelayString();
            if (StringUtils.hasText(fixedDelayString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedDelayString = this.embeddedValueResolver.resolveStringValue(fixedDelayString);
                }
                try {
                    fixedDelay = Long.parseLong(fixedDelayString);
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedDelayString value \"" + fixedDelayString + "\" - cannot parse into integer");
                }
                tasks.add(this.registrar.scheduleFixedDelayTask(new IntervalTask(runnable, fixedDelay, initialDelay)));
            }

            // Check fixed rate
            long fixedRate = scheduled.fixedRate();
            if (fixedRate >= 0) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                tasks.add(this.registrar.scheduleFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay)));
            }
            String fixedRateString = scheduled.fixedRateString();
            if (StringUtils.hasText(fixedRateString)) {
                Assert.isTrue(!processedSchedule, errorMessage);
                processedSchedule = true;
                if (this.embeddedValueResolver != null) {
                    fixedRateString = this.embeddedValueResolver.resolveStringValue(fixedRateString);
                }
                try {
                    fixedRate = Long.parseLong(fixedRateString);
                }
                catch (NumberFormatException ex) {
                    throw new IllegalArgumentException(
                            "Invalid fixedRateString value \"" + fixedRateString + "\" - cannot parse into integer");
                }
                tasks.add(this.registrar.scheduleFixedRateTask(new IntervalTask(runnable, fixedRate, initialDelay)));
            }

            // Check whether we had any attribute set
            Assert.isTrue(processedSchedule, errorMessage);

            // Finally register the scheduled tasks
            synchronized (this.scheduledTasks) {
                Set<ScheduledTask> registeredTasks = this.scheduledTasks.get(bean);
                if (registeredTasks == null) {
                    registeredTasks = new LinkedHashSet<ScheduledTask>(4);
                    this.scheduledTasks.put(bean, registeredTasks);
                }
                registeredTasks.addAll(tasks);
            }
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Encountered invalid @Scheduled method '" + method.getName() + "': " + ex.getMessage());
        }
    }

    public void postProcessBeforeDestruction(Object bean, String beanName) {
        Set<ScheduledTask> tasks;
        synchronized (this.scheduledTasks) {
            tasks = this.scheduledTasks.remove(bean);
        }
        if (tasks != null) {
            for (ScheduledTask task : tasks) {
                task.cancel();
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
            Collection<Set<ScheduledTask>> allTasks = this.scheduledTasks.values();
            for (Set<ScheduledTask> tasks : allTasks) {
                for (ScheduledTask task : tasks) {
                    task.cancel();
                }
            }
            this.scheduledTasks.clear();
        }
        this.registrar.destroy();
    }

}

