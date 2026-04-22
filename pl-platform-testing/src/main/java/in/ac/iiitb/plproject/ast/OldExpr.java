package in.ac.iiitb.plproject.ast;

public class OldExpr extends Expr {
    private final Expr inner;
    
    public OldExpr(Expr e) { 
        this.inner = e; 
    }
    
    public Expr getInner() {
        return inner;
    }
    
    @Override 
    public String toString() { 
        return "\\old(" + inner + ")"; 
    }
}