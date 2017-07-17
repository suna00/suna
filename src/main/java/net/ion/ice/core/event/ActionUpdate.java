package net.ion.ice.core.event;

import net.ion.ice.ApplicationContextManager;
import net.ion.ice.core.context.ExecuteContext;
import net.ion.ice.core.context.Template;
import net.ion.ice.core.context.Template;
import net.ion.ice.core.context.TemplateParam;
import net.ion.ice.core.data.DatabaseService;
import org.springframework.jdbc.core.JdbcTemplate;

import java.text.ParseException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jaeho on 2017. 7. 12..
 */
public class ActionUpdate extends Action {

    protected JdbcTemplate jdbcTemplate ;

    protected Template sqlTemplate  ;

    @Override
    public void execute(ExecuteContext executeContext) {
        if(this.jdbcTemplate == null){
            DatabaseService dbService = ApplicationContextManager.getBean(DatabaseService.class) ;
            this.jdbcTemplate = dbService.getJdbcTemplate(datasource) ;
        }

        if(this.sqlTemplate == null){
            this.sqlTemplate = new Template(this.actionBody) ;
            this.sqlTemplate.parsing();
        }

        try {
            this.jdbcTemplate.update(this.sqlTemplate.format(executeContext.getData()), this.sqlTemplate.getSqlParameterValues(executeContext.getData())) ;
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public ActionUpdate(String datasource, String actionBody) {
        super(datasource, actionBody);
    }
}
