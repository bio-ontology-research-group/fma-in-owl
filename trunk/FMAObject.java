import java.util.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

public class FMAObject {
    public String name ;
    public int frame ;
    public int fmaid ;
    public String definition ;
    public List<FMAObject> subclass = new LinkedList();
    public List<FMAObject> superclass = new LinkedList() ;
    public OntClass owlclass = null ;
    public List<FMARelInst> rels = new LinkedList() ;
    public List<FMAObject> hasConstPart = new LinkedList() ;
    public List<FMAObject> hasRegPart = new LinkedList() ;
    public List<FMAObject> hasPart = new LinkedList() ;
    public List<FMAObject> partOf = new LinkedList() ;
    public List<FMAObject> hasSystPart = new LinkedList() ;
    public List<FMAObject> boundedBy = new LinkedList() ;
    public List<FMAObject> has3DShape = new LinkedList() ;
    public List<FMAObject> developsFrom = new LinkedList() ;
}
