import java.io.* ;
import java.util.*;
import java.sql.*;
import java.net.* ;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.rdf.model.*;

public class Foo {

    public static void main(String argv[]) throws Exception {
	boolean obo = false ;
	int maxfmaid = 242149;
	PrintWriter debug = new PrintWriter(new BufferedWriter(new FileWriter("debug.out"))) ;
	if (argv.length<1) {
	    System.out.println("Usage: java Foo outputfile [obo]\nIf the second argument is \"obo\", classes will be given numerical IDs as names, otherwise human readable names.") ;
	    System.exit(1) ;
	}
	if (argv.length>1) {
	    if (argv[1].equals("obo"))
		obo = true ;
	}
	
	Map<Integer,FMAObject> objects = new TreeMap() ;
	Map<Integer,FMARel> relations = new TreeMap() ;
	Connection conn = null ;
	java.sql.Statement stmt = null ;
	ResultSet rs = null ;
	try {
	    Class.forName("com.mysql.jdbc.Driver").newInstance();
	} catch (Exception ex) {}
	try {
	    conn = DriverManager.getConnection("jdbc:mysql://localhost/fma?user=fmauser&password=fmauser");
	} catch (SQLException ex) {}
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery("SELECT short_value,frame FROM fma where is_template=0 and slot=\":ROLE\" and frame_type=6 and value_type=3") ;
	    rs.beforeFirst() ;
	    while (rs.next()) {
		FMAObject o = new FMAObject () ;
		o.name = URLEncoder.encode(rs.getString(1).replace(' ','_')) ;
		o.frame = rs.getInt(2) ;
		objects.put(new Integer(o.frame),o) ;
	    }
	    stmt = conn.createStatement();
	    // Select all relationships
	    rs = stmt.executeQuery("SELECT f2.short_value,f2.frame FROM fma f1,fma f2 where f2.is_template=0 and f2.slot=2002 and f2.frame_type=7 and f2.value_type=3 and (f1.short_value=218515 or f1.short_value=161600) and f1.frame=f2.frame") ;
	    rs.beforeFirst() ;
	    while (rs.next()) {
		FMARel r = new FMARel () ;
		r.name = URLEncoder.encode(rs.getString(1).replace(' ','_')) ;
		r.frame = rs.getInt(2) ;
		relations.put(new Integer(r.frame),r) ;
	    }
	    // Reading IDs of relations
	    Iterator<FMARel> rit = relations.values().iterator() ;
	    while (rit.hasNext()) {
		FMARel r = rit.next() ;
		stmt = conn.createStatement();
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=63836 and value_type=3 and frame="+r.frame) ;
		if (rs.next()) {
		    r.fmaid = rs.getInt(1) ;
		}
		// Adding value type of relations; may be broken
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=2014 and value_type=3 and frame="+r.frame) ;
		if (rs.next()) {
		    r.valueType = rs.getString(1) ;
		}
		// Adding inverse relation
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=2015 and value_type=7 and frame="+r.frame) ;
		if (rs.next()) {
		    r.inverse = relations.get(rs.getInt(1)) ;
		}
		// Adding domain: maybe broken
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=2014 and value_type=6 and frame="+r.frame) ;
		while (rs.next()) {
		    r.domain.add(objects.get(rs.getInt(1))) ;
		}
		// Adding range
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=2043 and value_type=6 and frame="+r.frame) ;
		while (rs.next()) {
		    r.range.add(objects.get(rs.getInt(1))) ;
		}
		// Adding description
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=2000 and value_type=3 and frame="+r.frame) ;
		if (rs.next()) {
		    r.description = rs.getString(1) ;
		}
		// Adding synonyms
		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=161601 and value_type=3 and frame="+r.frame) ;
		if (rs.next()) {
		    r.synonyms.add(rs.getString(1)) ;
		}
	    }
	    int c=0;
	    Iterator<FMAObject> it = objects.values().iterator() ;
	    while (it.hasNext()) {
		//		if (c>1000) break;
		if (c++%1000==0) {
		    System.out.println(c +" objects done.") ;
		}
		FMAObject o = it.next() ;
		rit = relations.values().iterator() ;
		while (rit.hasNext()) {
		    FMARel r = rit.next() ;
		    stmt = conn.createStatement();
		    rs = stmt.executeQuery("SELECT short_value FROM fma where slot="+r.frame+" and frame_type=6 and value_type=6 and frame="+o.frame) ;
		    while (rs.next()) {
			FMARelInst inst = new FMARelInst() ;
			inst.relid = r.frame ;
			inst.value = rs.getInt(1) ;
			o.rels.add(inst) ;
		    }
		}
 		stmt = conn.createStatement();
 		rs = stmt.executeQuery("SELECT short_value FROM fma where slot=63836 and frame_type=6 and value_type=3 and frame="+o.frame) ;
 		if (rs.next()) {
 		    o.fmaid = rs.getInt(1) ;
 		} else { // If no fmaid is set, we have to create one... unfortunately
		    o.fmaid=++maxfmaid ;
		    debug.println("No FMAID: "+o.name+"; Adding FMAID "+maxfmaid) ;
		}
 		stmt = conn.createStatement();
 		rs = stmt.executeQuery("SELECT long_value FROM fma where is_template=0 and slot=145445 and frame="+o.frame) ;
 		if (rs.next()) {
 		    o.definition = rs.getString(1) ;
 		}
 		stmt = conn.createStatement();
 		rs = stmt.executeQuery("SELECT short_value FROM fma where is_template=0 and slot=2005 and frame="+o.frame) ;
 		while (rs.next()) {
 		    o.subclass.add(objects.get(rs.getInt(1))) ;
 		}
 		stmt = conn.createStatement();
 		rs = stmt.executeQuery("SELECT short_value FROM fma where is_template=0 and slot=2004 and frame="+o.frame) ;
 		while (rs.next()) {
 		    o.superclass.add(objects.get(rs.getInt(1))) ;
 		}
		if (o.superclass.isEmpty()) {
		    debug.println("No Superclass: "+o.name+"\t"+o.fmaid) ;
		}
// 		stmt = conn.createStatement();//partOf
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=155552 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.partOf.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//hasConstitutionalPart
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=177081 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.hasConstPart.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//hasPart
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=63832 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.hasPart.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//hasRegPart
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=177072 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.hasRegPart.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//hasSystematicPart
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=177106 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.hasSystPart.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//boundedBy
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=155720 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.hasSystPart.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//has inherent 3D shape
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=145493 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.has3DShape.add(objects.get(rs.getInt(1))) ;
// 		}
// 		stmt = conn.createStatement();//developsFrom
// 		rs = stmt.executeQuery("SELECT short_value FROM FMA where is_template=0 and value_type=6 and slot=250740 and frame="+o.frame) ;
// 		while (rs.next()) {
// 		    o.developsFrom.add(objects.get(rs.getInt(1))) ;
// 		}

	    }
	} catch (Exception E) {E.printStackTrace();}
	String ns = "http://onto.eva.mpg.de/fma/fma.owl#" ;
	OntModel m = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM) ;
	// License and contributions issues
	AnnotationProperty contrib = m.createAnnotationProperty(ns+"attribution") ;
	contrib.addComment("This OWL file is based on the FMA developed at the University of"+
			   "Washington by the FMATM Research Project and is provided under license from"+
			   "the University of Washington.","EN") ;
	contrib.addComment("This conversion was done with a program written by Robert Hoehndorf that can be found on"+
			   "http://leechuck.de/poster/index.php?post=20061119165000","EN") ;

	Iterator<FMARel> rit = relations.values().iterator() ;
	while (rit.hasNext()) {
	    FMARel r = rit.next() ;
	    if (r.valueType.equals("Class")) {
		r.prop = m.createObjectProperty(ns+r.name) ;
	    }
	}
	// Adding inverse relationships
	rit = relations.values().iterator() ;
	while (rit.hasNext()) {
	    FMARel r = rit.next() ;
	    if (r.valueType.equals("Class")) {
		if (r.inverse!=null)
		    r.prop.addInverseOf(r.inverse.prop) ;
// 		Iterator<FMAObject> it3 = r.domain.iterator() ;
// 		while (it3.hasNext()) {
// 		    FMAObject ob = it3.next() ;
// 		    if (ob!=null)
// 			r.prop.addDomain(ob.owlclass) ;
// 		}
// 		it3 = r.range.iterator() ;
// 		while (it3.hasNext()) {
// 		    FMAObject ob = it3.next() ;
// 		    if (ob!=null)
// 			r.prop.addRange(ob.owlclass) ;
// 		}
		if (r.description!=null)
		    r.prop.addComment(r.description,"EN") ;
		Iterator<String> sit = r.synonyms.iterator() ;
		while (sit.hasNext()) {
		    String s = sit.next() ;
		    r.prop.addComment("Synonym: "+s,"EN") ;
		}
	    }
	}
// 	OntProperty partOf = m.createSymmetricProperty(ns+"partOf") ;
// 	OntProperty hasPart = m.createSymmetricProperty(ns+"hasPart") ;
// 	OntProperty hasConstPart = m.createObjectProperty(ns+"hasConstitutionalPart") ;
// 	OntProperty hasRegPart = m.createObjectProperty(ns+"hasRegionalPart") ;
// 	OntProperty hasSystPart = m.createObjectProperty(ns+"hasSystematicPart") ;
// 	OntProperty boundedBy = m.createObjectProperty(ns+"boundedBy") ;
// 	OntProperty has3DShape = m.createObjectProperty(ns+"hasInherent3DShape") ;
// 	OntProperty developsFrom = m.createObjectProperty(ns+"developsFrom") ;
// 	partOf.addInverseOf(hasPart) ;
// 	hasPart.addSubProperty(hasConstPart) ;
// 	hasPart.addSubProperty(hasRegPart) ;
// 	hasPart.addSubProperty(hasSystPart) ;
	Iterator<FMAObject> it = objects.values().iterator() ;
	while (it.hasNext()) {
	    FMAObject o = it.next() ;
	    if (o.fmaid==0) continue ;
	    OntClass c = null ;
	    if (!obo) {
		c = m.createClass(ns+o.name) ;
		c.addLabel(new String("FMA:"+o.fmaid),"EN") ;
	    } else {
		c = m.createClass(ns+"FMA_"+o.fmaid) ;
		c.addLabel(o.name,"EN") ;
	    }
	    if (o.definition!=null)
		c.addComment(o.definition,"EN") ;
	    o.owlclass = c ;
	}
	it = objects.values().iterator() ;
	while (it.hasNext()) {
	    FMAObject o = it.next() ;
	    OntClass c = o.owlclass ;
	    Iterator<FMAObject> li = o.superclass.iterator() ;
	    while (li.hasNext()) {
		c.addSuperClass(li.next().owlclass) ;
	    }
	    li = o.subclass.iterator() ;
	    while (li.hasNext()) {
		c.addSubClass(li.next().owlclass) ;
	    }
	    Iterator<FMARelInst> i2 = o.rels.iterator() ;
	    while (i2.hasNext()) {
		FMARelInst inst = i2.next() ;
		FMARel r = relations.get(inst.relid) ;
		if (r.valueType.equals("Class")) {
		    c.addSuperClass(m.createSomeValuesFromRestriction(null,r.prop,objects.get(new Integer(inst.value)).owlclass)) ;
		}
	    }
// 	    Iterator<FMAObject> i = o.partOf.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,partOf,o2.owlclass)) ;
// 	    }
// 	    i = o.hasConstPart.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,hasConstPart,o2.owlclass)) ;
// 	    }
// 	    i = o.hasPart.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,hasPart,o2.owlclass)) ;
// 	    }
// 	    i = o.hasRegPart.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,hasRegPart,o2.owlclass)) ;
// 	    }
// 	    i = o.hasSystPart.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,hasSystPart,o2.owlclass)) ;
// 	    }
// 	    i = o.boundedBy.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,boundedBy,o2.owlclass)) ;
// 	    }
// 	    i = o.has3DShape.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,has3DShape,o2.owlclass)) ;
// 	    }
// 	    i = o.developsFrom.iterator() ;
// 	    while (i.hasNext()) {
// 		FMAObject o2 = i.next() ;
// 		c.addSuperClass(m.createSomeValuesFromRestriction(null,developsFrom,o2.owlclass)) ;
// 	    }
	}
	try {
	    BufferedWriter out = new BufferedWriter(new FileWriter(argv[0])) ;
	    m.write(out) ;
	    debug.flush() ;
	} catch (Exception E) {E.printStackTrace() ;}
    }
}
