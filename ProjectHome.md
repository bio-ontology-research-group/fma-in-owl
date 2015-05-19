Here we collect tools and conversion approaches for the [Foundational Model of Anatomy (FMA)](http://sig.biostr.washington.edu/projects/fm/) in OWL.

A first result is a representation of the FMA in OWL (DL). Our version of the FMA classifies in under 4 seconds using the [CB reasoner](http://code.google.com/p/cb-reasoner/). If required, this time can be further improved through the use of the [EL Vira](http://el-vira.googlecode.com) modularization approach.

This FMA conversion assumes that relations
between two classes represent always `X SubClassOf: R some Y`
statements.
The conversion is complete in that all relations in the frames version
are translated (although we manually removed some template classes in the FMA version available on the Download page).
Additionally, domain and range restrictions are added (using a disjunction
if there are more than one) from the domain/range fillers available in
frames.
The code produces a conversion from frames without any
additional assumptions added.