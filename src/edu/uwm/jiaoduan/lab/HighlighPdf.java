package edu.uwm.jiaoduan.lab;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.util.PDFTextStripper;

public class HighlighPdf {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PDDocument doc= null;
		try {
			doc = PDDocument.load("test.pdf");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	    PDDocumentOutline root = doc.getDocumentCatalog().getDocumentOutline();
		
	    PDOutlineItem item = root.getFirstChild();
	      while( item != null )
	      {
	          System.out.println( "Item:" + item.getTitle() );
	          PDOutlineItem child = item.getFirstChild();
	          while( child != null )
	          {
	              System.out.println( "    Child:" + child.getTitle() );
	              child = child.getNextSibling();
	          }
	          item = item.getNextSibling();
	      }

	}

}
