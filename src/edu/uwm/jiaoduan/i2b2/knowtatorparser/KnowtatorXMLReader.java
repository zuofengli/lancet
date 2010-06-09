package edu.uwm.jiaoduan.i2b2.knowtatorparser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Parser for exported knotator files.
 * @author shashank
 */
public class KnowtatorXMLReader {
    public static final String ANNOTATION_XPATH = "//annotations";

    private AssociatedTextGetter associatedTextGetter;
    private Map<String, Annotation> annotations;
    private Map<String, String> slotAnnotation;
    private Document document;

    /**
     * Creates an instance of the parser
     * @param knowtatorXmlFile the location of exported knowtator file
     * @param articleFile the location of the article text
     */
    public KnowtatorXMLReader(String knowtatorXmlFile, String articleFile) {
        try {
            DOMParser parser = new DOMParser();
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            parser.parse(knowtatorXmlFile);
            document = parser.getDocument();
            associatedTextGetter = new AssociatedTextGetter(articleFile);
            annotations = new HashMap<String, Annotation>();
            slotAnnotation = new HashMap<String, String>();
        } catch (SAXException ex) {
            Logger.getLogger(KnowtatorXMLReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(KnowtatorXMLReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Parses the document.
     */
    public void parse() {
        try {
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            XPathExpression expression = xPath.compile(ANNOTATION_XPATH);
            NodeList nodes = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); ++i) {
                nodesRecursive(nodes.item(i));
            }
        } catch (XPathExpressionException ex) {
            Logger.getLogger(KnowtatorXMLReader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void nodesRecursive(Node root) {
        String nodeName = root.getNodeName();

        if (nodeName.equalsIgnoreCase("annotation")) {
            handleAnnotationNode(root);
            return;
        }

        if (nodeName.equalsIgnoreCase("classMention")) {
            handleClassMentionNode(root);
            return;
        }

        if (nodeName.endsWith("SlotMention")) {
            handleSlotMentionNode(root);
            return;
        }

        if (root.hasChildNodes()) {
            NodeList childNodes = root.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); ++i) {
                nodesRecursive(childNodes.item(i));
            }
        }
    }

    private void handleClassMentionNode(Node root) {
        String annotationId = root.getAttributes().getNamedItem("id").getTextContent();
        String annotationName = "";
        NodeList rootChildren = root.getChildNodes();
        for (int i = 0; i < rootChildren.getLength(); ++i) {
            Node rootChild = rootChildren.item(i);
            String rootChildName = rootChild.getNodeName();
            if (rootChildName.equalsIgnoreCase("hasSlotMention")) {
                String slotId = rootChild.getAttributes().getNamedItem("id").getTextContent();
                slotAnnotation.put(slotId, annotationId);
                Slot slotToAdd = new Slot(slotId);
                annotations.get(annotationId).putSlot(slotId, slotToAdd);
            } else if (rootChildName.equalsIgnoreCase("mentionClass")) {
                annotationName = rootChild.getAttributes().getNamedItem("id").getTextContent();
                annotations.get(annotationId).setName(annotationName);
            }
        }
    }

    private void handleSlotMentionNode(Node root) {
        String slotId = root.getAttributes().getNamedItem("id").getTextContent();
        String annotationId = slotAnnotation.get(slotId);
        String slotName = "";
        I2B2Knowtator slotValue = null;
        String slotValueName;
        NodeList childrenNodes = root.getChildNodes();
        for (int i = 0; i < childrenNodes.getLength(); ++i) {
            Node childNode = childrenNodes.item(i);
            String childName = childNode.getNodeName();

            if (childName.endsWith("SlotMentionValue")) {
                slotValueName = childNode.getAttributes().getNamedItem("value").getTextContent();
                if (childName.startsWith("complex")) {
                    slotValue = annotations.get(slotValueName);
                } else {
                    slotValue = new I2B2String(slotValueName);
                }
                annotations.get(annotationId).getSlot(slotId).addValue(slotValue);
            } else if (childName.equalsIgnoreCase("mentionSlot")) {
                slotName = childNode.getAttributes().getNamedItem("id").getTextContent();
            }
        }
        annotations.get(annotationId).getSlot(slotId).setSlotName(slotName);
    }

    private void handleAnnotationNode(Node root) {
        NodeList annotationChildren = root.getChildNodes();
        String annotationId = "";
        int spanStart = -1;
        int spanEnd = -1;
        for (int i = 0; i < annotationChildren.getLength(); ++i) {
            Node annotationChild = annotationChildren.item(i);
            if (annotationChild.getNodeName().equalsIgnoreCase("mention")) {
                annotationId = annotationChild.getAttributes().getNamedItem("id").getTextContent();
            } else if (annotationChild.getNodeName().equalsIgnoreCase("annotator")) {
                // do nothing
            } else if (annotationChild.getNodeName().equalsIgnoreCase("span")) {
                spanStart = Integer.parseInt(annotationChild.getAttributes().getNamedItem("start").getTextContent());
                spanEnd = Integer.parseInt(annotationChild.getAttributes().getNamedItem("end").getTextContent());
            }
        }
        Annotation annotationToAdd = new Annotation(annotationId, spanStart, spanEnd);
        String associatedText = associatedTextGetter.getAssociatedText(spanStart, spanEnd);
        annotationToAdd.setAssociatedText(associatedText);
        annotations.put(annotationId, annotationToAdd);
    }

    /**
     * Gets all the annotations extracted from the knowtator exported file.
     * @return
     */
    public Map<String, Annotation> getAnnotations() {
        return annotations;
    }
}
