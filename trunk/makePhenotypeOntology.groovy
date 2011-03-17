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
def fmafile = new File("fma1.owl")

OWLOntologyManager man = OWLManager.createOWLOntologyManager();
OWLDataFactory fac = man.getOWLDataFactory()
def baseont = man.loadOntologyFromOntologyDocument(fmafile)
def tempset = new TreeSet()
tempset.add(baseont)

OWLOntology ont = man.createOntology(IRI.create(onturi),tempset)

def classes = ont.getClassesInSignature().flatten()

def count = 0

def hasPart = fac.getOWLObjectProperty(IRI.create(onturi+"has-part"))
OWLAxiom ax = fac.getOWLDeclarationAxiom(hasPart)
man.addAxiom(ont, ax)
def inheresIn = fac.getOWLObjectProperty(IRI.create(onturi+"inheres-in"))
ax = fac.getOWLDeclarationAxiom(inheresIn)
man.addAxiom(ont, ax)


classes.each {
  def label = it.getAnnotations(ont,fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI())).toList()[0].getValue().getLiteral()
  label = label.replaceAll(" (canonical)","")
  def str = it.toString()
  if (str.contains("fma-can:")) { // create abnormality
    def cl = fac.getOWLClass(IRI.create(onturi+"fma-phene:"+(count++)))
    ax = fac.getOWLDeclarationAxiom(cl)
    man.addAxiom(ont, ax)
    OWLAnnotation desc = fac.getOWLAnnotation(
      fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
      fac.getOWLTypedLiteral("Abnormality of "+label));
    ax = fac.getOWLAnnotationAssertionAxiom(cl.getIRI(), desc)
    man.addAxiom(ont, ax)

    ax = fac.getOWLSubClassOfAxiom(cl,fac.getOWLObjectSomeValuesFrom(inheresIn,fac.getOWLObjectComplementOf(fac.getOWLObjectSomeValuesFrom(hasPart,it))))
    man.addAxiom(ont, ax)
  } else { // create absence
    def cl = fac.getOWLClass(IRI.create(onturi+"fma-phene:"+(count++)))
    ax = fac.getOWLDeclarationAxiom(cl)
    man.addAxiom(ont, ax)
    OWLAnnotation desc = fac.getOWLAnnotation(
      fac.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
      fac.getOWLTypedLiteral("Absence of "+label));
    ax = fac.getOWLAnnotationAssertionAxiom(cl.getIRI(), desc)
    man.addAxiom(ont, ax)
    
    ax = fac.getOWLSubClassOfAxiom(cl,fac.getOWLObjectSomeValuesFrom(inheresIn,fac.getOWLObjectComplementOf(fac.getOWLObjectSomeValuesFrom(hasPart,it))))
    man.addAxiom(ont, ax)
  }
}

man.saveOntology(ont, IRI.create("file:/tmp/fma-phenotype.owl"))
