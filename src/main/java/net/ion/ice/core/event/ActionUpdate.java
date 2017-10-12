package net.ion.ice.core.event;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.Template;
import net.ion.ice.core.data.DBService;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class ActionUpdate extends Action {

    protected JdbcTemplate jdbcTemplate ;

    protected Template sqlTemplate  ;

    @Override
    public void execute(ExecuteContext executeContext) {
        if(this.jdbcTemplate == null){
            DBService dbService = ApplicationContextManager.getBean(DBService.class) ;
            this.jdbcTemplate = dbService.getJdbcTemplate(datasource) ;
        }

        if(this.sqlTemplate == null){
            this.sqlTemplate = new Template(this.actionBody) ;
            this.sqlTemplate.parsing();
        }

        this.jdbcTemplate.update(this.sqlTemplate.format(executeContext.getData()).toString(), this.sqlTemplate.getSqlParameterValues(executeContext.getData())) ;

    }

    @Override
    public void execute() {
        System.out.println("net.ion.ice.core.event.ActionUpdate :: Not yet finished");
    }

    public ActionUpdate(String datasource, String actionBody) {
        super(datasource, actionBody);
    }
}
