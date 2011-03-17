import java.util.* ;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

public class FMARel {
    public String name ;
    public int fmaid ;
    public int frame ;
    public String valueType ;
    public OntProperty prop = null ;
    public FMARel inverse  = null ;
    public List<FMAObject> domain = new LinkedList() ;
    public List<FMAObject> range = new LinkedList() ;
    public String description = null ;
    public List<String> synonyms = new LinkedList() ;
}
