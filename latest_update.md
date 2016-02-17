# Update Nov 26 2010 #
I have wrapped Lancet with a XML-RPC services (written in Java). You could call Lancet by this way. The following is an example XML-PRC client code written in Python.
```
import xmlrpclib
s = xmlrpclib.Server('http://129.89.57.69:5282/')
medlist = s.Lancet.getMedicationList('aspirin 81 mg qid')
print medlist
```

The output should be something like following.
```
m="aspirin" 1:0 1:0||do="81 mg" 1:1 1:2||mo="nm"||f="qid" 1:3 1:3||du="nm"||r="nm"||ln="narrative"
```

# Update May 15 2010 #

This is an updated release of LancetMedExtractor which is ranked in the 2009 i2b2 NLP Medication Challenge. New features in this release:

1) all resources are bundled together into a runnable jar file.
In the first release, user need many efforts to make Lancet runnable. In this release, all resource are packaged into one runnable package. All needed resource files are packaged in this jar file.

2) A demo show
User could use following command to have a look of a simple demo. The results are displayed in i2b2 entry format.
```
java -Xmx250m -jar lancetMedExtractor.jar --demo
```
3) Help
```
 java -Xmx250m  -jar lancetMedExtractor.jar -h
```

All of these modification are aimed to run lancet on our own cluster (http://fire.ims.uwm.edu/wordpress/).