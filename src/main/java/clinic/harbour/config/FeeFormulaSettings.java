package clinic.harbour.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.math.BigDecimal;

/** Clinic-wide seasonal fee adjustment rule applied when a visit is requested. */
@ApplicationScoped
public class FeeFormulaSettings {
    private final ExpressionParser parser=new SpelExpressionParser();
    private volatile String expression="#fee";
    public String getExpression(){return expression;}
    public void setExpression(String expression){this.expression=expression;}
    public BigDecimal apply(BigDecimal fee,String appointmentType){
        StandardEvaluationContext context=new StandardEvaluationContext();
        context.setVariable("fee",fee);
        context.setVariable("type",appointmentType);
        Object result=parser.parseExpression(expression).getValue(context);
        return result==null?fee:new BigDecimal(String.valueOf(result));
    }
}
