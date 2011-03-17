/* Converted FMA into a C/NC representation */
/* All assertions are added to canonical subclasses of FMA classes instead of the classes themselves */

import java.io.* 
import java.util.*
import java.sql.*
import java.net.* 
import groovy.sql.Sql
import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary


def onturi = "http://bio2rdf.org/"

OWLOntologyManager man = OWLManager.createOWLOntologyManager();
OWLDataFactory fac = man.getOWLDataFactory()

OWLOntology ont = man.createOntology()


def sql = Sql.newInstance("jdbc:mysql://localhost/fma?user=root", "com.mysql.jdbc.Driver")

def rel2name = [:]
def name2rel = [:]
sql.eachRow("SELECT frame,short_value FROM fma where frame_type=7 and value_type=3 and slot like \"FMAID\"") {
  def s = new String(it.frame)
  rel2name[it.short_value] = s
  name2rel[s] = it.short_value
}

def rel2prop = [:]
def reln2prop = [:]
rel2name.keySet().each {
  def res = sql.rows("SELECT frame, short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and short_value like \"Class\"")
  if (res.size()>0) { // relation is object property
    def prop = fac.getOWLObjectProperty(IRI.create(onturi+"fma:"+it))
    OWLAxiom ax = fac.getOWLDeclarationAxiom(prop)
    man.addAxiom(ont, ax)
    rel2prop[it] = prop
    reln2prop[rel2name[it]] = prop
    OWLAnnotation label = fac.getOWLAnnotation(
      fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
      fac.getOWLTypedLiteral(rel2name[it]));
    ax = fac.getOWLAnnotationAssertionAxiom(prop.getIRI(), label)
    man.addAxiom(ont, ax)

    def res2 = sql.rows("SELECT short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and slot like ':DOCUMENTATION'")
    if (res2.size()>0) { // relation is object property
      if (res2[0].short_value!=null) {
	OWLAnnotation desc = fac.getOWLAnnotation(
	  fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDF_DESCRIPTION.getIRI()),
	  fac.getOWLTypedLiteral(res2[0].short_value));
	ax = fac.getOWLAnnotationAssertionAxiom(prop.getIRI(), desc)
	man.addAxiom(ont, ax)
      }
    }
  }
}



/* get the classes and assertions */

def id2name = [:]
def name2id = [:]

def cl2class = [:]
def cln2class = [:]
def cancl2class = [:]
def cancln2class = [:]
sql.eachRow("SELECT frame,short_value FROM fma where frame_type=6 and value_type=3 and slot like \"FMAID\"") {
  def s = new String(it.frame)
  id2name[it.short_value] = s
  name2id[s] = it.short_value
}

id2name.keySet().each {
  def cl = fac.getOWLClass(IRI.create(onturi+"fma:"+it))
  OWLAxiom ax = fac.getOWLDeclarationAxiom(cl)
  man.addAxiom(ont, ax)
  cl2class[it] = cl
  cln2class[id2name[it]] = cl
  def cl2 = fac.getOWLClass(IRI.create(onturi+"fma-can:"+it))
  ax = fac.getOWLDeclarationAxiom(cl2)
  man.addAxiom(ont, ax)
  cancl2class[it] = cl2
  cancln2class[id2name[it]] = cl2
  ax = fac.getOWLSubClassOfAxiom(cl2,cl)
  man.addAxiom(ont, ax)
  OWLAnnotation label = fac.getOWLAnnotation(
    fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
    fac.getOWLTypedLiteral(id2name[it]));
  ax = fac.getOWLAnnotationAssertionAxiom(cl.getIRI(), label)
  man.addAxiom(ont, ax)
  label = fac.getOWLAnnotation(
    fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
    fac.getOWLTypedLiteral(id2name[it]+" (canonical)"));
  ax = fac.getOWLAnnotationAssertionAxiom(cl2.getIRI(), label)
  man.addAxiom(ont, ax)

  def res2 = sql.rows("SELECT short_value FROM fma where frame_type=6 and frame like ${id2name[it]} and slot like 'definition'")
  if (res2.size()>0) { // relation is object property
    if (res2[0].short_value!=null) {
      OWLAnnotation desc = fac.getOWLAnnotation(
	fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDF_DESCRIPTION.getIRI()),
	fac.getOWLTypedLiteral(res2[0].short_value));
      ax = fac.getOWLAnnotationAssertionAxiom(cl.getIRI(), desc)
      man.addAxiom(ont, ax)
      desc = fac.getOWLAnnotation(
	fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDF_DESCRIPTION.getIRI()),
	fac.getOWLTypedLiteral(res2[0].short_value));
      ax = fac.getOWLAnnotationAssertionAxiom(cl2.getIRI(), desc)
      man.addAxiom(ont, ax)
    }
  }
}


/* structure object properties, assign domain and ranges, sub- and super-properties */
rel2name.keySet().each {
  def rel = rel2prop[it]
  def res = sql.rows("SELECT short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and slot like ':DIRECT-SUBSLOTS'")
  res.each { sub ->
    def rel2 = reln2prop[sub.short_value]
    OWLAxiom ax = fac.getOWLSubObjectPropertyOfAxiom(rel2,rel)
    try {
      man.addAxiom(ont, ax)
    } catch (Exception e){}
  }

  res = sql.rows("SELECT short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and slot like ':SLOT-INVERSE'")
  res.each { inv ->
    def rel2 = reln2prop[inv.short_value]
    OWLAxiom ax = fac.getOWLInverseObjectPropertiesAxiom(rel,rel2)
    try {
      man.addAxiom(ont, ax)
    } catch (Exception e){}
  }


  def range = new TreeSet()
  def domain = new TreeSet()
  res = sql.rows("SELECT short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and slot like ':SLOT-VALUE-TYPE' and value_type=6")
  res.each { r ->
    if (r.short_value!=null) {
      range.add(cln2class[r.short_value])
    }
  }
  res = sql.rows("SELECT short_value FROM fma where frame_type=7 and frame like ${rel2name[it]} and slot like ':DIRECT-DOMAIN' and value_type=6")
  res.each { r ->
    if (r.short_value!=null) {
      domain.add(cln2class[r.short_value])
    }
  }
  
  if (range.size()>1) {
    def cl = fac.getOWLObjectUnionOf(range)
    OWLAxiom ax = fac.getOWLObjectPropertyRangeAxiom(rel,cl)
    try {
      man.addAxiom(ont,ax)
    } catch (Exception e) {}
  } else {
    def cl = range.toList()[0]
    OWLAxiom ax = fac.getOWLObjectPropertyRangeAxiom(rel,cl)
    try {
      man.addAxiom(ont,ax)
    } catch (Exception e) {}
  }
  if (domain.size()>1) {
    def cl = fac.getOWLObjectUnionOf(domain)
    def ax = fac.getOWLObjectPropertyDomainAxiom(rel,cl)
    try {
      man.addAxiom(ont,ax)
    } catch (Exception e) {}
  } else {
    def cl = domain.toList()[0]
    OWLAxiom ax = fac.getOWLObjectPropertyDomainAxiom(rel,cl)
    try {
      man.addAxiom(ont,ax)
    } catch (Exception e) {}
  }
}

/* structure classes */
id2name.keySet().each {
  def cl = cl2class[it]
  def cancl = cancl2class[it]
  res = sql.rows("SELECT short_value FROM fma where frame_type=6 and frame like ${id2name[it]} and slot like ':DIRECT-SUBCLASSES' and value_type=6")
  res.each {r ->
    def cl2 = cln2class[r.short_value]
    def ax = fac.getOWLSubClassOfAxiom(cl2,cl)
    man.addAxiom(ont,ax)
  }
  res = sql.rows("SELECT slot,short_value FROM fma where frame_type=6 and frame like ${id2name[it]} and slot not like ':%' and value_type=6")
  res.each {r ->
    def rel = reln2prop[new String(r.slot)]
    def cl2 = cancln2class[r.short_value]
    def restriction = fac.getOWLObjectSomeValuesFrom(rel,cl2)
    def ax = fac.getOWLSubClassOfAxiom(cancl,restriction)
    try {
      man.addAxiom(ont,ax)
    } catch (Exception e) {}

  }
}



man.saveOntology(ont, IRI.create("file:/tmp/fma1.owl"))
