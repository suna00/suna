package net.ion.ice.core.event;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.TemplateParam;
import net.ion.ice.core.data.DatabaseService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class ActionUpdate extends Action {

    protected JdbcTemplate jdbcTemplate ;
    protected List<TemplateParam> parameters ;


    @Override
    public void execute(ExecuteContext executeContext) {
        if(this.jdbcTemplate == null){
            DatabaseService dbService = ApplicationContextManager.getBean(DatabaseService.class) ;
            this.jdbcTemplate = dbService.getJdbcTemplate(datasource) ;
        }

        if(this.parameters == null){
            this.parameters = new ArrayList<>() ;

        }

        this.jdbcTemplate.update(this.actionBody) ;
    }

    public ActionUpdate(String datasource, String actionBody) {
        super(datasource, actionBody);
    }
}
